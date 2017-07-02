package com.szss.mysqlproxy.frontend;

import com.szss.mysqlproxy.net.buffer.BufferPool;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/18.
 */
public class FrontendConnectionFactory {

  public static FrontendConnection make(String reactorName, SocketChannel socketChannel,
      BufferPool bufferPool) throws IOException{
    ConByteBuffer readBuffer = new ConByteBuffer(bufferPool);
    ConByteBuffer writeBuffer = new ConByteBuffer(bufferPool);
    FrontendConnection connection = new FrontendConnection(reactorName, socketChannel, readBuffer,
        writeBuffer);
    return connection;
  }

}
