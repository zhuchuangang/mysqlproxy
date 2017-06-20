package com.szss.mysqlproxy.net;

import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by zcg on 2017/5/2.
 */
public abstract class NIOHandler implements Runnable {

  private static Logger logger = LogManager.getLogger(NIOHandler.class);
  protected static final int MAX_READ = 1024;
  protected final Selector selector;
  protected final Connection connection;
  protected SelectionKey selectionKey;
  protected ConByteBuffer readBuffer;
  protected ConByteBuffer writeBuffer;

  private volatile LinkedList<ConByteBuffer> bufferQueue = new LinkedList<>();

  private AtomicBoolean writeFlag = new AtomicBoolean(false);


  public NIOHandler(Selector selector, Connection connection)
      throws IOException {
    this.selector = selector;
    this.selector.wakeup();
    this.connection = connection;
    //设置为非阻塞状态
    this.connection.getSocketChannel().configureBlocking(false);
    //注册写事件
    //TODO 使用select(timeout),register方法阻塞几秒，如何解决？
    //TODO 使用select(),register方法阻塞死掉，如何解决？
    //http://tool.oschina.net/apidocs/apidoc?api=jdk-zh
    //如果一个selection thread已经在select方法上等待ing,那么这个时候如果有另一条线程调用channal.register方法的话,那么它将被blocking.
    //http://blog.csdn.net/chenxuegui1234/article/details/17766813
    this.selectionKey = this.connection.getSocketChannel().register(selector, SelectionKey.OP_READ);
    this.selectionKey.attach(this);
    this.connection.setNioHandler(this);
    this.onConnection();
  }

  public abstract void onConnection() throws IOException;


  public void doReadData() throws IOException {
    throw new IOException(
        "If don`t implement NioHandler.doReadData() method,you can`t read socket data!");
  }

  public void doWriteData() throws IOException {
    while (!writeFlag.compareAndSet(false, true)) {
      //until the release
      //System.out.println("doWriteData:until the release");
    }
    try {
      writeToChannel(writeBuffer);
    } finally {
      writeFlag.lazySet(false);
    }
  }


  public void writeData(ConByteBuffer buffer) throws IOException {
    while (!writeFlag.compareAndSet(false, true)) {
      //until the release
    }
    try {
      if (writeBuffer == null && bufferQueue.isEmpty()) {
        writeToChannel(buffer);
      } else {
        bufferQueue.add(buffer);
        writeToChannel(writeBuffer);
      }
    } finally {
      writeFlag.lazySet(false);
    }
  }


  public void writeToChannel(final ConByteBuffer buffer) throws IOException {
    long writeNum = buffer.transferTo(this.connection.getSocketChannel());
    if (buffer.hasRemaining()) {
      selectionKey.interestOps(SelectionKey.OP_WRITE);
      this.selector.wakeup();
      if (writeBuffer != buffer) {
        writeBuffer = buffer;
      }
    } else {
      if (bufferQueue.isEmpty()) {
        selectionKey.interestOps(SelectionKey.OP_READ);
        this.selector.wakeup();
      } else {
        ConByteBuffer curBuffer = bufferQueue.removeFirst();
        writeToChannel(curBuffer);
      }
    }

  }

}
