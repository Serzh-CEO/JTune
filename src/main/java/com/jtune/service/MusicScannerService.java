package com.jtune.service;

import com.jtune.database.DatabaseManager;
import com.jtune.database.SongRepository;
import com.jtune.utils.FileUtils;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;

public class MusicScannerService {
    private static final String MUSIC_DIR = "C:\\Users\\Админ\\Desktop\\musics";

    private final SongRepository songRepository = new SongRepository(new DatabaseManager());

    public void scanAndSyncLibrary() {
        File root = new File(MUSIC_DIR);
        if (!root.exists() || !root.isDirectory()) {
            return;
        }
        scanFolder(root);
    }

    private void scanFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanFolder(file);
                continue;
            }
            if (!file.getName().toLowerCase().endsWith(".mp3")) {
                continue;
            }
            SongMeta meta = readMeta(file);
            songRepository.upsertSong(meta.title(), meta.artist(), file.getAbsolutePath());
        }
    }

    private SongMeta readMeta(File file) {
        String title = FileUtils.fileNameWithoutExtension(file);
        String artist = "Unknown Artist";
        try {
            Mp3File mp3File = new Mp3File(file.getAbsolutePath());
            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2 = mp3File.getId3v2Tag();
                if (id3v2.getArtist() != null && !id3v2.getArtist().isBlank()) {
                    artist = id3v2.getArtist().trim();
                }
                if (id3v2.getTitle() != null && !id3v2.getTitle().isBlank()) {
                    title = id3v2.getTitle().trim();
                }
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1 = mp3File.getId3v1Tag();
                if (id3v1.getArtist() != null && !id3v1.getArtist().isBlank()) {
                    artist = id3v1.getArtist().trim();
                }
                if (id3v1.getTitle() != null && !id3v1.getTitle().isBlank()) {
                    title = id3v1.getTitle().trim();
                }
            }
        } catch (IOException | UnsupportedTagException | InvalidDataException ignored) {
            // Keep fallback file name and unknown artist.
        }
        return new SongMeta(title, artist);
    }

    private record SongMeta(String title, String artist) {
    }
}
