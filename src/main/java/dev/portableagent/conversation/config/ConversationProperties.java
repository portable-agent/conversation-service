package dev.portableagent.conversation.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversation")
public record ConversationProperties(Duration openTtl, int cleanupBatchSize) {

    public ConversationProperties {
        if (openTtl == null || openTtl.isZero() || openTtl.isNegative()) {
            throw new IllegalArgumentException("conversation.open-ttl must be positive");
        }
        if (cleanupBatchSize < 1) {
            throw new IllegalArgumentException("conversation.cleanup-batch-size must be positive");
        }
    }
}
