package com.szss.mysqlproxy.backend;

import com.szss.mysqlproxy.backend.state.BackendHandshakeResponseState;
import com.szss.mysqlproxy.backend.state.BackendState;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/6/13.
 */
public class BackendConnection extends Connection {

  private static Logger logger = LogManager.getLogger(BackendConnection.class);

  public static final int CMD_QUERY_STATUS = 11;
  public static final int RESULT_WAIT_STATUS = 21;
  public static final int RESULT_INIT_STATUS = 22;
  public static final int RESULT_FETCH_STATUS = 23;
  public static final int RESULT_HEADER_STATUS = 24;
  public static final int RESULT_FAIL_STATUS = 29;

  private BackendState state;
  private int connectionState;
  //还有多少字节包头或包体读完
  private int leftSize=0;
  //如果是包头没有读完，那么缓存上次度过的一部分包头，header大小为4个字节
  private byte[] header;
  private FrontendConnection frontendConnection;

  public BackendConnection(String reactorName, SocketChannel socketChannel,
      ConByteBuffer readBuffer,
      ConByteBuffer writeBuffer) {
    this.leftSize = 0;
    this.reactorName = reactorName;
    this.socketChannel = socketChannel;
    this.readBuffer = readBuffer;
    this.writeBuffer = writeBuffer;
    this.connectionState = Connection.CONNECTING_STATE;
    this.state = BackendHandshakeResponseState.instance();
  }


  @Override
  public void onConnection() throws IOException {
    logger.info("Backend connection connect mysql server!");
  }

  @Override
  public void handle() throws IOException {
    state.handle(this);
  }

  public BackendState getState() {
    return state;
  }

  public void setState(BackendState state) {
    this.state = state;
  }

  public int getConnectionState() {
    return connectionState;
  }

  public void setConnectionState(int connectionState) {
    this.connectionState = connectionState;
  }

  public FrontendConnection getFrontendConnection() {
    return frontendConnection;
  }

  public void setFrontendConnection(FrontendConnection frontendConnection) {
    this.frontendConnection = frontendConnection;
  }

  public int getLeftSize() {
    return leftSize;
  }

  public void setLeftSize(int leftSize) {
    this.leftSize = leftSize;
  }

  public byte[] getHeader() {
    return header;
  }

  public void setHeader(byte[] header) {
    this.header = header;
  }
}
