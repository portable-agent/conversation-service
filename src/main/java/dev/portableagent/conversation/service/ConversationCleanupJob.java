package dev.portableagent.conversation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationCleanupJob {

    private final MessageService messageService;

    @Scheduled(fixedDelayString = "${conversation.cleanup-delay:PT5M}")
    public void closeExpired() {
        messageService.closeExpired();
    }
}
