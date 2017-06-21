package com.szss.mysqlproxy;

import com.szss.mysqlproxy.net.NIOAcceptor;
import com.szss.mysqlproxy.net.NIOConnector;
import com.szss.mysqlproxy.net.NIOReactor;
import com.szss.mysqlproxy.util.NetSystem;
import com.szss.mysqlproxy.util.SystemConfig;

/**
 * Created by zcg on 2017/6/13.
 */
public class Bootstrap {

  public static void main(String[] args) {
    SystemConfig config = SystemConfig.instance();
    config.setHost("127.0.0.1");
    config.setPort(3308);
    config.setDatabase("mysql");
    config.setUsername("root");
    config.setPassword("123456");
    config.setInitSize(5);
    try {
      NIOReactor[] reactors = new NIOReactor[Runtime.getRuntime().availableProcessors()];
      for (int i = 0; i < reactors.length; i++) {
        reactors[i] = new NIOReactor(i);
        reactors[i].start();
      }
      NIOConnector connector = new NIOConnector(reactors);
      NIOAcceptor acceptor = new NIOAcceptor(3306, reactors);

      NetSystem netSystem = NetSystem.instance();
      netSystem.setConnector(connector);

      connector.start();
      acceptor.start();

//      for (NIOReactor reactor : reactors) {
//        reactor.initBackendConnetion();
//      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
