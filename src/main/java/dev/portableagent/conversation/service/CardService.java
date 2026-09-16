package dev.portableagent.conversation.service;

import dev.portableagent.conversation.client.Proposal;
import dev.portableagent.conversation.client.SavedAction;
import dev.portableagent.conversation.model.SavedReply;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CardService {

    private final Map<String, CardMaker> makers;

    public CardService(List<CardMaker> makers) {
        var byKind = new LinkedHashMap<String, CardMaker>();
        for (var maker : List.copyOf(makers)) {
            if (byKind.putIfAbsent(maker.kind(), maker) != null) {
                throw new IllegalArgumentException("Several card makers support " + maker.kind());
            }
        }
        this.makers = Map.copyOf(byKind);
    }

    public SavedReply make(Proposal proposal, SavedAction action) {
        Objects.requireNonNull(proposal, "proposal must not be null");
        Objects.requireNonNull(action, "action must not be null");
        var maker = makers.get(proposal.kind());
        if (maker == null) {
            throw new IllegalArgumentException("Card maker is not configured for " + proposal.kind());
        }
        return maker.make(action);
    }
}
