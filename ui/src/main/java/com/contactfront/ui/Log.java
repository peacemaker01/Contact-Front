package com.contactfront.ui;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Log {
    private static final Path LOG_FILE = Path.of("logs", "contactfront.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    static {
        try {
            Files.createDirectories(LOG_FILE.getParent());
        } catch (IOException ignored) {}
    }
    
    private Log() {}
    
    public static void info(String msg) {
        log("INFO", msg);
    }
    
    public static void error(String msg, Throwable t) {
        log("ERROR", msg + (t != null ? " | " + t.getMessage() : ""));
    }
    
    public static void error(String msg) {
        log("ERROR", msg);
    }
    
    public static void warning(String msg) {
        log("WARN", msg);
    }
    
    private static void log(String level, String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] [" + level + "] " + msg;
        System.out.println(line);
        try {
            Files.writeString(LOG_FILE, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}