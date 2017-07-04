package com.szss.mysqlproxy.frontend.state;

import com.szss.mysqlproxy.backend.BackendConnection;
import com.szss.mysqlproxy.backend.BackendConnectionPool;
import com.szss.mysqlproxy.backend.state.BackendCommandResponseState;
import com.szss.mysqlproxy.backend.state.BackendHandshakeResponseState;
import com.szss.mysqlproxy.frontend.FrontendConnection;
import com.szss.mysqlproxy.net.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by zcg on 2017/6/19.
 */
public class FrontendCommandState implements FrontendState {

    private static Logger logger = LogManager.getLogger(FrontendCommandState.class);

    private static FrontendCommandState state;

    private FrontendCommandState() {
    }

    public static FrontendCommandState instance() {
        if (state == null) {
            state = new FrontendCommandState();
        }
        return state;
    }

    @Override
    public void handle(FrontendConnection connection) throws IOException {
        //后端连接是否是从连接池中获取的
        boolean formPool = false;
        if (connection.getBackendConnection() == null) {
            formPool = true;
            BackendConnectionPool connectionPool = BackendConnectionPool.getInstance();
            BackendConnection backendCon = connectionPool.connection(connection.getReactorName());
            connection.setBackendConnection(backendCon);
        }
        BackendConnection backendCon = connection.getBackendConnection();
        if (backendCon.getConnectionState() == Connection.CONNECTING_STATE) {
            connection.getTaskQueue().add(() -> {
                logger.info("mysql backend connection is idle,share the buffer of front connection!");
                if (backendCon.getReadBuffer() != connection.getWriteBuffer()) {
                    backendCon.setReadBuffer(connection.getWriteBuffer());
                }
                if (backendCon.getWriteBuffer() != connection.getReadBuffer()) {
                    backendCon.setWriteBuffer(connection.getReadBuffer());
                }
                backendCon.enableWrite(false);
            });
        } else {
            //如果后端连接是从其他闲置连接借用过来或者从连接池中获取的，需要后端连接需要绑定前段连接的buffer
            if (formPool) {
                backendCon.setState(BackendCommandResponseState.instance());
                backendCon.setWriteBuffer(connection.getReadBuffer());
                backendCon.setReadBuffer(connection.getWriteBuffer());
            }
            //根据前段报文类型，推动状态机状态变化
            byte packetType = connection.getBackendConnection().getWriteBuffer().getByte(4);
            connection.getBackendConnection().nextConnectionState(packetType);
            logger.info("frontend enable write");
            connection.getBackendConnection().enableWrite(false);
        }
    }
}
