package directory.robert.spotify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import directory.robert.commands.constants;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DB {

    private static final String DB_URL = "jdbc:sqlite:"+ constants.resources_path +"/db/database.db";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void insertOrUpdateHashMap(String key, Map<String, String> map) {
        String selectSql = "SELECT * FROM hashmap WHERE key = ?";
        String insertSql = "INSERT INTO hashmap(key, value) VALUES(?, ?)";
        String updateSql = "UPDATE hashmap SET value = ? WHERE key = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setString(1, key);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                // Update existing entry
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, objectMapper.writeValueAsString(map));
                updateStmt.setString(2, key);
                updateStmt.executeUpdate();
            } else {
                // Insert new entry
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, key);
                insertStmt.setString(2, objectMapper.writeValueAsString(map));
                insertStmt.executeUpdate();
            }
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static Map<String, String> getHashMap(String key) {
        String sql = "SELECT value FROM hashmap WHERE key = ?";
        Map<String, String> map = new HashMap<>();
        if (key.equals("REFRESH_TOKENS")) {

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String json = rs.getString("value");
                map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            }
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
            }
        }
        return map;
    }

    public static void main() {
        // Create database and table if they do not exist
        SQLiteUtil.createNewDatabase();
        SQLiteUtil.createNewTable();


        //Map<String, String> map1 = new HashMap<>();
        //map1.put("469278229484797962", "AQCryqtJLPfX2HOwNwsr4mjIfqWPqC0KXGa_KxeMqx-Z7HLihL9DI52Etlm2b-Hz8wzVXD5y1xeHMmvJmXxDgG8dylBGdDo_8yTd6bsjv_SA3g6jvGFB5lgIEmiDYNWHu5g");


        // Insert or update HashMaps into SQLite database
        //insertOrUpdateHashMap("REFRESH_TOKENS", map1);

    }
}

class SQLiteUtil {

    private static final String DB_URL = "jdbc:sqlite:"+ constants.resources_path +"/db/database.db";

    public static void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) { }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createNewTable() {
        String sql = "CREATE TABLE IF NOT EXISTS hashmap (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " key TEXT NOT NULL,\n"
                + " value TEXT NOT NULL\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
