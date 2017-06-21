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

  private BackendState state;
  private int connectionState;
  private FrontendConnection frontendConnection;

  public BackendConnection(String reactorName, SocketChannel socketChannel,
      ConByteBuffer readBuffer,
      ConByteBuffer writeBuffer) {
    this.reactorName = reactorName;
    this.socketChannel = socketChannel;
    this.readBuffer = readBuffer;
    this.writeBuffer = writeBuffer;
    this.connectionState = Connection.STATE_CONNECTING;
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
}
