package com.szss.mysqlproxy.net.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/18.
 */
public class ConByteBuffer {

  private BufferPool bufferPool;
  private ByteBuffer byteBuffer;
  private int readPos;
  private int writePos;

  public ConByteBuffer(BufferPool bufferPool) {
    this.bufferPool = bufferPool;
    this.byteBuffer = bufferPool.allocate();
    this.readPos = 0;
    this.writePos = 0;
  }

  public ConByteBuffer() {
    this.byteBuffer = ByteBuffer.allocate(300);
    this.readPos = 0;
    this.writePos = 0;
  }


  public int transferFrom(SocketChannel socketChanel) throws IOException {
    byteBuffer.clear();
    readPos = 0;
    int readNum = socketChanel.read(byteBuffer);
    if (readNum > 0) {
      writePos = readPos + readNum;
    }
    return readNum;
  }


  public void putBytes(ByteBuffer buf) {
    byteBuffer.clear();
    readPos = 0;
    byteBuffer.put(buf);
    writePos = byteBuffer.position();
  }


  public void putBytes(byte[] buf) {
    byteBuffer.clear();
    readPos = 0;
    byteBuffer.put(buf);
    writePos = byteBuffer.position();
  }


  public ByteBuffer beginWrite(int length) {
    byteBuffer.clear();
    readPos = 0;
    writePos = 0;
    if (length <= byteBuffer.capacity()) {
      byteBuffer.limit(length);
    } else {
      byteBuffer.limit(byteBuffer.capacity());
    }
    return byteBuffer.slice();
  }


  public void endWrite(ByteBuffer buffer) {
    writePos = buffer.position();
  }


  public byte getByte(int index) throws IOException {
    return byteBuffer.get(index);
  }


  public ByteBuffer getBytes(int index, int length) {
    int p = byteBuffer.position();
    int l = byteBuffer.limit();
    byteBuffer.position(index);
    byteBuffer.limit(index + length);
    ByteBuffer buffer = byteBuffer.slice();
    byteBuffer.limit(l);
    byteBuffer.position(p);
    return buffer;
  }

  public ByteBuffer getByteBuffer() {
    int p = byteBuffer.position();
    int l = byteBuffer.limit();
    byteBuffer.position(readPos);
    byteBuffer.limit(writePos);
    ByteBuffer buffer = byteBuffer.slice();
    byteBuffer.limit(l);
    byteBuffer.position(p);
    return buffer;
  }


  public int transferTo(SocketChannel socketChanel) throws IOException {
    if (readPos > writePos) {
      throw new IOException("readPos more than writePos，this is error！");
    }
    byteBuffer.position(readPos);
    byteBuffer.limit(writePos);
    int writeNum = socketChanel.write(byteBuffer);
    if (writeNum > 0) {
      readPos += writeNum;
    }
    return writeNum;
  }


  public int writingPos() {
    return writePos;
  }


  public int readPos() {
    return readPos;
  }


  public int totalSize() {
    return byteBuffer.capacity();
  }


  public void setWritingPos(int writingPos) {
    this.writePos = writingPos;
  }


  public void setReadingPos(int readingPos) {
    this.readPos = readingPos;
  }


  public boolean isFull() {
    return byteBuffer.position() == byteBuffer.capacity();
  }

  public boolean hasRemaining() {
    return readPos < writePos;
  }


  public void recycle() {
    this.readPos = 0;
    this.writePos = 0;
    this.byteBuffer.clear();
    if (bufferPool != null) {
      bufferPool.recycle(byteBuffer);
    }
  }
}
