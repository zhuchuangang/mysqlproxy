package com.szss.mysqlproxy.backend;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/4.
 */
public class BackendConnectionPool {

  private static Logger logger = LogManager.getLogger(BackendConnectionPool.class);

  private static BackendConnectionPool pool;
  private ConcurrentHashMap<String, List<BackendConnection>> conMap;

  private BackendConnectionPool() {
    conMap = new ConcurrentHashMap();
  }

  public static BackendConnectionPool getInstance() {
    if (pool == null) {
      pool = new BackendConnectionPool();
    }
    return pool;
  }

  public void addConnection(BackendConnection connection) {
    List<BackendConnection> backendCons = conMap.get(connection.getReactorName());
    if (backendCons == null) {
      backendCons = new LinkedList<>();
    }
    backendCons.add(connection);
    this.conMap.put(connection.getReactorName(), backendCons);
  }

  public BackendConnection connection(String reactorName) throws IOException {
    BackendConnection con = null;
    List<BackendConnection> backendCons = conMap.get(reactorName);
    if (backendCons!=null&&!backendCons.isEmpty()) {
      con = backendCons.remove(0);
    }
    if (con == null) {
      con = BackendConnectionFactory.make(reactorName);
    }
    return con;
  }

}
