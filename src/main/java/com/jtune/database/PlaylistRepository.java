package com.jtune.database;

import com.jtune.model.Playlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistRepository {
    private final DatabaseManager databaseManager;

    public PlaylistRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Playlist createPlaylist(String name) {
        String sql = "INSERT INTO playlists (name) VALUES (?);";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            int id = 0;
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
            return new Playlist(id, name);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create playlist.", e);
        }
    }

    public boolean deletePlaylist(int playlistId) {
        String sql = "DELETE FROM playlists WHERE id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete playlist.", e);
        }
    }

    public List<Playlist> getAllPlaylists() {
        String sql = "SELECT id, name FROM playlists ORDER BY name COLLATE NOCASE;";
        List<Playlist> playlists = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                playlists.add(new Playlist(rs.getInt("id"), rs.getString("name")));
            }
            return playlists;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read playlists.", e);
        }
    }
}
