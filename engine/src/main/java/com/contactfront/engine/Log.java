package com.contactfront.engine;

public final class Log {
    private Log() {}
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Log.class.getName());
    
    public static void info(String msg) {
        logger.info(msg);
    }
    
    public static void error(String msg) {
        logger.severe(msg);
    }
    
    public static void warning(String msg) {
        logger.warning(msg);
    }
}