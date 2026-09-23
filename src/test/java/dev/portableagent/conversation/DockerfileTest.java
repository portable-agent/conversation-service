package dev.portableagent.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DockerfileTest {

    @Test
    void build_shouldReuseGradleCache() throws IOException {
        var dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile)
                .contains("# syntax=docker/dockerfile:1.7")
                .contains("RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon")
                .contains("RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon");
    }
}
