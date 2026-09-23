package dev.portableagent.conversation.client;

import dev.portableagent.conversation.action.api.model.ActionResponse;
import dev.portableagent.conversation.action.api.model.ProposeActionRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestActionClient implements ActionClient {

    private final RestClient client;

    public RestActionClient(RestClient client) {
        this.client = client;
    }

    @Override
    public SavedAction create(Proposal proposal, String requestKey, String accessToken) {
        try {
            var request = new ProposeActionRequest(
                    ProposeActionRequest.KindEnum.fromValue(proposal.kind()),
                    ProposeActionRequest.ConnectorEnum.fromValue(proposal.connector()),
                    CalendarPayloads.toAction(proposal.payload()),
                    requestKey);
            var response = client.post()
                    .uri("/api/v1/actions")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(request)
                    .retrieve()
                    .body(ActionResponse.class);
            if (response == null
                    || response.getId() == null
                    || response.getPayloadHash() == null
                    || response.getPayload() == null) {
                throw new IllegalArgumentException("Action response is empty");
            }
            return new SavedAction(
                    response.getId(), response.getPayloadHash(), CalendarPayloads.fromAction(response.getPayload()));
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ActionUnavailable();
        }
    }
}
