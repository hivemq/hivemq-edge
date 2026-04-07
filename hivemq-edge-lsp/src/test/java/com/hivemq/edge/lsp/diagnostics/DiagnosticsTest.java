/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.lsp.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link DiagnosticsPublisher} with a real temp filesystem and a recording
 * {@link LanguageClient}.
 *
 * <p>Uses the package-private {@link DiagnosticsPublisher#validateNow(Path)} method to run
 * validation synchronously without the 300 ms debounce.
 */
class DiagnosticsTest {

    @TempDir
    Path tempDir;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void validAdapterProducesNoDiagnostics() throws Exception {
        final Path adapterDir = createAdapter("pump-01", "opcua");
        writeFile(
                adapterDir.resolve("tags.yaml"),
                "tags:\n  - name: Pressure\n    deviceTag:\n      id: 'ns=2;i=1001'\n      dataType: Float\n");

        final RecordingClient client = new RecordingClient();
        final DiagnosticsPublisher publisher = new DiagnosticsPublisher(new WorkspaceIndex(), tempDir);
        publisher.connect(client);
        publisher.validateNow(adapterDir.resolve("adapter.yaml"));

        // No errors — publishDiagnostics should not have been called (or called with an empty list)
        final boolean hasErrors =
                client.published.stream().anyMatch(p -> !p.getDiagnostics().isEmpty());
        assertThat(hasErrors).isFalse();
    }

    // ── Error case ────────────────────────────────────────────────────────────

    @Test
    void danglingDeviceTagIdProducesDiagnostic() throws Exception {
        final Path adapterDir = createAdapter("pump-01", "opcua");
        // tags.yaml references a deviceTagId that is never declared in a deviceTags file
        writeFile(adapterDir.resolve("tags.yaml"), "tags:\n  - name: Pressure\n    deviceTagId: 'ns=2;i=9999'\n");

        final RecordingClient client = new RecordingClient();
        final DiagnosticsPublisher publisher = new DiagnosticsPublisher(new WorkspaceIndex(), tempDir);
        publisher.connect(client);
        publisher.validateNow(adapterDir.resolve("tags.yaml"));

        assertThat(client.published).isNotEmpty();
        final boolean hasErrors =
                client.published.stream().anyMatch(p -> !p.getDiagnostics().isEmpty());
        assertThat(hasErrors).isTrue();
    }

    // ── Clear on fix ─────────────────────────────────────────────────────────

    @Test
    void diagnosticsAreClearedAfterFixingError() throws Exception {
        final Path adapterDir = createAdapter("pump-01", "opcua");
        final Path tagsFile = adapterDir.resolve("tags.yaml");

        // First write — causes UNRESOLVED_DEVICE_TAG_REFERENCE
        writeFile(tagsFile, "tags:\n  - name: Pressure\n    deviceTagId: 'ns=2;i=9999'\n");

        final RecordingClient client = new RecordingClient();
        final DiagnosticsPublisher publisher = new DiagnosticsPublisher(new WorkspaceIndex(), tempDir);
        publisher.connect(client);
        publisher.validateNow(tagsFile);

        assertThat(client.published.stream().anyMatch(p -> !p.getDiagnostics().isEmpty()))
                .as("should have errors after first validation")
                .isTrue();

        client.published.clear();

        // Fix the error — use inline device tag instead
        writeFile(
                tagsFile,
                "tags:\n  - name: Pressure\n    deviceTag:\n      id: 'ns=2;i=1001'\n      dataType: Float\n");
        publisher.validateNow(tagsFile);

        // The previously-errored URI should now be published with an empty list (cleared)
        final boolean allCleared =
                client.published.stream().allMatch(p -> p.getDiagnostics().isEmpty());
        assertThat(allCleared).as("diagnostics should be cleared after fix").isTrue();
    }

    // ── File outside instances/ ───────────────────────────────────────────────

    @Test
    void fileOutsideInstancesProducesNoDiagnostics() throws Exception {
        final Path outsideFile = tempDir.resolve("some-other.yaml");
        writeFile(outsideFile, "key: value\n");

        final RecordingClient client = new RecordingClient();
        final DiagnosticsPublisher publisher = new DiagnosticsPublisher(new WorkspaceIndex(), tempDir);
        publisher.connect(client);
        publisher.validateNow(outsideFile);

        assertThat(client.published).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates {@code instances/<adapterId>/adapters/<adapterId>/adapter.yaml} under {@code tempDir}.
     * Returns the adapter directory path.
     */
    private Path createAdapter(final String adapterId, final String adapterType) throws Exception {
        final Path adapterDir = tempDir.resolve("instances")
                .resolve("test-instance")
                .resolve("adapters")
                .resolve(adapterId);
        Files.createDirectories(adapterDir);
        writeFile(
                adapterDir.resolve("adapter.yaml"),
                "type: " + adapterType + "\nid: " + adapterId + "\nconnection:\n  host: 192.168.1.1\n  port: 4840\n");
        return adapterDir;
    }

    private static void writeFile(final Path path, final String content) throws Exception {
        Files.writeString(path, content);
    }

    // ── Recording LanguageClient ───────────────────────────────────────────────

    /**
     * Minimal {@link LanguageClient} that records all {@code publishDiagnostics} calls.
     */
    private static final class RecordingClient implements LanguageClient {

        final List<PublishDiagnosticsParams> published = new ArrayList<>();

        @Override
        public void publishDiagnostics(final PublishDiagnosticsParams diagnostics) {
            published.add(diagnostics);
        }

        @Override
        public void telemetryEvent(final Object object) {}

        @Override
        public void logMessage(final MessageParams message) {}

        @Override
        public void showMessage(final MessageParams messageParams) {}

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(final ShowMessageRequestParams requestParams) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(final ApplyWorkspaceEditParams params) {
            return CompletableFuture.completedFuture(new ApplyWorkspaceEditResponse(false));
        }

        @Override
        public CompletableFuture<Void> registerCapability(final RegistrationParams params) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> unregisterCapability(final UnregistrationParams params) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<List<Object>> configuration(final ConfigurationParams configurationParams) {
            return CompletableFuture.completedFuture(List.of());
        }
    }
}
