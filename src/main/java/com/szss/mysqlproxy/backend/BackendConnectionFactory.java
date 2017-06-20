package com.szss.mysqlproxy.backend;

import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import com.szss.mysqlproxy.util.NetSystem;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/20.
 */
public class BackendConnectionFactory {

  public static BackendConnection make() throws IOException {
    ConByteBuffer readBuffer = new ConByteBuffer();
    ConByteBuffer writeBuffer = new ConByteBuffer();
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    BackendConnection connection = new BackendConnection(socketChannel, readBuffer, writeBuffer);
    NetSystem.instance().getConnector().postRegister(connection);
    return connection;
  }
}