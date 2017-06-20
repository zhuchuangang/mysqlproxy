package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.frontend.FakeMysqlServer;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import com.szss.mysqlproxy.protocol.HandshakeV10Packet;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/6/15.
 */
public class FrontendInitialHandshakeState implements FrontendState {

  private static Logger logger = LogManager.getLogger(FrontendInitialHandshakeState.class);

  private static FrontendInitialHandshakeState state;

  private FrontendInitialHandshakeState() {
  }

  public static FrontendInitialHandshakeState instance() {
    if (state == null) {
      state = new FrontendInitialHandshakeState();
    }
    return state;
  }

  @Override
  public void handle(FrontendConnection connection) throws IOException {
    HandshakeV10Packet handshake = FakeMysqlServer.instance().response();
    logger.info(handshake);
    final ConByteBuffer writeBuffer=connection.getWriteBuffer();
    ByteBuffer buf=writeBuffer.beginWrite(handshake.calcPacketSize()+4);
    handshake.write(buf);
    writeBuffer.endWrite(buf);
    connection.getNioHandler().writeData(writeBuffer);
    connection.setState(FrontendHandshakeResponseState.instance());
  }
}
