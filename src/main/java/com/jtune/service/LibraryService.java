package com.jtune.service;

import com.jtune.database.DatabaseManager;
import com.jtune.database.PlaylistRepository;
import com.jtune.database.PlaylistSongRepository;
import com.jtune.database.SongRepository;
import com.jtune.model.Playlist;
import com.jtune.model.Song;

import java.util.List;

public class LibraryService {
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;

    public LibraryService() {
        DatabaseManager manager = new DatabaseManager();
        this.songRepository = new SongRepository(manager);
        this.playlistRepository = new PlaylistRepository(manager);
        this.playlistSongRepository = new PlaylistSongRepository(manager);
    }

    public List<Song> getAllSongs() {
        return songRepository.getAllSongs();
    }

    public List<Song> searchSongs(String query) {
        if (query == null || query.isBlank()) {
            return getAllSongs();
        }
        return songRepository.searchSongs(query.trim());
    }

    public List<Song> getRecentSongs() {
        return songRepository.getRecentSongs(6);
    }

    public List<String> getArtists() {
        return songRepository.getAllArtists();
    }

    public List<Song> getSongsByArtist(String artist) {
        return songRepository.getSongsByArtist(artist);
    }

    public List<Playlist> getPlaylists() {
        return playlistRepository.getAllPlaylists();
    }

    public Playlist createPlaylist(String name) {
        return playlistRepository.createPlaylist(name);
    }

    public boolean deletePlaylist(int playlistId) {
        return playlistRepository.deletePlaylist(playlistId);
    }

    public void addSongToPlaylist(int playlistId, int songId) {
        playlistSongRepository.addSongToPlaylist(playlistId, songId);
    }

    public List<Song> getSongsForPlaylist(int playlistId) {
        return playlistSongRepository.getSongsForPlaylist(playlistId);
    }

    public boolean removeSongFromPlaylist(int playlistId, int songId) {
        return playlistSongRepository.removeSongFromPlaylist(playlistId, songId);
    }
}
