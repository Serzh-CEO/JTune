package com.jtune.database;

import com.jtune.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SongRepository {
    private final DatabaseManager databaseManager;

    public SongRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void upsertSong(String title, String artist, String filePath) {
        String sql = """
                INSERT INTO songs (title, artist, file_path)
                VALUES (?, ?, ?)
                ON CONFLICT(file_path) DO UPDATE SET
                    title = excluded.title,
                    artist = excluded.artist;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, artist);
            statement.setString(3, filePath);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save song " + filePath, e);
        }
    }

    public List<Song> getAllSongs() {
        String sql = """
                SELECT id, title, artist, file_path, added_at
                FROM songs
                ORDER BY title COLLATE NOCASE;
                """;
        return querySongs(sql, null);
    }

    public List<Song> searchSongs(String query) {
        String sql = """
                SELECT id, title, artist, file_path, added_at
                FROM songs
                WHERE lower(title) LIKE lower(?) OR lower(artist) LIKE lower(?)
                ORDER BY title COLLATE NOCASE;
                """;
        String wildcard = "%" + query + "%";
        return querySongs(sql, statement -> {
            statement.setString(1, wildcard);
            statement.setString(2, wildcard);
        });
    }

    public List<Song> getRecentSongs(int limit) {
        String sql = """
                SELECT id, title, artist, file_path, added_at
                FROM songs
                ORDER BY datetime(added_at) DESC
                LIMIT ?;
                """;
        return querySongs(sql, statement -> statement.setInt(1, limit));
    }

    public List<String> getAllArtists() {
        String sql = """
                SELECT DISTINCT artist
                FROM songs
                WHERE artist IS NOT NULL AND trim(artist) <> ''
                ORDER BY artist COLLATE NOCASE;
                """;
        List<String> artists = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                artists.add(rs.getString("artist"));
            }
            return artists;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read artists.", e);
        }
    }

    public List<Song> getSongsByArtist(String artist) {
        String sql = """
                SELECT id, title, artist, file_path, added_at
                FROM songs
                WHERE lower(artist) = lower(?)
                ORDER BY title COLLATE NOCASE;
                """;
        return querySongs(sql, statement -> statement.setString(1, artist));
    }

    private List<Song> querySongs(String sql, SqlConsumer consumer) {
        List<Song> songs = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (consumer != null) {
                consumer.accept(statement);
            }
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
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read songs.", e);
        }
        return songs;
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }
}
