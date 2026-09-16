package dev.portableagent.conversation.client;

public class ActionUnavailable extends RuntimeException {

    public ActionUnavailable() {
        super("Action service is unavailable");
    }
}
