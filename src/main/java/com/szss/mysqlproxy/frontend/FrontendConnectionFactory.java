package com.szss.mysqlproxy.frontend;

import com.szss.mysqlproxy.net.buffer.BufferPool;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/18.
 */
public class FrontendConnectionFactory {

  public static FrontendConnection make(SocketChannel socketChannel, BufferPool bufferPool) {
    ConByteBuffer readBuffer = new ConByteBuffer(bufferPool);
    ConByteBuffer writeBuffer = new ConByteBuffer(bufferPool);
    FrontendConnection connection = new FrontendConnection(socketChannel, readBuffer, writeBuffer);
    return connection;
  }

}
