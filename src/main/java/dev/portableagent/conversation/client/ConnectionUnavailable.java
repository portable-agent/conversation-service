package dev.portableagent.conversation.client;

public class ConnectionUnavailable extends RuntimeException {

    public ConnectionUnavailable() {
        super("Connection Service is unavailable");
    }
}
