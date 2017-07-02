package com.szss.mysqlproxy.backend.state;

import com.szss.mysqlproxy.backend.BackendConnection;

import java.io.IOException;

/**
 * Created by zcg on 2017/6/20.
 */
public interface BackendState {

  void handle(BackendConnection connection) throws IOException;

}
