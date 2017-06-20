package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/5/2.
 */
public abstract class Connection {

  protected SocketChannel socketChannel;
  protected ConByteBuffer readBuffer;
  protected ConByteBuffer writeBuffer;
  protected NIOHandler nioHandler;

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

  public NIOHandler getNioHandler() {
    return nioHandler;
  }

  public void setNioHandler(NIOHandler nioHandler) {
    this.nioHandler = nioHandler;
  }
}
