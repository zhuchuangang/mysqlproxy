package com.szss.mysqlproxy.frontend;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.frontend.state.FrontendInitialHandshakeState;
import com.szss.mysqlproxy.frontend.state.FrontendState;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/2.
 */
public class FrontendConnection extends Connection {

  private static Logger logger = LogManager.getLogger(FrontendConnection.class);

  private FrontendState state;

  private BackendConnection backendConnection;

  private LinkedList<NoneBlockTask> taskQueue;

  public FrontendConnection(String reactorName, SocketChannel socketChannel,
      ConByteBuffer readBuffer,
      ConByteBuffer writeBuffer) {
    this.reactorName = reactorName;
    this.state = FrontendInitialHandshakeState.instance();
    this.socketChannel = socketChannel;
    this.readBuffer = readBuffer;
    this.writeBuffer = writeBuffer;
    this.taskQueue = new LinkedList();
  }

  @Override
  public void onConnection() throws IOException {
    logger.info("Front connection send handshake packet to client!");
    state.handle(this);
  }

  @Override
  public void handle() throws IOException {
    state.handle(this);
  }

  public void setState(FrontendState state) {
    this.state = state;
  }

  public void setBackendConnection(BackendConnection backendConnection) {
    this.backendConnection = backendConnection;
    this.backendConnection.setFrontendConnection(this);
//    if (this.backendConnection.getConnectionState() == Connection.STATE_IDLE) {
//      logger.info("mysql backend connection is idle,share the buffer of front connection!");
//      this.backendConnection.setReadBuffer(this.getWriteBuffer());
//      this.backendConnection.setWriteBuffer(this.getReadBuffer());
//    }
  }

  public BackendConnection getBackendConnection() {
    return backendConnection;
  }

  public LinkedList<NoneBlockTask> getTaskQueue() {
    return taskQueue;
  }
}
