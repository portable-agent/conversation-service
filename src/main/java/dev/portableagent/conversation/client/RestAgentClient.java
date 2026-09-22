package dev.portableagent.conversation.client;

import dev.portableagent.conversation.agent.api.model.ProposalRequest;
import dev.portableagent.conversation.agent.api.model.ProposalResponse;
import dev.portableagent.conversation.agent.api.model.UserContext;
import dev.portableagent.conversation.model.Message;
import java.util.Set;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestAgentClient implements AgentClient {

    private final RestClient client;

    public RestAgentClient(RestClient client) {
        this.client = client;
    }

    @Override
    public AgentReply ask(Message message, String accessToken) {
        try {
            var context = new UserContext()
                    .locale(message.locale())
                    .timeZone(message.timeZone())
                    .availableConnectors(Set.of(UserContext.AvailableConnectorsEnum.FAKE_CALENDAR));
            var response = client.post()
                    .uri("/api/v1/proposals")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(new ProposalRequest(message.text(), context))
                    .retrieve()
                    .body(ProposalResponse.class);
            return map(response);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new AgentUnavailable();
        }
    }

    private AgentReply map(ProposalResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Agent response is empty");
        }
        if (response.getClarification() != null && response.getProposal() == null) {
            return AgentReply.question(response.getClarification().getQuestion());
        }
        var plan = response.getProposal();
        if (plan == null || response.getClarification() != null || !Boolean.TRUE.equals(plan.getRequiresApproval())) {
            throw new IllegalArgumentException("Agent response is invalid");
        }
        if (plan.getKind() == null || plan.getConnector() == null || plan.getPayload() == null) {
            throw new IllegalArgumentException("Agent proposal is incomplete");
        }
        return AgentReply.proposal(new Proposal(
                plan.getKind().getValue(),
                plan.getConnector().getValue(),
                CalendarPayloads.fromAgent(plan.getPayload())));
    }
}
