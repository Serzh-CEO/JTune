package com.jtune.utils;

import java.io.File;

public final class FileUtils {
    private FileUtils() {
    }

    public static String fileNameWithoutExtension(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }
}
