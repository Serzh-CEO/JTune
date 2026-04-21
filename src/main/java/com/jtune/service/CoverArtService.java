package com.jtune.service;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoverArtService {
    private final Map<String, Image> cache = new ConcurrentHashMap<>();
    private final Image placeholder = createPlaceholder();

    public Image getCover(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return placeholder;
        }
        return cache.computeIfAbsent(filePath, this::loadCoverInternal);
    }

    private Image loadCoverInternal(String filePath) {
        try {
            Mp3File mp3File = new Mp3File(filePath);
            if (!mp3File.hasId3v2Tag()) {
                return placeholder;
            }
            ID3v2 id3v2 = mp3File.getId3v2Tag();
            byte[] imageData = id3v2.getAlbumImage();
            if (imageData == null || imageData.length == 0) {
                return placeholder;
            }
            Image image = new Image(new ByteArrayInputStream(imageData), 300, 300, true, true);
            return image.isError() ? placeholder : image;
        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            return placeholder;
        }
    }

    private Image createPlaceholder() {
        WritableImage image = new WritableImage(20, 20);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                boolean dark = ((x / 5) + (y / 5)) % 2 == 0;
                image.getPixelWriter().setColor(x, y, dark ? Color.web("#2a2a2a") : Color.web("#3b3b3b"));
            }
        }
        return image;
    }
}
