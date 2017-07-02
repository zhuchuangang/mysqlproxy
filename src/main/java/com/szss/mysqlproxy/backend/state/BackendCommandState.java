package com.szss.mysqlproxy.backend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.frontend.NoneBlockTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by zcg on 2017/6/20.
 */
public class BackendCommandState implements BackendState {

  private static Logger logger = LogManager.getLogger(BackendCommandState.class);
  private static BackendCommandState state;

  private BackendCommandState() {
  }

  public static BackendCommandState instance() {
    if (state == null) {
      state = new BackendCommandState();
    }
    return state;
  }

  @Override
  public void handle(BackendConnection connection) throws IOException {
    FrontendConnection frontCon = connection.getFrontendConnection();
    if (frontCon != null && !frontCon.getTaskQueue().isEmpty()) {
      NoneBlockTask task = frontCon.getTaskQueue().removeFirst();
      try {
        task.execute();
        byte packetType = connection.getWriteBuffer().getByte(4);
        connection.nextConnectionState(packetType);
        connection.setState(BackendCommandResponseState.instance());
      } catch (Exception e) {

      }
    }
  }
}
