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
package com.hivemq.edge.knappogue;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.edge.compiler.CompilerMain;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles the {@code edge --compile [--push ...]} seam in {@link com.hivemq.HiveMQEdgeMain}.
 *
 * <p>The compiler itself ({@code hivemq-edge-compiler}) remains lightweight and MQTT-free. Push
 * logic lives here, in the Edge runtime, which already has {@code hivemq-mqtt-client} on the
 * classpath.
 *
 * <p>Usage (forwarded from {@code edge --compile}):
 * <pre>
 *   edge --compile -p /path/to/project --instance wolery              # compile only
 *   edge --compile -p /path/to/project --instance wolery --push       # compile + push to localhost:1883
 *   edge --compile -p /path/to/project --instance wolery --push \
 *        --fleet acme --host 192.168.1.10 --port 1883
 * </pre>
 *
 * <p>Push-specific flags ({@code --push}, {@code --fleet}, {@code --host}, {@code --port},
 * {@code --topic}) are stripped from the argument list before forwarding to the compiler CLI.
 * {@code --instance} is a compiler flag and is passed through.
 *
 * <p>Topic resolution follows {@link KnappogueTopic}: {@code --topic} overrides the pattern,
 * then {@code HIVEMQ_COMPILED_CONFIG_TOPIC} env var, then the default
 * {@value KnappogueTopic#DEFAULT_PATTERN}. {@code {fleetId}} and {@code {edgeInstanceId}}
 * placeholders are replaced in whichever pattern is used.
 */
public class EdgeCompileCommand {

    static final @NotNull String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 1883;
    static final @NotNull String DEFAULT_TOPIC = KnappogueTopic.DEFAULT_PATTERN;

    /**
     * Runs the compile-and-optionally-push command.
     *
     * @param args the arguments after {@code --compile} (i.e. {@code args[1..]} from main)
     * @return exit code: 0 = success, 1 = compile error, 2 = I/O or push error
     */
    public static int run(final @NotNull String[] args) {
        final String instanceId = extractInstanceId(args);
        final PushOptions pushOptions = PushOptions.parse(args, instanceId);
        final List<String> strippedArgs = pushOptions.strippedArgs();

        final int compileExit = CompilerMain.run(strippedArgs.toArray(new String[0]));
        if (compileExit != 0) {
            return compileExit;
        }

        if (!pushOptions.push()) {
            return 0;
        }

        // Locate the compiled artifact written by the compiler.
        // The compiler writes to <project>/build/<instanceId>/compiled-config.json by default,
        // or to the path given by --output / -o.
        final File compiledFile = resolveCompiledFile(strippedArgs, instanceId);
        if (compiledFile == null) {
            System.err.println("ERROR: Could not determine compiled config path for push.");
            return 2;
        }

        final CompiledConfig compiledConfig;
        try {
            compiledConfig = new CompiledConfigSerializer().fromJson(compiledFile);
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to read compiled config for push: " + e.getMessage());
            return 2;
        }

        return push(compiledConfig, pushOptions);
    }

    private static int push(final @NotNull CompiledConfig compiledConfig, final @NotNull PushOptions opts) {
        final byte[] payload;
        try {
            payload = new CompiledConfigSerializer().toJson(compiledConfig).getBytes(StandardCharsets.UTF_8);
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to serialize compiled config for push: " + e.getMessage());
            return 2;
        }

        final String clientId =
                "edge-compile-push-" + UUID.randomUUID().toString().substring(0, 8);
        System.err.println("Pushing to " + opts.host() + ":" + opts.port() + " → " + opts.topic());

        final Mqtt5BlockingClient client = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(opts.host())
                .serverPort(opts.port())
                .identifier(clientId)
                .buildBlocking();
        try {
            client.connect();
            client.publishWith()
                    .topic(opts.topic())
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .payload(payload)
                    .send();
            System.err.println("Pushed successfully.");
            return 0;
        } catch (final Exception e) {
            System.err.println("ERROR: Failed to push to Edge: " + e.getMessage());
            return 2;
        } finally {
            try {
                client.disconnect();
            } catch (final Exception e) {
                System.err.println("WARN: Failed to disconnect cleanly: " + e.getMessage());
            }
        }
    }

    /**
     * Re-derives the path of the compiled config file from the stripped compiler args.
     * Mirrors the logic in {@code CompileCommand}: {@code --output} / {@code -o} if present,
     * otherwise {@code <project>/build/<instanceId>/compiled-config.json}.
     */
    private static @Nullable File resolveCompiledFile(
            final @NotNull List<String> compilerArgs, final @Nullable String instanceId) {
        // --output / -o override
        for (int i = 0; i < compilerArgs.size() - 1; i++) {
            final String arg = compilerArgs.get(i);
            if ("--output".equals(arg) || "-o".equals(arg)) {
                return new File(compilerArgs.get(i + 1));
            }
        }

        // default: <project>/build/<instanceId>/compiled-config.json
        String projectDir = ".";
        for (int i = 0; i < compilerArgs.size() - 1; i++) {
            final String arg = compilerArgs.get(i);
            if ("--project".equals(arg) || "-p".equals(arg)) {
                projectDir = compilerArgs.get(i + 1);
                break;
            }
        }

        final Path projectRoot = Path.of(projectDir).toAbsolutePath().normalize();
        String resolvedInstanceId = instanceId;
        if (resolvedInstanceId == null) {
            resolvedInstanceId = detectSingleInstance(projectRoot);
        }
        if (resolvedInstanceId == null) {
            return null;
        }
        return projectRoot
                .resolve("build")
                .resolve(resolvedInstanceId)
                .resolve("compiled-config.json")
                .toFile();
    }

    /** Returns the single instance directory name if exactly one exists under {@code instances/}, else {@code null}. */
    static @Nullable String detectSingleInstance(final @NotNull Path projectRoot) {
        final Path instancesDir = projectRoot.resolve("instances");
        if (!Files.isDirectory(instancesDir)) {
            return null;
        }
        try (final var dirs = Files.list(instancesDir)) {
            final List<String> instances = dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .toList();
            return instances.size() == 1 ? instances.get(0) : null;
        } catch (final IOException e) {
            return null;
        }
    }

    /** Extracts the value of {@code --instance} / {@code -i} from the raw argument array, or {@code null} if absent. */
    private static @Nullable String extractInstanceId(final @NotNull String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--instance".equals(args[i]) || "-i".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    // ── Inner types ──────────────────────────────────────────────────────────

    record PushOptions(
            boolean push,
            @NotNull String host,
            int port,
            @NotNull String topic,
            @NotNull List<String> strippedArgs) {

        /**
         * Parses push-specific flags out of {@code args} and returns a {@link PushOptions}.
         *
         * <p>Flags consumed (stripped from the remaining args forwarded to the compiler):
         * {@code --push}, {@code --fleet <id>}, {@code --host <h>}, {@code --port <p>},
         * {@code --topic <pattern>}.
         *
         * <p>Topic resolution follows {@link KnappogueTopic}: {@code --topic} → env override →
         * default pattern. {@code {fleetId}} and {@code {edgeInstanceId}} are replaced in whichever
         * pattern is chosen.
         *
         * @param instanceId  value of {@code --instance} / {@code -i}, or {@code null} if not given
         */
        static @NotNull PushOptions parse(final @NotNull String[] args, final @Nullable String instanceId) {
            boolean push = false;
            String host = DEFAULT_HOST;
            int port = DEFAULT_PORT;
            String fleet = KnappogueTopic.envOr(KnappogueTopic.FLEET_ID_ENV_VAR, "-");
            String cliTopicOverride = null;
            final List<String> remaining = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--push" -> push = true;
                    case "--fleet" -> {
                        if (i + 1 < args.length) {
                            fleet = args[++i];
                        }
                    }
                    case "--host" -> {
                        if (i + 1 < args.length) {
                            host = args[++i];
                        }
                    }
                    case "--port" -> {
                        if (i + 1 < args.length) {
                            try {
                                port = Integer.parseInt(args[++i]);
                            } catch (final NumberFormatException e) {
                                System.err.println(
                                        "WARN: Invalid --port value '" + args[i] + "' — using " + DEFAULT_PORT);
                                port = DEFAULT_PORT;
                            }
                        }
                    }
                    case "--topic" -> {
                        if (i + 1 < args.length) {
                            cliTopicOverride = args[++i];
                        }
                    }
                    default -> remaining.add(args[i]);
                }
            }

            final String resolvedInstanceId = instanceId != null ? instanceId : "-";
            final String topic = KnappogueTopic.resolveForPush(fleet, resolvedInstanceId, cliTopicOverride);
            return new PushOptions(push, host, port, topic, List.copyOf(remaining));
        }
    }
}
