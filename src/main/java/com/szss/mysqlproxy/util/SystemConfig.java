package com.szss.mysqlproxy.util;

/**
 * Created by zcg on 2017/6/18.
 */
public class SystemConfig {
  private String host;
  private int port;
  private String database;
  private String username;
  private String password;
  //每个reactor连接池大小
  private int initSize;

  private static SystemConfig config;

  private SystemConfig(){
  }

  public static SystemConfig instance(){
    if (config==null){
      config=new SystemConfig();
    }
    return config;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getInitSize() {
    return initSize;
  }

  public void setInitSize(int initSize) {
    this.initSize = initSize;
  }
}
