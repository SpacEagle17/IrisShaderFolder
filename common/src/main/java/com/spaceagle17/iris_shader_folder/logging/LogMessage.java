package com.spaceagle17.iris_shader_folder.logging;

public class LogMessage {
    private final String message;
    private long timestamp;
    private int occurrenceCount;

    public LogMessage(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.occurrenceCount = 1;
    }

    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public int getOccurrenceCount() { return occurrenceCount; }

    public void incrementOccurrenceCount() { occurrenceCount++; }
    public void refreshTimestamp() { timestamp = System.currentTimeMillis(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return message.equals(((LogMessage) o).message);
    }

    @Override
    public int hashCode() { return message.hashCode(); }
}
