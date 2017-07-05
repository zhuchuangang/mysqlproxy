package com.szss.mysqlproxy.backend;

import com.szss.mysqlproxy.backend.state.BackendHandshakeResponseState;
import com.szss.mysqlproxy.backend.state.BackendState;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import com.szss.mysqlproxy.protocol.MySQLPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by zcg on 2017/6/13.
 */
public class BackendConnection extends Connection {

    private static Logger logger = LogManager.getLogger(BackendConnection.class);

    public static final int CMD_QUERY_STATUS = 11;
    public static final int RESULT_WAIT_STATUS = 21;
    public static final int RESULT_INIT_STATUS = 22;
    public static final int RESULT_FETCH_STATUS = 23;
    public static final int RESULT_HEADER_STATUS = 24;
    public static final int RESULT_FAIL_STATUS = 29;

    private BackendState state;
    //还有多少字节包头或包体读完
    private int leftSize = 0;
    //如果是包头没有读完，那么缓存上次度过的一部分包头，header大小为4个字节
    private byte[] header;

    private FrontendConnection frontendConnection;

    public BackendConnection(String reactorName, SocketChannel socketChannel,
                             ConByteBuffer readBuffer,
                             ConByteBuffer writeBuffer) {
        super();
        this.leftSize = 0;
        this.reactorName = reactorName;
        this.socketChannel = socketChannel;
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
        this.state = BackendHandshakeResponseState.instance();
    }


    @Override
    public void onConnection() throws IOException {
        logger.info("Backend connection connect mysql server!");
    }

    @Override
    public void handle() throws IOException {
        state.handle(this);
    }


    public void nextConnectionState(byte packetType, int offset) throws IOException {
        if (packetType == MySQLPacket.EOF_PACKET) {
            setServerStatus(readBuffer, offset);
        }
        nextConnectionState(packetType);
    }

    public void nextConnectionState(byte packetType) {
        //logger.info("The state of the back connection is {}",connectionState);
        switch (connectionState) {
            case Connection.CONNECTING_STATE:
                if (packetType == MySQLPacket.OK_PACKET) {
                    this.setConnectionState(Connection.IDLE_STATE);
                }
                break;
            case Connection.IDLE_STATE:
                if (packetType == MySQLPacket.COM_QUERY) {
                    this.setConnectionState(BackendConnection.RESULT_INIT_STATUS);
                } else if (packetType == MySQLPacket.COM_QUIT) {
                    this.setConnectionState(BackendConnection.IDLE_STATE);
                }
                break;

            case BackendConnection.RESULT_INIT_STATUS:
                if (packetType == MySQLPacket.OK_PACKET || packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(BackendConnection.IDLE_STATE);
                } else {
                    this.setConnectionState(BackendConnection.RESULT_HEADER_STATUS);
                }
                break;

            case BackendConnection.RESULT_HEADER_STATUS:
                if (packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(BackendConnection.IDLE_STATE);
                } else if (packetType == MySQLPacket.EOF_PACKET) {
                    this.setConnectionState(BackendConnection.RESULT_FETCH_STATUS);
                }
                break;

            case BackendConnection.RESULT_FETCH_STATUS:
                if ((packetType == MySQLPacket.EOF_PACKET && !multiQuery) || packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(BackendConnection.IDLE_STATE);
                }
                if (packetType == MySQLPacket.EOF_PACKET && multiQuery) {
                    this.setConnectionState(BackendConnection.RESULT_INIT_STATUS);
                }
                break;
        }
        //logger.info("The next state of the back connection is {}", connectionState);
    }

    public void reset() {
        //解除前段连接的绑定
        frontendConnection.setBackendConnection(null);
        frontendConnection = null;
        //重置所有mysql服务状态
        resetServerStatus();
        //解除前段buffer绑定
        readBuffer = null;
        writeBuffer = null;
        executionTimeAtLast = System.currentTimeMillis();
    }

    public BackendState getState() {
        return state;
    }

    public void setState(BackendState state) {
        this.state = state;
    }

    public FrontendConnection getFrontendConnection() {
        return frontendConnection;
    }

    public void setFrontendConnection(FrontendConnection frontendConnection) {
        this.frontendConnection = frontendConnection;
    }

    public int getLeftSize() {
        return leftSize;
    }

    public void setLeftSize(int leftSize) {
        this.leftSize = leftSize;
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }
}
