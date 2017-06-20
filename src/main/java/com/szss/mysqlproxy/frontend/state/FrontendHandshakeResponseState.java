package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.backend.BackendConnectionPool;
import com.szss.mysqlproxy.frontend.FakeMysqlServer;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import com.szss.mysqlproxy.protocol.AuthPacket;
import com.szss.mysqlproxy.protocol.ERRPacket;
import com.szss.mysqlproxy.protocol.HandshakeV10Packet;
import com.szss.mysqlproxy.protocol.OKPacket;
import com.szss.mysqlproxy.protocol.constants.ErrorCode;
import com.szss.mysqlproxy.protocol.constants.StatusFlags;
import com.szss.mysqlproxy.protocol.support.SecurityUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/6/18.
 */
public class FrontendHandshakeResponseState implements FrontendState {

  private static Logger logger = LogManager.getLogger(FrontendHandshakeResponseState.class);

  private static FrontendHandshakeResponseState state;

  private FrontendHandshakeResponseState() {
  }

  public static FrontendHandshakeResponseState instance() {
    if (state == null) {
      state = new FrontendHandshakeResponseState();
    }
    return state;
  }


  @Override
  public void handle(FrontendConnection connection) throws IOException {
    AuthPacket ap = new AuthPacket();
    ap.read(connection.getReadBuffer().getByteBuffer());
    boolean success = auth(ap);

    if (success) {
      OKPacket op = new OKPacket();
      op.packetSequenceId = 2;
      op.capabilities = FakeMysqlServer.getFakeServerCapabilities();
      op.statusFlags = StatusFlags.SERVER_STATUS_AUTOCOMMIT.getCode();
      final ConByteBuffer writeBuffer = connection.getWriteBuffer();
      ByteBuffer buf = writeBuffer.beginWrite(op.calcPacketSize() + 4);
      op.write(buf);
      writeBuffer.endWrite(buf);

      if (logger.isDebugEnabled()) {
        logger.debug("login authentication success,server response client ok packet:{}", op);
      }
      connection.doWriteData();
      BackendConnectionPool connectionPool = BackendConnectionPool.getInstance();
      BackendConnection bc = connectionPool.connection();
      connection.setBackendConnection(bc);
      connection.setState(FrontendCommandState.instance());
    } else {
      ERRPacket ep = new ERRPacket();
      ep.packetSequenceId = 2;
      ep.capabilities = FakeMysqlServer.getFakeServerCapabilities();
      ep.errorCode = ErrorCode.ER_ACCESS_DENIED_ERROR;
      ep.sqlState = "28000";
      String address = "";
      try {
        address = connection.getSocketChannel().getLocalAddress().toString();
      } catch (IOException e) {
        e.printStackTrace();
      }
      ep.errorMessage =
          "Access denied for user '" + ap.username + "'@'" + address + "' (using password: YES)";
      final ConByteBuffer writeBuffer = connection.getWriteBuffer();
      ByteBuffer buf = writeBuffer.beginWrite(ep.calcPacketSize() + 4);
      ep.write(buf);
      writeBuffer.endWrite(buf);
      if (logger.isDebugEnabled()) {
        logger.debug("login authentication error,server response client error packet:{}", ep);
      }
      connection.doWriteData();
      connection.setState(FrontendInitialHandshakeState.instance());
    }

  }


  public boolean auth(AuthPacket ap) {
    FakeMysqlServer server = FakeMysqlServer.instance();
    if (ap.username.equals(server.getUsername())) {
      HandshakeV10Packet handshake = server.getHandshake();
      int len1 = handshake.authPluginDataPart1.length;
      int len2 = handshake.authPluginDataPart2.length;
      byte[] seed = new byte[len1 + len2];
      System.arraycopy(handshake.authPluginDataPart1, 0, seed, 0, len1);
      System.arraycopy(handshake.authPluginDataPart2, 0, seed, len1, len2);
      String serverEncryptedPassword = null;
      try {
        serverEncryptedPassword = new String(
            SecurityUtil.scramble411(server.getPassword().getBytes(), seed));
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
      String clientEncryptedPassword = new String(ap.password);
      if (clientEncryptedPassword.equals(serverEncryptedPassword)) {
        return true;
      }
    }
    return false;
  }
}
