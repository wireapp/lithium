package com.wire.lithium.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class Util {
    public static void deleteDir(String dir) throws IOException {
        Path rootPath = Paths.get(dir);
        if (!rootPath.toFile().exists()) return;
        //noinspection ResultOfMethodCallIgnored
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
