package dev.portableagent.conversation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConversationCleanupJob {

    private final MessageService messageService;

    public ConversationCleanupJob(MessageService messageService) {
        this.messageService = messageService;
    }

    @Scheduled(fixedDelayString = "${conversation.cleanup-delay:PT5M}")
    public void closeExpired() {
        messageService.closeExpired();
    }
}
