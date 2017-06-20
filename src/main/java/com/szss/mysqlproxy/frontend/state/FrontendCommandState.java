package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.protocol.CommandPacket;
import com.szss.mysqlproxy.protocol.MySQLPacket;
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
    byte packetType=connection.getReadBuffer().getByte(4);
    if (packetType == MySQLPacket.COM_QUERY){
      CommandPacket cp=new CommandPacket();
      cp.read(connection.getReadBuffer().getByteBuffer());
    }
    BackendConnection backendCon = connection.getBackendConnection();
    backendCon.doWriteData();
  }
}
