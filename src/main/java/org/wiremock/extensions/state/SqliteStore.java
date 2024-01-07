/*
 * Copyright (C) 2023 Dirk Bolte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.extensions.state;

import com.github.tomakehurst.wiremock.store.Store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import static org.wiremock.extensions.state.internal.ExtensionLogger.logger;

public class SqliteStore implements Store<String, Object> {
    private Connection conn;
    public SqliteStore() {
        conn = null;
        try {
            String url = "jdbc:sqlite:wiremock-persistent-state.db";
            String sql = "CREATE TABLE IF NOT EXISTS states (\n"
                + "	key text PRIMARY KEY,\n"
                + "	object text\n"
                + ");";
            conn = DriverManager.getConnection(url);
            logger().info("DB", "Connection to wiremock-persistent-state.db has been established.");
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            logger().info("DB", e.getMessage());
        }
    }

    private static String serializeToString(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger().info("DB:serializeToString", e.getMessage());
            return null;
        }
    }

    private static Object deserializeFromString(String serializedData) {
        try {
            byte[] data = java.util.Base64.getDecoder().decode(serializedData);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                return ois.readObject();
            }
        } catch (Exception e) {
            logger().info("DB:deserializeFromString", e.getMessage());
            return null;
        }
    }

    @Override
    public Stream<String> getAllKeys() {
        String sql = "SELECT key FROM states";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet resultSet = stmt.executeQuery();
            ArrayList<String> result = new ArrayList<>();

            while(resultSet.next()) {
                result.add(resultSet.getString("key"));
            }

            return result.stream();
        } catch (SQLException ex) {
            logger().info("DB:getAllKeys", ex.getMessage());
            return Stream.empty();
        }
    }

    @Override
    public Optional<Object> get(String key) {
        String sql = "SELECT key, object FROM states WHERE key=?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                String str = resultSet.getString("object");
                logger().info("DB:get-debug", str);

                Object obj = deserializeFromString(str);
                return Optional.of(obj);
            }
            return Optional.empty();
        } catch (SQLException ex) {
            logger().info("DB:get", ex.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void put(String s, Object o) {
        String sql = "INSERT INTO states(key, object) VALUES(?, ?)\n" +
            "ON CONFLICT(key) DO UPDATE SET object=?\n WHERE key=?";
        try {
            String strO = serializeToString(o);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, s);
            stmt.setString(2, strO);
            stmt.setString(3, strO);
            stmt.setString(4, s);

            logger().info("DB:put", strO);

            stmt.execute();
        } catch (SQLException ex) {
            logger().info("DB:put", ex.getMessage());
        }
    }

    @Override
    public void remove(String s) {
        String sql = "DELETE FROM states\n" +
            "WHERE key=?\n";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, s);
            stmt.execute();
        } catch (SQLException ex) {
            logger().info("DB:remove", ex.getMessage());
        }
    }

    @Override
    public void clear() {
        String sql = "TRUNCATE states";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.execute();
        } catch (SQLException ex) {
            logger().info("DB:clear", ex.getMessage());
        }
    }
}
