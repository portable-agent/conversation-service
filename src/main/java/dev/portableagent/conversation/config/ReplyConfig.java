package dev.portableagent.conversation.config;

import dev.portableagent.conversation.model.ReplyType;
import dev.portableagent.conversation.service.ReplyViewer;
import dev.portableagent.conversation.util.MapTools;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReplyConfig {

    @Bean
    Map<ReplyType, ReplyViewer> replyViewers(List<ReplyViewer> viewers) {
        return MapTools.byKeys(viewers, ReplyViewer::types);
    }
}
