package com.jtune.service;

import com.jtune.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendationService {
    private final Random random = new Random();

    public List<Song> randomSongs(List<Song> songs, int limit) {
        List<Song> copy = new ArrayList<>(songs);
        Collections.shuffle(copy, random);
        return copy.stream().limit(limit).toList();
    }

    public List<String> randomArtists(List<Song> songs, int limit) {
        Set<String> artists = songs.stream()
                .map(Song::getArtist)
                .filter(artist -> artist != null && !artist.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> list = new ArrayList<>(artists);
        Collections.shuffle(list, random);
        return list.stream().limit(limit).toList();
    }
}
