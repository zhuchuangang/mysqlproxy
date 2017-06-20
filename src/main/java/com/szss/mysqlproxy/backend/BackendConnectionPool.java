package com.szss.mysqlproxy.backend;

import java.io.IOException;
import java.util.concurrent.LinkedTransferQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/4.
 */
public class BackendConnectionPool {

  private static Logger logger = LogManager.getLogger(BackendConnectionPool.class);

  private static BackendConnectionPool pool;
  private LinkedTransferQueue<BackendConnection> connections;

  private BackendConnectionPool() {
    connections = new LinkedTransferQueue<>();
  }

  public static BackendConnectionPool getInstance() {
    if (pool == null) {
      pool = new BackendConnectionPool();
    }
    return pool;
  }

  public void addConnection(BackendConnection connection) {
    this.connections.add(connection);
  }

  public BackendConnection connection() throws IOException {
    BackendConnection con = null;
    if (!this.connections.isEmpty()) {
      con = this.connections.poll();
    }
    if (con == null) {
      con = BackendConnectionFactory.make();
    }
    return con;
  }

  public void init() throws IOException {
    for (int i = 0; i < 1; i++) {
      BackendConnection con = BackendConnectionFactory.make();
      connections.add(con);
      logger.info("init mysql backend connection {}",con);
    }
  }

}
