package com.kc.encrypted.efm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IoUtils {
    private IoUtils() {}

    public static void ensureDir(Path p) throws IOException {
        Files.createDirectories(p);
    }
}