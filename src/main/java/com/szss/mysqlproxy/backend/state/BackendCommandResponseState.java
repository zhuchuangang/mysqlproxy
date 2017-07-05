package com.szss.mysqlproxy.backend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by zcg on 2017/6/21.
 */
public class BackendCommandResponseState implements BackendState {

  private static Logger logger = LogManager.getLogger(BackendCommandResponseState.class);
  private static BackendCommandResponseState state;

  private BackendCommandResponseState() {
  }

  public static BackendCommandResponseState instance() {
    if (state == null) {
      state = new BackendCommandResponseState();
    }
    return state;
  }

  @Override
  public void handle(BackendConnection connection) throws IOException {
    final ConByteBuffer dataBuffer = connection.getReadBuffer();
    //logger.info("The reading position is {}，the writing position is {}", dataBuffer.readPos(),dataBuffer.writingPos());
    byte packetType = -1;
    int offset = dataBuffer.readPos();
    int length = 0;
    int limit = dataBuffer.writingPos();
    int initOffset = offset;
    //logger.info("offset="+offset+"  limit="+limit);

    // 循环收到的报文处理
    //报文处理分两种情况：1.包头没有读完 2.包体没有读完
    while (true) {

      //情况1的后续处理，如果缓存了头部数据，说明上一次读取信息的最后一个mysql包头信息没有读完
      int leftSize = connection.getLeftSize();
      if (connection.getHeader() != null) {
        //获取缓存了头部数据,包头数据可能不完成
        byte[] header = connection.getHeader();
        //获取上一次剩余没有读取的包头长度
        //获取剩余包头数据
        for (int i = 0; i < leftSize; i++) {
          header[4 - leftSize + i] = dataBuffer.getByte(offset + 4 - leftSize + i);
        }
        //拼接包体
        int bodyLength = Connection.getLength(dataBuffer, offset);
        //设置offset
        offset += leftSize + bodyLength;
        //logger.info("A.  Skip {} bytes", leftSize+bodyLength);
        connection.setLeftSize(0);
        connection.setHeader(null);
      }

      //情况2的后续处理,如果没有缓存包头，但是还有剩余没有读取字节，说明是包体没有读完
      if (connection.getHeader() == null && leftSize != 0) {
        //如果数据包总长度-初始位置>=剩余包的长度  说明没有读完的包在这次的数据包中完整的包含，否则说明数据包的包体部分还没有读完
        if (limit-offset>=leftSize) {
          offset += leftSize;
          //logger.info("B.  Skip {} bytes", leftSize);
          connection.setLeftSize(0);
        }else {
          int skip=limit-offset;
          offset+=skip;
          connection.setLeftSize(leftSize-(limit-offset));
         // logger.info("=====================Skip {} bytes", skip);
        }
      }

      //情况1：readBuffer中最后一个mysql报文的包头没有读完
      //如果头部验证失败，并且offset < limit，说明readBuffer中的字节数不够4个字节，offset到limit的字节只是mysql数据包头部的部分信息
      if (!Connection.validateHeader(offset, limit)) {
        //如果offset<limit说明这个数据包最后一个mysql包包头不全
        if (offset < limit) {
          //计算还有多少字节包头没有读取
          int left = 4 - (limit - offset);
          connection.setLeftSize(left);
          //缓存不完整的mysql包头部信息
          byte[] header = new byte[Connection.MYSQL_PACKET_HEADER_SIZE];
          for (int i = 0; i < left; i++) {
            header[i] = dataBuffer.getByte(offset + i);
          }
          connection.setHeader(header);
        }
        //跳出循环，到此readBuffer已经解析完毕
        break;
      }

      //获取mysql包长度，包括包头部分
      length = Connection.getPacketLength(dataBuffer, offset);
      //logger.info("The packet length is {}", length);

      //解析报文类型
      packetType = dataBuffer.getByte(offset + Connection.MYSQL_PACKET_HEADER_SIZE);
      logger.info("The packet type is 0x{}", Integer.toHexString(packetType & 0xff));
      //根据报文类型和当前连接的状态，推动连接状态变化
      connection.nextConnectionState(packetType,offset);

      //情况2：readBuffer中最后一个mysql报文的包体没有读完
      //如果包体部分没有读完，计算下次reactor读取数据后，offset位移的字节数
      if (offset + length > limit) {
        //包体部分没有读完的长度
        int left = (length + offset) - limit;
        connection.setLeftSize(left);
        //logger.info("Not whole package,remain {} bytes what will be readed by the reactor on this next time！",left);
        //跳出循环，到此readBuffer已经解析完毕
        break;
      }
      //完整的报文，直接根据报文长度，位移到下一个报文的开始位置
      offset += length;
      //logger.info("C.  Skip {} bytes", length);
      dataBuffer.setReadingPos(offset);

    }
    //还原到初始的offset
    dataBuffer.setReadingPos(initOffset);
    //如果连接空闲，清除缓存值
    if (connection.getConnectionState() == Connection.IDLE_STATE) {
      connection.setLeftSize(0);
      connection.setHeader(null);
    }
    //将报文由前段连接写出
    logger.info("frontend enable write");
    connection.getFrontendConnection().enableWrite(false);
  }
}
