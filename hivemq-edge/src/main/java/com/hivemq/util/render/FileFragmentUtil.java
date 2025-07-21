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
package com.hivemq.util.render;

import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jose4j.base64url.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileFragmentUtil {
    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    private static final @NotNull String FRAGMENT_VAR_PATTERN = "\\$\\{FRAGMENT:(.*)}";

    /**
     * Replaces fragment markers like '${FRAGMENT:VAR_NAME}' with the according fragment loaded from the filesystem.
     *
     * @param text the text which contains placeholders (or not)
     * @return replacement result containing the actual string and file and their modification time used in the replacement
     * @throws UnrecoverableException if a fragment used in a placeholder can't be loaded
     */
    public static @NotNull FragmentResult replaceFragmentPlaceHolders(final @NotNull String text, final boolean contentIsZippedAndBase64Encoded) {

        final StringBuilder resultString = new StringBuilder();
        final Map<Path, Long> fragmentToModificationTime = new HashMap<>();
        final Matcher matcher = Pattern.compile(FRAGMENT_VAR_PATTERN)
                .matcher(text);

        while (matcher.find()) {

            if (matcher.groupCount() < 1) {
                //this should never happen as we declared 1 groups in the ENV_VAR_PATTERN
                log.warn("Found unexpected fragment variable placeholder in config.xml");
                matcher.appendReplacement(resultString, "");
                continue;
            }
            final String pathString = matcher.group(1);
            try {
                final Path fragmentPath = Path.of(pathString);
                final Long modificationTime = fragmentPath.toRealPath().toFile().lastModified();
                fragmentToModificationTime.put(fragmentPath, modificationTime);

                final String replacement;
                if (contentIsZippedAndBase64Encoded) {
                    log.info("Config fragment {} is expected to be zipped and base64 encoded, extracting content.", pathString);
                    replacement =
                            deflate(Base64.decode(Files.readString(fragmentPath, StandardCharsets.UTF_8)))
                                    .map(deflated -> new String(deflated, StandardCharsets.UTF_8))
                                    .orElseThrow(UnrecoverableException::new);
                } else {
                    replacement = Files.readString(fragmentPath, StandardCharsets.UTF_8);
                }

                //sets replacement for this match
                matcher.appendReplacement(resultString, escapeReplacement(replacement));
            } catch (IOException e) {
                log.error("Fragment {} for HiveMQ config.xml can't be loaded.", pathString, e);
            }
        }

        //adds everything except the replacements to the string buffer
        matcher.appendTail(resultString);

        return new FragmentResult(fragmentToModificationTime, resultString.toString());
    }

    private static @NotNull String escapeReplacement(final @NotNull String replacement) {
        return replacement
                .replace("\\", "\\\\")
                .replace("$", "\\$");
    }

    public static class FragmentResult {
        private final @NotNull Map<Path, Long> fragmentToModificationTime;
        private final @NotNull String renderResult;

        public FragmentResult(
                @NotNull final Map<Path, Long> fragmentToModificationTime,
                @NotNull final String renderResult) {
            this.fragmentToModificationTime = fragmentToModificationTime;
            this.renderResult = renderResult;
        }

        public @NotNull Map<Path, Long> getFragmentToModificationTime() {
            return fragmentToModificationTime;
        }

        public @NotNull String getRenderResult() {
            return renderResult;
        }
    }

    public static Optional<byte[]> deflate(final byte[] zipData) {
        try (final var zipStream = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            if (zipStream.getNextEntry() != null) {
                return Optional.of(zipStream.readAllBytes());
            }
            return Optional.empty();
        } catch (final IOException e) {
            log.error("Error while extracting from ZIP: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
