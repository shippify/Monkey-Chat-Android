package com.criptext.database;

/**
 * Created by gesuwall on 2/5/16.
 */
public class PendingMessage {
    private String id, message;

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public PendingMessage(String id, String message){
        this.id = id;
        this.message = message;
    }
}
