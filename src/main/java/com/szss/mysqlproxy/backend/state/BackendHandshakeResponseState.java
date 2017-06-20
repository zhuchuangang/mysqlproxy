package com.szss.mysqlproxy.backend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.protocol.AuthPacket;
import com.szss.mysqlproxy.protocol.HandshakeV10Packet;
import com.szss.mysqlproxy.protocol.MySQLPacket;
import com.szss.mysqlproxy.protocol.constants.ClientCapabilityFlags;
import com.szss.mysqlproxy.protocol.support.SecurityUtil;
import com.szss.mysqlproxy.util.SystemConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/6/20.
 */
public class BackendHandshakeResponseState implements BackendState {

  private static Logger logger = LogManager.getLogger(BackendHandshakeResponseState.class);

  private static BackendHandshakeResponseState state;

  private BackendHandshakeResponseState() {
  }

  public static BackendHandshakeResponseState instance() {
    if (state == null) {
      state = new BackendHandshakeResponseState();
    }
    return state;
  }

  @Override
  public void handle(BackendConnection connection) throws IOException {
    byte packetType = connection.getReadBuffer().getByte(4);
    if (packetType == MySQLPacket.OK_PACKET) {
      connection.setState(BackendCommandState.instance());
      connection.setConnectionState(Connection.STATE_IDLE);
      FrontendConnection frontCon = connection.getFrontendConnection();
      if (frontCon != null) {
        //后端登录验证成功，共享前后端buffer
        logger.info("The backend connection connect successful,mysql backend connection is idle,share the buffer of front connection!");
        connection.setWriteBuffer(frontCon.getReadBuffer());
        connection.setReadBuffer(frontCon.getWriteBuffer());
      }
    } else if (packetType == MySQLPacket.ERROR_PACKET) {

    } else {
      auth(connection);
    }
  }

  private void auth(BackendConnection connection) throws IOException {
    HandshakeV10Packet handshake = new HandshakeV10Packet();
    handshake.read(connection.getReadBuffer().getByteBuffer());
    logger.info(handshake);

    SystemConfig config = SystemConfig.instance();
    AuthPacket ap = new AuthPacket();
    int len1 = handshake.authPluginDataPart1.length;
    int len2 = handshake.authPluginDataPart2.length;
    byte[] seed = new byte[len1 + len2];
    System.arraycopy(handshake.authPluginDataPart1, 0, seed, 0, len1);
    System.arraycopy(handshake.authPluginDataPart2, 0, seed, len1, len2);
    try {
      ap.password = SecurityUtil.scramble411(config.getPassword().getBytes(), seed);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    ap.packetSequenceId = 1;
    ap.capabilityFlags = ClientCapabilityFlags.getClientCapabilities();
    ap.characterSet = 0x53;//utf8_bin
    ap.username = config.getUsername();
    ap.database = config.getDatabase();
    ap.authPluginName = handshake.authPluginName;
    logger.info(ap);
    ByteBuffer buffer = connection.getWriteBuffer().beginWrite(ap.calcPacketSize() + 4);
    ap.write(buffer);
    connection.getWriteBuffer().endWrite(buffer);
    connection.getNioHandler().writeData(connection.getWriteBuffer());
  }
}
