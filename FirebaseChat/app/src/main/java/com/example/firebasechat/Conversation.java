package com.example.firebasechat;

/**
 * Created by Shyam Panchal.
 */
public class Conversation {

    private boolean seen;
    private long timestamp;

    public Conversation() {}

    public Conversation(String seen, long timestamp) {
        this.seen = Boolean.parseBoolean(seen);
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
