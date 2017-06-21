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


  public static final int STATE_CONNECTING = 0;
  public static final int STATE_IDLE = 1;
  public static final int STATE_CLOSING = -1;
  public static final int STATE_CLOSED = -2;

  public static final int CMD_QUERY_STATUS = 11;
  public static final int RESULT_WAIT_STATUS = 21;
  public static final int RESULT_INIT_STATUS = 22;
  public static final int RESULT_FETCH_STATUS = 23;
  public static final int RESULT_HEADER_STATUS = 24;
  public static final int RESULT_FAIL_STATUS = 29;


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
