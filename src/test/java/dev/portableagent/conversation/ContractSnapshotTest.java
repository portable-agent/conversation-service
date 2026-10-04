package dev.portableagent.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContractSnapshotTest {

    @Test
    void contractSnapshot_hasExpectedVersionAndMessagePath() throws IOException {
        var contract = Files.readString(Path.of("src/main/openapi/conversation-api.yaml"));

        assertThat(contract).contains("version: 4.0.0");
        assertThat(contract).contains("/api/v1/messages:");
        assertThat(contract).contains("operationId: createConversationMessage");
    }

    @Test
    void buildFile_hasJooqAndNoJpa() throws IOException {
        var buildFile = Files.readString(Path.of("build.gradle.kts"));

        assertThat(buildFile).contains("spring-boot-starter-jooq");
        assertThat(buildFile).doesNotContain("spring-boot-starter-data-jpa");
        assertThat(buildFile).doesNotContain("hibernate");
    }

    @Test
    void clientSnapshots_shouldStayPinnedAndKeepApprovalRule() throws IOException {
        var agent = Files.readString(Path.of("src/main/openapi/clients/agent-runtime-api.yaml"));
        var action = Files.readString(Path.of("src/main/openapi/clients/action-api.yaml"));
        var connection = Files.readString(Path.of("src/main/openapi/clients/connection-api.yaml"));

        assertThat(agent).contains("version: 4.0.0", "/api/v1/proposals:", "const: true", "google-calendar");
        assertThat(action).contains("version: 4.0.0", "/api/v1/actions:", "google-calendar");
        assertThat(connection)
                .contains("version: 4.0.0", "/api/v1/connections:", "/api/v1/connections/start:", "google-calendar");
    }
}
