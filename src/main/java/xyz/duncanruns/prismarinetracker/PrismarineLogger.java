package xyz.duncanruns.prismarinetracker;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class PrismarineLogger {
    private static final Path FOLDER_PATH = PrismarineTracker.TRACKER_DIR.resolve("logs");
    private static final Path CURRENT_PATH = FOLDER_PATH.resolve(System.currentTimeMillis() + ".log");
    private static RandomAccessFile file = null;

    private static final Queue<Pair<Long, String>> QUEUE = new LinkedList<>();

    public static void queueLog(long time, String msg) {
        QUEUE.add(Pair.of(time, msg));
    }

    public static void flushLog() throws IOException {
        if (QUEUE.isEmpty()) return;
        if (file == null) {
            FOLDER_PATH.toFile().mkdirs();
            file = new RandomAccessFile(CURRENT_PATH.toFile(), "rw");
        }
        QUEUE.stream().sorted(Comparator.comparingLong(Pair::getLeft)).forEach(p -> {
            try {
                write(p.getLeft() + " " + p.getRight().trim());
            } catch (IOException ignored) {
            }
        });
        QUEUE.clear();
    }

    private static void write(String msg) throws IOException {
        if (file == null) {
            return;
        }
        file.writeBytes(msg + "\n");
    }
}
