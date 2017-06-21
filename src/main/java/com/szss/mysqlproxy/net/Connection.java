package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/2.
 */
public abstract class Connection {

  private static Logger logger = LogManager.getLogger(Connection.class);

  protected SocketChannel socketChannel;
  protected Selector selector;
  protected SelectionKey selectionKey;
  protected ConByteBuffer readBuffer;
  protected ConByteBuffer writeBuffer;
  protected String reactorName;


  public static final int CONNECTING_STATE = 0;
  public static final int IDLE_STATE = 1;
  public static final int CLOSING_STATE = -1;
  public static final int CLOSED_STATE = -2;

  public final static int MYSQL_PACKET_HEADER_SIZE = 4;


  public void register(Selector selector) throws IOException {
    this.selector = selector;
    //设置为非阻塞状态
    this.socketChannel.configureBlocking(false);
    //注册写事件
    this.selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
    this.selectionKey.attach(this);
    onConnection();
  }

  public abstract void onConnection() throws IOException;

  public abstract void handle() throws IOException;

  public void doReadData() throws IOException {
    int readNum = readBuffer.transferFrom(socketChannel);
    if (readNum == 0) {
      return;
    }
    if (readNum == -1) {
      socketChannel.socket().close();
      socketChannel.close();
      selectionKey.cancel();
      return;
    }

    handle();
  }


  public void doWriteData() throws IOException {
    int writeNum = writeBuffer.transferTo(socketChannel);
    boolean hasRemaining = writeBuffer.hasRemaining();
    if (hasRemaining) {
      if (selectionKey.isValid() && !selectionKey.isWritable()) {
        enableWrite(false);
      }
    } else {
      if (selectionKey.isValid() && selectionKey.isWritable()) {
        disableWrite();
      }
    }
  }

  public void disableWrite() {
    try {
      selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
    } catch (Exception e) {
      logger.error("can't disable write {},connection is {}", e, this);
    }
  }


  public void enableWrite(boolean wakeup) {
    boolean needWakeup = false;
    try {
      selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
      needWakeup = true;
    } catch (Exception e) {
      logger.error("can't enable write {},connection is {}", e, this);
    }
    if (needWakeup && wakeup) {
      selector.wakeup();
    }

  }

  public static final boolean validateHeader(final long offset, final long position) {
    return (position >= (offset + MYSQL_PACKET_HEADER_SIZE));
  }

  public static final int getPacketLength(ConByteBuffer buffer, int offset) throws IOException {
    int length = buffer.getByte(offset) & 0xff;
    length |= (buffer.getByte(++offset) & 0xff) << 8;
    length |= (buffer.getByte(++offset) & 0xff) << 16;
    return length + MYSQL_PACKET_HEADER_SIZE;
  }

  public static final int getLength(ConByteBuffer buffer, int offset) throws IOException {
    int length = buffer.getByte(offset) & 0xff;
    length |= (buffer.getByte(++offset) & 0xff) << 8;
    length |= (buffer.getByte(++offset) & 0xff) << 16;
    return length;
  }

  public SocketChannel getSocketChannel() {
    return socketChannel;
  }

  public void setSocketChannel(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  public ConByteBuffer getReadBuffer() {
    return readBuffer;
  }

  public void setReadBuffer(ConByteBuffer readBuffer) {
    this.readBuffer = readBuffer;
  }

  public ConByteBuffer getWriteBuffer() {
    return writeBuffer;
  }

  public void setWriteBuffer(ConByteBuffer writeBuffer) {
    this.writeBuffer = writeBuffer;
  }

  public String getReactorName() {
    return reactorName;
  }

  public void setReactorName(String reactorName) {
    this.reactorName = reactorName;
  }
}
