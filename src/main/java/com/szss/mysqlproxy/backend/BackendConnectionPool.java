package com.szss.mysqlproxy.backend;

import com.szss.mysqlproxy.util.SystemConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zcg on 2017/5/4.
 */
public class BackendConnectionPool {

    private static Logger logger = LogManager.getLogger(BackendConnectionPool.class);
    private ConcurrentHashMap<String, List<BackendConnection>> conMap;
    private ConcurrentHashMap<String, List<BackendConnection>> usedConMap;
    private static BackendConnectionPool pool;

    private BackendConnectionPool() {
        conMap = new ConcurrentHashMap<>();
        usedConMap = new ConcurrentHashMap<>();
    }

    public static BackendConnectionPool getInstance() {
        if (pool == null) {
            pool = new BackendConnectionPool();
        }
        return pool;
    }

    public void addConnection(BackendConnection connection) {
        List<BackendConnection> backendCons = conMap.get(connection.getReactorName());
        if (backendCons == null) {
            backendCons = new LinkedList<>();
        }
        backendCons.add(connection);
        this.conMap.put(connection.getReactorName(), backendCons);
    }

    public BackendConnection connection(String reactorName) throws IOException {
        BackendConnection chooseCon = null;
        List<BackendConnection> poolCons = conMap.get(reactorName);
        if (poolCons != null && !poolCons.isEmpty()) {
            chooseCon = poolCons.remove(0);
        }
        if (chooseCon == null) {
            List<BackendConnection> usedCons = usedConMap.get(reactorName);
            if (usedCons != null) {
                logger.debug("The count of the backend used connections is {}", usedCons.size());
                int index = -1;
                long maxInterval = 0;
                long currentTime = System.currentTimeMillis();
                long defaultMaxInterval = SystemConfig.instance().getIdleMaxInterval();
                //后端连接处于空闲 不在事务过程中 后端连接空闲时间最长
                for (int i = 0; i < usedCons.size(); i++) {
                    BackendConnection c = usedCons.get(i);
                    long currentInterval = currentTime - c.getExecutionTimeAtLast();
                    if (c.idle() && defaultMaxInterval < currentInterval && maxInterval < currentInterval) {
                        maxInterval = currentTime - c.getExecutionTimeAtLast();
                        index = i;
                    }
                }
                if (index != -1) {
                    chooseCon = usedCons.get(index);
                    logger.debug("The backend connection which is chosen is {}", chooseCon);
                    chooseCon.reset();
                    return chooseCon;
                }
            }
        }
        if (chooseCon == null) {
            chooseCon = BackendConnectionFactory.make(reactorName);
        }
        List<BackendConnection> usedCons = usedConMap.get(reactorName);
        if (usedCons == null) {
            usedCons = new ArrayList<>();
            usedConMap.put(reactorName, usedCons);
        }
        usedCons.add(chooseCon);
        return chooseCon;
    }

}
