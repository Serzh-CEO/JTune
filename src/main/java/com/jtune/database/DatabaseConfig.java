package com.jtune.database;

public final class DatabaseConfig {
    private static final String DB_URL = "jdbc:sqlite:jtune.db";

    private DatabaseConfig() {
    }

    public static String getDbUrl() {
        return DB_URL;
    }
}
