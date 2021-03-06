/*-
 * <<
 * DBus
 * ==
 * Copyright (C) 2016 - 2017 Bridata
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

package com.creditease.dbus.heartbeat.container;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp.BasicDataSource;

import com.creditease.dbus.heartbeat.log.LoggerFactory;
import com.creditease.dbus.heartbeat.vo.JdbcVo;

public class DataSourceContainer {

    private static DataSourceContainer container;

    private ConcurrentHashMap<String, BasicDataSource> cmap = 
            new ConcurrentHashMap<String, BasicDataSource>();

    private DataSourceContainer() {
    }

    public static DataSourceContainer getInstance() {
        if (container == null) {
            synchronized (DataSourceContainer.class) {
                if (container == null)
                    container = new DataSourceContainer();
            }
        }
        return container;
    }

    public boolean register(JdbcVo conf) {
        boolean isOk = true;
        try {
            BasicDataSource bds = new BasicDataSource();
            bds.setDriverClassName(conf.getDriverClass());
            bds.setUrl(conf.getUrl());
            bds.setUsername(conf.getUserName());
            bds.setPassword(conf.getPassword());
            bds.setInitialSize(conf.getInitialSize());
            bds.setMaxActive(conf.getMaxActive());
            bds.setMaxIdle(conf.getMaxIdle());
            bds.setMinIdle(conf.getMinIdle());
            cmap.put(conf.getKey(), bds);
        } catch (Exception e) {
            LoggerFactory.getLogger().error("[db container initThreadPool key " + conf.getKey() + " datasource error!]", e);
            isOk = false;
        }
        return isOk;
    }

    public Connection getConn(String key) {
        Connection conn = null;
        try {
            if (cmap.containsKey(key))
                conn = cmap.get(key).getConnection();
        } catch (SQLException e) {
            LoggerFactory.getLogger().error("[db container get key: " + key + " conn error!]", e);
        }
        return conn;
    }

    public void release(String key) {
        try {
            if (cmap.containsKey(key))
                cmap.get(key).close();
        } catch (SQLException e) {
            LoggerFactory.getLogger().error("[db container close key: " + key + " datasource error!]", e);
        }
    }

    public void clear() {
        Enumeration<String> keys = cmap.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            release(key);
        }
        cmap.clear();
    }
}
