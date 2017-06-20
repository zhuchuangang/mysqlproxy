package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.backend.BackendConnection;
import java.io.IOException;
import java.nio.channels.Selector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/2.
 */
public class BackendHandler extends NIOHandler {

  private static Logger logger = LogManager.getLogger(BackendHandler.class);

  public BackendHandler(Selector selector, BackendConnection connection) throws IOException {
    super(selector, connection);
  }

  @Override
  public void run() {
    try {
      if (this.selectionKey.isReadable()) {
        doReadData();
      } else if (this.selectionKey.isWritable()) {
        doWriteData();
      }
    } catch (IOException e) {
      //e.printStackTrace();
    }
  }

  @Override
  public void onConnection() throws IOException {
    logger.info("{} connection is connected,socket channel is {}", Thread.currentThread().getName(),
        connection.getSocketChannel());
  }

  @Override
  public void doReadData() throws IOException {
    int readNum = connection.getReadBuffer().transferFrom(connection.getSocketChannel());
    if (readNum == 0) {
      return;
    }
    if (readNum == -1) {
      this.connection.getSocketChannel().socket().close();
      this.connection.getSocketChannel().close();
      selectionKey.cancel();
      return;
    }
    ((BackendConnection)connection).handle();
  }

}
