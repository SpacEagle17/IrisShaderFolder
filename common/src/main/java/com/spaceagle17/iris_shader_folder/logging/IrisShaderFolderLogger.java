package com.spaceagle17.iris_shader_folder.logging;

import com.spaceagle17.iris_shader_folder.IrisShaderFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IrisShaderFolderLogger {
    private static Logger logger;
    private static boolean log4jAvailable = true;

    private static final int SPAM_PROTECTION_QUEUE_SIZE = 100;
    private final LogMessageQueue logMessageQueue = new LogMessageQueue(SPAM_PROTECTION_QUEUE_SIZE);

    static {
        try {
            logger = LogManager.getLogger(IrisShaderFolder.MOD_ID);
        } catch (NoClassDefFoundError | Exception e) {
            log4jAvailable = false;
            System.out.println("[IrisShaderFolder] Log4j not available, using System.out fallback");
        }
    }

    public void log(int messageLevel, String message) {
        if (shouldSuppressMessage(message)) return;
        String loggingMessage = "IrisShaderFolder: " + message;
        if (messageLevel == -1) loggingMessage = "\n \n" + loggingMessage + "\n\n ";

        switch (messageLevel) {
            case 0:
            case 1:
                if (log4jAvailable && logger != null) logger.info(loggingMessage);
                else System.out.println("[INFO] " + loggingMessage);
                break;
            case 2:
                if (log4jAvailable && logger != null) logger.warn(loggingMessage);
                else System.out.println("[WARN] " + loggingMessage);
                break;
            case 3:
                if (log4jAvailable && logger != null) logger.error(loggingMessage);
                else System.err.println("[ERROR] " + loggingMessage);
                break;
            default:
                System.out.println(loggingMessage);
                break;
        }
    }

    private boolean shouldSuppressMessage(String message) {
        LogMessage logMessage = new LogMessage(message);
        int count = logMessageQueue.getOccurrenceCount(logMessage);
        if (count == -1) {
            logMessageQueue.add(logMessage);
            return false;
        }
        return count > 3;
    }

    public static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
