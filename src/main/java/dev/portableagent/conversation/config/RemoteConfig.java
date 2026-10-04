package dev.portableagent.conversation.config;

import dev.portableagent.conversation.agent.api.model.UserContext;
import dev.portableagent.conversation.client.ActionClient;
import dev.portableagent.conversation.client.AgentClient;
import dev.portableagent.conversation.client.ConnectionClient;
import dev.portableagent.conversation.client.RestActionClient;
import dev.portableagent.conversation.client.RestAgentClient;
import dev.portableagent.conversation.client.RestConnectionClient;
import java.net.http.HttpClient;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RemoteProperties.class)
public class RemoteConfig {

    @Bean
    AgentClient agentClient(RemoteProperties properties) {
        var connectors = properties.availableConnectors().stream()
                .map(UserContext.AvailableConnectorsEnum::fromValue)
                .collect(Collectors.toUnmodifiableSet());
        return new RestAgentClient(client(properties, properties.agentUrl()), connectors);
    }

    @Bean
    ActionClient actionClient(RemoteProperties properties) {
        return new RestActionClient(client(properties, properties.actionUrl()));
    }

    @Bean
    ConnectionClient connectionClient(RemoteProperties properties) {
        return new RestConnectionClient(client(properties, properties.connectionUrl()));
    }

    private RestClient client(RemoteProperties properties, java.net.URI url) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(url.toString())
                .requestFactory(requestFactory)
                .build();
    }
}
