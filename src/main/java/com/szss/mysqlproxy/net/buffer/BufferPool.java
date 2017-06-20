package com.szss.mysqlproxy.net.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Created by zcg on 2017/6/15.
 */
public class BufferPool {

  private final int pageSize;
  private final int chunkSize;
  private List<ByteBuffer> pages;
  private ByteBuffer currentBuffer;
  private int createdChunkCount;
  private int totalChunkCount;
  private final LinkedTransferQueue<ByteBuffer> freeBuffers = new LinkedTransferQueue<ByteBuffer>();

  public BufferPool(int pageSize, int chunkSize) {
    this.createdChunkCount = 0;
    this.totalChunkCount = pageSize / chunkSize + (pageSize % chunkSize == 0 ? 0 : 1);
    this.pageSize = totalChunkCount * chunkSize;
    this.chunkSize = chunkSize;
    this.currentBuffer = ByteBuffer.allocate(pageSize);
    this.pages = new ArrayList<>();
    this.pages.add(currentBuffer);
  }


  public ByteBuffer allocate() {
    ByteBuffer node = freeBuffers.poll();
    if (node == null) {
      createdChunkCount++;
      node = this.createDirectBuffer(chunkSize);
    } else {
      node.clear();
    }

    return node;
  }


  private ByteBuffer createDirectBuffer(int size) {
    if (createdChunkCount > totalChunkCount) {
      this.currentBuffer = ByteBuffer.allocate(pageSize);
      this.pages.add(currentBuffer);
      totalChunkCount = createdChunkCount * 2;
    }
    int position = (createdChunkCount - 1) * chunkSize;
    int limit = createdChunkCount * chunkSize;
    currentBuffer.position(position);
    currentBuffer.limit(limit);
    return currentBuffer.slice();
  }

  public void recycle(ByteBuffer buffer) {
    System.out.println(Thread.currentThread().getName() + "recycle buffer ");
    this.freeBuffers.add(buffer);
  }

}
