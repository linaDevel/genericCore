package ru.linachan.common.utils;

import java.io.File;
import java.io.IOException;

public class Utils {

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    public static File createTempDirectory(String postfix) throws IOException {
        final File temp;

        temp = File.createTempFile("ctf", postfix);

        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if(!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }

    public static Long tryParseLong(String value, Long failOver) {
        try { return Long.parseLong(value); } catch (Exception ignored) {}

        return failOver;
    }

    public static Integer tryParseInt(String value, Integer failOver) {
        try { return Integer.parseInt(value); } catch (Exception ignored) {}

        return failOver;
    }
}