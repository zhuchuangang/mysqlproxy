package com.szss.mysqlproxy.frontend;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.frontend.state.FrontendInitialHandshakeState;
import com.szss.mysqlproxy.frontend.state.FrontendState;
import com.szss.mysqlproxy.net.Connection;
import com.szss.mysqlproxy.net.buffer.ConByteBuffer;
import com.szss.mysqlproxy.protocol.MySQLPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * Created by zcg on 2017/5/2.
 */
public class FrontendConnection extends Connection {

    private static Logger logger = LogManager.getLogger(FrontendConnection.class);

    private FrontendState state;

    private BackendConnection backendConnection;

    private LinkedList<NoneBlockTask> taskQueue;

    public static final int COMMAND_STATUS = 60;
    public static final int LOAD_DATA_STATUS = 61;

    public FrontendConnection(String reactorName, SocketChannel socketChannel,
                              ConByteBuffer readBuffer,
                              ConByteBuffer writeBuffer) throws IOException {
        super();
        this.reactorName = reactorName;
        this.state = FrontendInitialHandshakeState.instance();
        this.socketChannel = socketChannel;
        this.socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        this.socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024 * 1024);
        this.socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 1024);
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
        this.taskQueue = new LinkedList();
    }

    @Override
    public void onConnection() throws IOException {
        logger.info("Front connection send handshake packet to client!");
        state.handle(this);
    }

    @Override
    public void handle() throws IOException {
        state.handle(this);
    }

    public void setState(FrontendState state) {
        this.state = state;
    }

    public void setBackendConnection(BackendConnection backendConnection) {
        this.backendConnection = backendConnection;
        if (this.backendConnection != null) {
            this.backendConnection.setFrontendConnection(this);
        }
    }

    public BackendConnection getBackendConnection() {
        return backendConnection;
    }

    public LinkedList<NoneBlockTask> getTaskQueue() {
        return taskQueue;
    }


    public void nextConnectionState(byte packetType) {
        switch (connectionState) {
            case Connection.CONNECTING_STATE:
                if (packetType == MySQLPacket.OK_PACKET || packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(Connection.IDLE_STATE);
                }
                break;
            case Connection.IDLE_STATE:
                if (packetType == MySQLPacket.COM_QUERY) {
                    this.setConnectionState(COMMAND_STATUS);
                }
                break;
            case COMMAND_STATUS:
                if (packetType == MySQLPacket.OK_PACKET || packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(Connection.IDLE_STATE);
                }
                //load data infile第一个响应包的类型是0xfb
                if (packetType == 0xfb) {
                    this.setConnectionState(LOAD_DATA_STATUS);
                }
                break;
            case LOAD_DATA_STATUS:
                if (packetType == MySQLPacket.OK_PACKET || packetType == MySQLPacket.ERROR_PACKET) {
                    this.setConnectionState(Connection.IDLE_STATE);
                }
                break;
        }
    }
}
