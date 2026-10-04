package dev.portableagent.conversation.client;

import java.net.URI;

public interface ConnectionClient {

    ConnectionStatus status(String provider, String accessToken);

    URI start(String provider, String accessToken);
}
