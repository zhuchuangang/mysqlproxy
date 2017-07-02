package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.frontend.FrontendConnection;

import java.io.IOException;

/**
 * Created by zcg on 2017/6/15.
 */
public interface FrontendState {

  void handle(FrontendConnection connection) throws IOException;
}
