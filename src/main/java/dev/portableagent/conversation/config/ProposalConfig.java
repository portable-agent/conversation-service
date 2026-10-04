package dev.portableagent.conversation.config;

import dev.portableagent.conversation.service.ProposalHandler;
import dev.portableagent.conversation.util.MapTools;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ProposalConfig {

    @Bean
    Map<String, ProposalHandler> proposalHandlers(List<ProposalHandler> handlers) {
        return MapTools.byKey(handlers, ProposalHandler::connector);
    }
}
