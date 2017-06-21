package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.util.SystemConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/2.
 */
public class NIOConnector extends Thread {

  private static Logger logger = LogManager.getLogger(NIOConnector.class);
  private NIOReactor[] reactors;
  private Selector selector;
  private ConcurrentLinkedQueue<BackendConnection> connectQueue;

  public NIOConnector(NIOReactor[] reactors) throws IOException {
    this.reactors = reactors;
    this.selector = Selector.open();
    this.connectQueue = new ConcurrentLinkedQueue<>();
  }

  public void postRegister(BackendConnection connection) {
    connectQueue.offer(connection);
    selector.wakeup();
    logger.info("{} add the channel to register queue,and wakeup selector", getName());
  }

  public void connect() {
    BackendConnection c = null;
    while ((c = connectQueue.poll()) != null) {
      try {
        SocketChannel channel = c.getSocketChannel();
        channel.register(selector, SelectionKey.OP_CONNECT, c);
        SystemConfig config = SystemConfig.instance();
        SocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        channel.connect(address);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void run() {
    Set<SelectionKey> selectionKeys;
    while (!Thread.interrupted()) {
      try {
        selector.select(1000);
        connect();
        selectionKeys = selector.selectedKeys();
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }
      Iterator<SelectionKey> iterator = selectionKeys.iterator();
      while (iterator.hasNext()) {
        SelectionKey key = iterator.next();
        BackendConnection connection = (BackendConnection) key.attachment();
        if (key.isValid() && key.isConnectable()) {
          try {
            if (connection.getSocketChannel().isConnectionPending()) {
              connection.getSocketChannel().finishConnect();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          String reactorName = connection.getReactorName();
          String index = reactorName.replace("nio-reactor-", "");
          reactors[Integer.parseInt(index)].postRegister(connection);
        }
        iterator.remove();
      }
      selectionKeys.clear();
    }
  }
}
