package com.jtune.controller;

import com.jtune.model.Playlist;
import com.jtune.model.Song;
import com.jtune.service.CoverArtService;
import com.jtune.service.LibraryService;
import com.jtune.service.PlayerService;
import com.jtune.service.RecommendationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

public class MainController {

    private final LibraryService libraryService = new LibraryService();
    private final PlayerService playerService = new PlayerService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final CoverArtService coverArtService = new CoverArtService();

    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private final ObservableList<String> artists = FXCollections.observableArrayList();
    private final ObservableList<Song> songs = FXCollections.observableArrayList();

    private boolean isSeeking = false;
    private Integer activePlaylistId = null;
    private String activeArtist = null;

    // ── FXML fields ────────────────────────────────────────────────────────────

    @FXML private TextField searchField;
    @FXML private ListView<Playlist> playlistListView;
    @FXML private ListView<String> artistListView;
    @FXML private ListView<Song> songListView;
    @FXML private FlowPane recommendationPane;
    @FXML private Label nowPlayingTitleLabel;
    @FXML private Label nowPlayingArtistLabel;
    @FXML private ImageView nowPlayingCoverImageView;
    @FXML private Label contentModeLabel;

    // Player bar
    @FXML private Label bottomSongTitleLabel;
    @FXML private Label bottomSongArtistLabel;
    @FXML private ImageView playerBarCoverView;
    @FXML private Button prevButton;
    @FXML private Button playPauseButton;
    @FXML private Button stopButton;
    @FXML private Button nextButton;
    @FXML private Label elapsedLabel;
    @FXML private Slider progressSlider;
    @FXML private Label durationLabel;
    @FXML private Slider volumeSlider;

    // ── Initialize ─────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        playlistListView.setItems(playlists);
        artistListView.setItems(artists);
        songListView.setItems(songs);

        setupSongCells();
        loadPlaylists();
        loadArtists();
        loadAllSongs();
        loadRecommendations();

        // Search
        searchField.textProperty().addListener((obs, o, n) -> applyCurrentFilterWithSearch(n));

        // Double-click to play from queue
        songListView.setOnMouseClicked(event -> {
            Song selected = songListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                playFromQueue(selected);
            }
        });

        // Playlist selection
        playlistListView.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected == null) { activePlaylistId = null; loadAllSongs(); return; }
            activePlaylistId = selected.getId();
            activeArtist = null;
            artistListView.getSelectionModel().clearSelection();
            contentModeLabel.setText("Плейлист: " + selected.getName());
            songs.setAll(libraryService.getSongsForPlaylist(selected.getId()));
            applyCurrentFilterWithSearch(searchField.getText());
        });

        // Artist selection
        artistListView.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel == null) { activeArtist = null; if (activePlaylistId == null) loadAllSongs(); return; }
            activeArtist = sel;
            activePlaylistId = null;
            playlistListView.getSelectionModel().clearSelection();
            contentModeLabel.setText("Артист: " + sel);
            songs.setAll(libraryService.getSongsByArtist(sel));
            applyCurrentFilterWithSearch(searchField.getText());
        });

        // Volume slider
        volumeSlider.setValue(70);
        volumeSlider.valueProperty().addListener((obs, o, v) -> playerService.setVolume(v.doubleValue() / 100.0));

        // Progress slider (user seeking)
        progressSlider.valueProperty().addListener((obs, o, newVal) -> {
            if (progressSlider.isValueChanging() && playerService.hasMedia()) {
                isSeeking = true;
                playerService.seek(Duration.seconds(newVal.doubleValue()));
                isSeeking = false;
            }
        });

        // PlayerService callbacks
        playerService.setOnSongChanged(this::syncPlayerUi);
        playerService.setOnStopped(this::resetPlayerUi);
        playerService.setOnDurationReady(total -> {
            progressSlider.setMax(total.toSeconds());
            durationLabel.setText(formatDuration(total));
        });
        playerService.setOnProgress(now -> {
            if (!isSeeking) {
                progressSlider.setValue(now.toSeconds());
                elapsedLabel.setText(formatDuration(now));
            }
        });
        playerService.setOnEndReached(() -> {
            playPauseButton.setText("▶");
        });

        resetPlayerUi();
    }

    // ── FXML actions ───────────────────────────────────────────────────────────

    @FXML private void onSearchAction() { songs.setAll(libraryService.searchSongs(searchField.getText())); }
    @FXML private void onClearSearch()  { searchField.clear(); applyCurrentFilterWithSearch(""); }

    @FXML
    private void onCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("New Playlist");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        String name = result.get().trim();
        if (name.isEmpty()) { showError("Playlist name cannot be empty."); return; }
        try { libraryService.createPlaylist(name); loadPlaylists(); loadArtists(); }
        catch (Exception e) { showError("Cannot create playlist: " + e.getMessage()); }
    }

    @FXML
    private void onDeletePlaylist() {
        Playlist selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        libraryService.deletePlaylist(selected.getId());
        activePlaylistId = null;
        loadPlaylists();
        applyCurrentFilterWithSearch(searchField.getText());
    }

    @FXML
    private void onPlayPause() {
        if (!playerService.hasMedia()) return;
        playerService.togglePause();
        updatePlayPauseButton();
    }

    @FXML
    private void onStop() {
        playerService.stop();
    }

    @FXML
    private void onNext() {
        Song song = playerService.next();
        if (song != null) syncPlayerUi(song);
    }

    @FXML
    private void onPrevious() {
        Song song = playerService.previous();
        if (song != null) syncPlayerUi(song);
    }

    // ── Playback helpers ───────────────────────────────────────────────────────

    /** Play from the current visible list → enables Prev/Next. */
    private void playFromQueue(Song song) {
        List<Song> queue = songs.isEmpty() ? libraryService.getAllSongs() : List.copyOf(songs);
        playerService.setQueue(queue);
        playerService.playSong(song);
        // syncPlayerUi is called via callback
    }

    private void syncPlayerUi(Song song) {
        if (song == null) return;

        // Right panel
        nowPlayingTitleLabel.setText(song.getTitle());
        nowPlayingArtistLabel.setText(song.getArtist());
        nowPlayingCoverImageView.setImage(coverArtService.getCover(song.getFilePath()));

        // Bottom bar info
        bottomSongTitleLabel.setText(song.getTitle());
        bottomSongArtistLabel.setText(song.getArtist());
        playerBarCoverView.setImage(coverArtService.getCover(song.getFilePath()));

        // Show/hide Prev+Next depending on mode
        boolean queueMode = playerService.getMode() == PlayerService.Mode.QUEUE;
        prevButton.setVisible(queueMode);
        prevButton.setManaged(queueMode);
        nextButton.setVisible(queueMode);
        nextButton.setManaged(queueMode);
        stopButton.setVisible(!queueMode);
        stopButton.setManaged(!queueMode);

        playPauseButton.setText("⏸");

        // Reset progress
        progressSlider.setValue(0);
        elapsedLabel.setText("00:00");
        durationLabel.setText("00:00");
    }

    private void resetPlayerUi() {
        nowPlayingTitleLabel.setText("Ничего не играет");
        nowPlayingArtistLabel.setText("-");
        nowPlayingCoverImageView.setImage(null);
        bottomSongTitleLabel.setText("Ничего не играет");
        bottomSongArtistLabel.setText("-");
        playerBarCoverView.setImage(null);
        playPauseButton.setText("▶");
        progressSlider.setValue(0);
        elapsedLabel.setText("00:00");
        durationLabel.setText("00:00");
        prevButton.setVisible(false);
        prevButton.setManaged(false);
        nextButton.setVisible(false);
        nextButton.setManaged(false);
        stopButton.setVisible(true);
        stopButton.setManaged(true);
    }

    private void updatePlayPauseButton() {
        playPauseButton.setText(playerService.isPlaying() ? "⏸" : "▶");
    }

    // ── List helpers ───────────────────────────────────────────────────────────

    private void loadPlaylists() { playlists.setAll(libraryService.getPlaylists()); }
    private void loadArtists()   { artists.setAll(libraryService.getArtists()); }

    private void loadAllSongs() {
        contentModeLabel.setText("Все песни");
        songs.setAll(libraryService.getAllSongs());
    }

    private void applyCurrentFilterWithSearch(String query) {
        List<Song> base;
        if (activePlaylistId != null) {
            base = libraryService.getSongsForPlaylist(activePlaylistId);
        } else if (activeArtist != null) {
            base = libraryService.getSongsByArtist(activeArtist);
        } else {
            base = libraryService.getAllSongs();
            contentModeLabel.setText("Все песни");
        }
        if (query == null || query.isBlank()) { songs.setAll(base); return; }
        String q = query.trim().toLowerCase();
        songs.setAll(base.stream()
                .filter(s -> contains(s.getTitle(), q) || contains(s.getArtist(), q))
                .toList());
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    // ── Song cells ─────────────────────────────────────────────────────────────

    private void setupSongCells() {
        songListView.setCellFactory(list -> new ListCell<>() {
            private final Label textLabel   = new Label();
            private final Label subLabel    = new Label();
            private final Button addButton  = new Button("+");
            private final Button removeButton = new Button("−");
            private final ImageView cover   = new ImageView();
            private final VBox textBox      = new VBox(2, textLabel, subLabel);
            private final HBox box          = new HBox(10, cover, textBox, addButton, removeButton);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.getStyleClass().add("song-card");
                cover.setFitWidth(46);
                cover.setFitHeight(46);
                cover.setPreserveRatio(true);
                cover.getStyleClass().add("song-cover");
                textLabel.getStyleClass().add("song-card-title");
                subLabel.getStyleClass().add("song-card-subtitle");
                addButton.getStyleClass().add("song-add-btn");
                removeButton.getStyleClass().add("song-remove-btn");
                addButton.setOnAction(e -> {
                    Song s = getItem();
                    if (s != null) openAddToPlaylistPopup(s);
                });
                removeButton.setOnAction(e -> {
                    Song s = getItem();
                    if (s != null && activePlaylistId != null) {
                        libraryService.removeSongFromPlaylist(activePlaylistId, s.getId());
                        applyCurrentFilterWithSearch(searchField.getText());
                    }
                });
            }

            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                textLabel.setText(item.getTitle());
                subLabel.setText(item.getArtist());
                cover.setImage(coverArtService.getCover(item.getFilePath()));
                removeButton.setVisible(activePlaylistId != null);
                removeButton.setManaged(activePlaylistId != null);
                setGraphic(box);
            }
        });
    }

    private void openAddToPlaylistPopup(Song song) {
        List<Playlist> available = libraryService.getPlaylists();
        if (available.isEmpty()) { showError("Create playlist first."); return; }
        ChoiceDialog<Playlist> dialog = new ChoiceDialog<>(available.get(0), available);
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Add \"" + song.getTitle() + "\"");
        dialog.setContentText("Choose playlist:");
        dialog.showAndWait().ifPresent(p -> libraryService.addSongToPlaylist(p.getId(), song.getId()));
    }

    // ── Recommendations ────────────────────────────────────────────────────────

    private void loadRecommendations() {
        recommendationPane.getChildren().clear();
        List<Song> allSongs = libraryService.getAllSongs();

        for (Song song : recommendationService.randomSongs(allSongs, 4))
            recommendationPane.getChildren().add(createRecommendCard(song, "Mix"));

        for (String artist : recommendationService.randomArtists(allSongs, 3)) {
            Button card = new Button("Artist Mix\n" + artist);
            card.getStyleClass().add("recommend-card-artist");
            card.setOnAction(e -> {
                activeArtist = artist;
                activePlaylistId = null;
                playlistListView.getSelectionModel().clearSelection();
                artistListView.getSelectionModel().select(artist);
                contentModeLabel.setText("Артист: " + artist);
                applyCurrentFilterWithSearch(searchField.getText());
            });
            recommendationPane.getChildren().add(card);
        }

        for (Song recent : libraryService.getRecentSongs())
            recommendationPane.getChildren().add(createRecommendCard(recent, "Recently Added"));
    }

    private StackPane createRecommendCard(Song song, String badge) {
        ImageView cover = new ImageView(coverArtService.getCover(song.getFilePath()));
        cover.setFitWidth(78);
        cover.setFitHeight(78);
        cover.setPreserveRatio(true);
        cover.getStyleClass().add("recommend-cover");
        Label title = new Label(song.getTitle()); title.getStyleClass().add("recommend-title");
        Label artist = new Label(song.getArtist()); artist.getStyleClass().add("recommend-subtitle");
        Label badgeLabel = new Label(badge); badgeLabel.getStyleClass().add("recommend-badge");
        VBox text = new VBox(2, badgeLabel, title, artist);
        HBox body = new HBox(10, cover, text); body.setAlignment(Pos.CENTER_LEFT);
        StackPane card = new StackPane(body); card.getStyleClass().add("recommend-card");
        card.setOnMouseClicked(e -> playFromQueue(song));
        return card;
    }

    // ── Utils ──────────────────────────────────────────────────────────────────

    private String formatDuration(Duration d) {
        int total = (int) Math.floor(d.toSeconds());
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("JTune");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
