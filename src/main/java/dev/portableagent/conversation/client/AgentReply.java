package dev.portableagent.conversation.client;

public record AgentReply(Proposal proposal, String question) {

    public AgentReply {
        boolean hasProposal = proposal != null;
        boolean hasQuestion = question != null && !question.isBlank();
        if (hasProposal == hasQuestion) {
            throw new IllegalArgumentException("Agent reply must contain one proposal or one question");
        }
    }

    public static AgentReply proposal(Proposal proposal) {
        return new AgentReply(proposal, null);
    }

    public static AgentReply question(String question) {
        return new AgentReply(null, question);
    }

    public boolean needsDetails() {
        return question != null;
    }

    @Override
    public String toString() {
        return "AgentReply[proposal=[REDACTED], question=[REDACTED]]";
    }
}
