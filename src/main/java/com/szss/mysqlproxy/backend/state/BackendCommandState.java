package com.szss.mysqlproxy.backend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    connection.doWriteData();
  }
}
