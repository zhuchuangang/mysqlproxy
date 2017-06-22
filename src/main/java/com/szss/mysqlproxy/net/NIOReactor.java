package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.backend.BackendConnectionFactory;
import com.szss.mysqlproxy.backend.BackendConnectionPool;
import com.szss.mysqlproxy.net.buffer.BufferPool;
import com.szss.mysqlproxy.util.SystemConfig;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * reactor作为IO线程，业务处理的handler线程在线程池中执行
 */
public class NIOReactor extends Thread {

  private static Logger logger = LogManager.getLogger(NIOReactor.class);

  private static final String PREFIX_NAME = "nio-reactor-";

  private BufferPool bufferPool;
  //每个reactor都有各自的selector
  private Selector selector;
  //注册队列
  private ConcurrentLinkedQueue<Connection> registerQueue;

  public NIOReactor(int index) throws IOException {
    this.bufferPool = new BufferPool(1024 * 1024 * 100, 1024*1024);
    this.selector = Selector.open();
    this.registerQueue = new ConcurrentLinkedQueue();
    setName(PREFIX_NAME + index);
    logger.info("{} create reactor thread:{}", getName(), getName());
  }

  public void postRegister(Connection connection) {
    registerQueue.offer(connection);
    selector.wakeup();
    logger.info("{} add the channel to register queue,and wakeup selector", getName());
  }


  @Override
  public void run() {
    Set<SelectionKey> keys;
    int selectNum = 0;
    while (!Thread.interrupted()) {
      try {
        selectNum = selector.select(400 / (selectNum + 1));
        //System.out.println(getName() + " there is " + selectNum + " ready event");
        if (selectNum == 0) {
          register();
          continue;
        }
        keys = selector.selectedKeys();
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }
      Iterator<SelectionKey> iterator = keys.iterator();
      while (iterator.hasNext()) {
        Connection con = null;
        try {
          SelectionKey key = iterator.next();
          con = (Connection) key.attachment();
          if (con != null) {
            if (key.isValid() && key.isReadable()) {
              con.doReadData();
            }
            if (key.isValid() && key.isWritable()) {
              con.doWriteData();
            }
          }
          iterator.remove();
        } catch (Exception e) {
          if (e instanceof CancelledKeyException) {
            if (logger.isDebugEnabled()) {
              logger.debug("{} socket key canceled", con);
            }
          } else {
            logger.warn("connection is error", e);
          }
        }
      }
      keys.clear();
    }
  }

  private void register() {
    if (registerQueue.isEmpty()) {
      return;
    }
    Connection c = null;
    while ((c = registerQueue.poll()) != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("{} register queue poll {}", getName(), c);
      }
      try {
        c.register(selector);
        if (logger.isDebugEnabled()) {
          logger.debug("{} register channel to listen for read event",
              getName(), c);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void initBackendConnetion() throws IOException {
    int initSize = SystemConfig.instance().getInitSize();
    BackendConnectionPool conPool = BackendConnectionPool.getInstance();
    for (int i = 0; i < initSize; i++) {
      BackendConnection backendCon = BackendConnectionFactory.make(getName());
      conPool.addConnection(backendCon);
    }
  }

  public BufferPool getBufferPool() {
    return bufferPool;
  }
}
