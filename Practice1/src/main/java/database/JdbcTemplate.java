package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTemplate {

    private final ConnectionProvider connectionProvider;

    public JdbcTemplate(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public int update(String sql, Object... params) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("update failed: " + sql, e);
        }
    }

    public long insert(String sql, Object... params) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(stmt, params);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new DatabaseException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert failed: " + sql, e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new DatabaseException("query failed: " + sql, e);
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        if (results.isEmpty()) return Optional.empty();
        if (results.size() > 1) throw new DatabaseException("expected 1 row, got " + results.size());
        return Optional.of(results.getFirst());
    }

    public long queryForLong(String sql, Object... params) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParams(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                throw new DatabaseException("queryForLong returned no rows: " + sql);
            }
        } catch (SQLException e) {
            throw new DatabaseException("queryForLong failed: " + sql, e);
        }
    }

    private void bindParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message) {
            super(message);
        }
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}