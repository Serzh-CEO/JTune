package com.jtune.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public void initialize() {
        try (Connection connection = DriverManager.getConnection(DatabaseConfig.getDbUrl());
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS songs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        file_path TEXT NOT NULL UNIQUE,
                        added_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS playlist_songs (
                        playlist_id INTEGER NOT NULL,
                        song_id INTEGER NOT NULL,
                        PRIMARY KEY (playlist_id, song_id),
                        FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
                    );
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize JTune database.", e);
        }
    }
}
