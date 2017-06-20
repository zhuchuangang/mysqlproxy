package com.szss.mysqlproxy.util;

import com.szss.mysqlproxy.net.NIOConnector;

/**
 * Created by zcg on 2017/6/20.
 */
public class NetSystem {

  private static NetSystem netSystem;
  private NIOConnector connector;

  private NetSystem(){
  }

  public static NetSystem instance(){
    if (netSystem==null){
      netSystem=new NetSystem();
    }
    return netSystem;
  }

  public NIOConnector getConnector() {
    return connector;
  }

  public void setConnector(NIOConnector connector) {
    this.connector = connector;
  }
}
