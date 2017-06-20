package com.szss.mysqlproxy.backend;

import com.szss.mysqlproxy.backend.state.BackendHandshakeResponseState;
import com.szss.mysqlproxy.backend.state.BackendState;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/13.
 */
public class BackendConnection extends Connection {

  private BackendState state;
  private int connectionState;
  private FrontendConnection frontendConnection;

  public BackendConnection(SocketChannel socketChannel, ConByteBuffer readBuffer,
      ConByteBuffer writeBuffer) {
    this.socketChannel = socketChannel;
    this.readBuffer = readBuffer;
    this.writeBuffer = writeBuffer;
    this.connectionState = Connection.STATE_CONNECTING;
    this.state = BackendHandshakeResponseState.instance();
  }


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
