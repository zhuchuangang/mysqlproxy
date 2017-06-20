package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.frontend.FrontendConnection;
import java.io.IOException;
import java.nio.channels.Selector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/3/25.
 */
public class FrontendHandler extends NIOHandler {

  private static Logger logger = LogManager.getLogger(FrontendHandler.class);

  public FrontendHandler(Selector selector, FrontendConnection connection) throws IOException {
    super(selector, connection);
  }

  @Override
  public void run() {
    if (!this.selectionKey.isValid()) {
      logger.debug("select-key cancelled");
      return;
    }
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
    logger
        .info("{} connection has to accept,socket channel is {}", Thread.currentThread().getName(),
            connection.getSocketChannel());
    ((FrontendConnection) connection).handle();
  }

  @Override
  public void doReadData() throws IOException {
    int readNum = connection.getReadBuffer().transferFrom(this.connection.getSocketChannel());
    if (readNum == 0) {
      return;
    }
    if (readNum == -1) {
      this.connection.getSocketChannel().socket().close();
      this.connection.getSocketChannel().close();
      selectionKey.cancel();
      return;
    }

    ((FrontendConnection) connection).handle();
//    chunk = commandHandler.response(chunk, this.connection.getSocketChannel(), this);
//    if (chunk != null) {
      //writeData(writeBuffer);
//    }
  }
}
