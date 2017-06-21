package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.backend.BackendConnectionPool;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/6/19.
 */
public class FrontendCommandState implements FrontendState {

  private static Logger logger = LogManager.getLogger(FrontendCommandState.class);

  private static FrontendCommandState state;

  private FrontendCommandState() {
  }

  public static FrontendCommandState instance() {
    if (state == null) {
      state = new FrontendCommandState();
    }
    return state;
  }

  @Override
  public void handle(FrontendConnection connection) throws IOException {
    if (connection.getBackendConnection() == null) {
      BackendConnectionPool connectionPool = BackendConnectionPool.getInstance();
      BackendConnection backendCon = connectionPool.connection(connection.getReactorName());
      connection.setBackendConnection(backendCon);
    }
    if (connection.getBackendConnection().getConnectionState() == Connection.CONNECTING_STATE) {
      connection.getTaskQueue().add(() -> {
        BackendConnection backendCon = connection.getBackendConnection();
        logger.info("mysql backend connection is idle,share the buffer of front connection!");
        if (backendCon.getReadBuffer() != connection.getWriteBuffer()) {
          backendCon.setReadBuffer(connection.getWriteBuffer());
        }
        if (backendCon.getWriteBuffer() != connection.getReadBuffer()) {
          backendCon.setWriteBuffer(connection.getReadBuffer());
        }
        backendCon.enableWrite(false);
      });
    } else {
      connection.getBackendConnection().enableWrite(false);
    }
  }
}
