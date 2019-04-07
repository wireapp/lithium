package com.wire.bots.sdk.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.NewBot;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class PostgresState implements State {
    private final static ObjectMapper mapper = new ObjectMapper();

    private final UUID botId;
    private final Configuration.DB conf;

    public PostgresState(String botId, Configuration.DB conf) {
        this.botId = UUID.fromString(botId);
        this.conf = conf;
    }

    @Override
    public boolean saveState(NewBot newBot) throws IOException {
        try (Connection c = newConnection()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(mapper.writeValueAsString(newBot));

            PreparedStatement stmt = c.prepareStatement("INSERT INTO states (botId, bot) VALUES (?, ?)" +
                    " ON CONFLICT (botId) DO UPDATE SET bot = EXCLUDED.bot");
            stmt.setObject(1, botId);
            stmt.setObject(2, jsonObject);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public NewBot getState() throws IOException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT bot FROM states WHERE botId = ?");
            stmt.setObject(1, botId);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                String json = resultSet.getString("bot");
                return mapper.readValue(json, NewBot.class);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
        throw new MissingStateException(botId);
    }

    @Override
    public boolean removeState() throws IOException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM states WHERE botId = ?");
            stmt.setObject(1, botId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public ArrayList<NewBot> listAllStates() {
        return null;
    }

    @Override
    public boolean saveFile(String filename, String content) {
        return false;
    }

    @Override
    public String readFile(String filename) {
        return null;
    }

    @Override
    public boolean deleteFile(String filename) {
        return false;
    }

    @Override
    public boolean saveGlobalFile(String filename, String content) {
        return false;
    }

    @Override
    public String readGlobalFile(String filename) {
        return null;
    }

    @Override
    public boolean deleteGlobalFile(String filename) {
        return false;
    }

    private Connection newConnection() throws SQLException {
        String driver = conf.driver != null ? conf.driver : "postgresql";
        String url = conf.url != null
                ? conf.url
                : String.format("jdbc:%s://%s:%d/%s", driver, conf.host, conf.port, conf.database);
        return DriverManager.getConnection(url, conf.user, conf.password);
    }
}
