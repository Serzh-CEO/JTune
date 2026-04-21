package com.jtune.database;

import com.jtune.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSongRepository {
    private final DatabaseManager databaseManager;

    public PlaylistSongRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void addSongToPlaylist(int playlistId, int songId) {
        String sql = "INSERT OR IGNORE INTO playlist_songs (playlist_id, song_id) VALUES (?, ?);";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add song to playlist.", e);
        }
    }

    public List<Song> getSongsForPlaylist(int playlistId) {
        String sql = """
                SELECT s.id, s.title, s.artist, s.file_path, s.added_at
                FROM songs s
                INNER JOIN playlist_songs ps ON ps.song_id = s.id
                WHERE ps.playlist_id = ?
                ORDER BY s.title COLLATE NOCASE;
                """;
        List<Song> songs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getString("file_path"),
                            rs.getString("added_at")
                    ));
                }
            }
            return songs;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch playlist songs.", e);
        }
    }

    public boolean removeSongFromPlaylist(int playlistId, int songId) {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove song from playlist.", e);
        }
    }
}
