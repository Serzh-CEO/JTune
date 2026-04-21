package com.jtune.service;

import com.jtune.model.Song;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerService {

    public enum Mode { QUEUE, SINGLE }

    private final List<Song> queue = new ArrayList<>();
    private int currentIndex = -1;
    private MediaPlayer mediaPlayer;
    private Mode mode = Mode.SINGLE;

    private Consumer<Song> onSongChanged;
    private Runnable onStopped;
    private Consumer<Duration> onProgress;
    private Consumer<Duration> onDurationReady;
    private Runnable onEndReached;

    public void setOnSongChanged(Consumer<Song> cb)       { this.onSongChanged = cb; }
    public void setOnStopped(Runnable cb)                 { this.onStopped = cb; }
    public void setOnProgress(Consumer<Duration> cb)      { this.onProgress = cb; }
    public void setOnDurationReady(Consumer<Duration> cb) { this.onDurationReady = cb; }
    public void setOnEndReached(Runnable cb)              { this.onEndReached = cb; }

    public void setQueue(List<Song> songs) {
        queue.clear();
        queue.addAll(songs);
        mode = Mode.QUEUE;
    }

    public Mode getMode() { return mode; }

    public Song playSong(Song song) {
        int index = queue.indexOf(song);
        if (index == -1) {
            mode = Mode.SINGLE;
            queue.clear();
            queue.add(song);
            currentIndex = 0;
        } else {
            mode = Mode.QUEUE;
            currentIndex = index;
        }
        startCurrentSong();
        return getCurrentSong();
    }

    public Song next() {
        if (queue.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % queue.size();
        startCurrentSong();
        return getCurrentSong();
    }

    public Song previous() {
        if (queue.isEmpty()) return null;
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3) {
            mediaPlayer.seek(Duration.ZERO);
            return getCurrentSong();
        }
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        startCurrentSong();
        return getCurrentSong();
    }

    public void togglePause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (onStopped != null) Platform.runLater(onStopped);
    }

    public void setVolume(double volume) {
        if (mediaPlayer != null) mediaPlayer.setVolume(volume);
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) mediaPlayer.seek(duration);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public boolean hasMedia() { return mediaPlayer != null; }

    public MediaPlayer getMediaPlayer() { return mediaPlayer; }

    public Song getCurrentSong() {
        if (currentIndex < 0 || currentIndex >= queue.size()) return null;
        return queue.get(currentIndex);
    }

    private void startCurrentSong() {
        Song current = getCurrentSong();
        if (current == null) return;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        Media media = new Media(new File(current.getFilePath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(0.7);

        mediaPlayer.setOnReady(() -> {
            if (onDurationReady != null)
                Platform.runLater(() -> onDurationReady.accept(mediaPlayer.getTotalDuration()));
        });
        mediaPlayer.currentTimeProperty().addListener((obs, old, now) -> {
            if (onProgress != null) Platform.runLater(() -> onProgress.accept(now));
        });
        mediaPlayer.setOnEndOfMedia(() -> {
            if (mode == Mode.QUEUE && queue.size() > 1) {
                Song nextSong = next();
                if (onSongChanged != null && nextSong != null)
                    Platform.runLater(() -> onSongChanged.accept(nextSong));
            } else {
                if (onEndReached != null) Platform.runLater(onEndReached);
            }
        });
        mediaPlayer.play();
        if (onSongChanged != null) Platform.runLater(() -> onSongChanged.accept(current));
    }
}
