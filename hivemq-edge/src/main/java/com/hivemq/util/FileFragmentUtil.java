package com.hivemq.util;

import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileFragmentUtil {
    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    private static final @NotNull String FRAGMENT_VAR_PATTERN = "\\$\\{FRAGMENT:(.*)}";

    /**
     * Load a file fragment
     *
     * @param path the path where the fragment is located
     * @return return the content of the fragment
     */
    public static @Nullable String getFragment(final @NotNull String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Unable to load fragment from {}", path, e);
        }
        return null;
    }

    /**
     * Replaces fragment markers like '${FRAGMENT:VAR_NAME}' with the according fragment loaded from the filesystem.
     *
     * @param text the text which contains placeholders (or not)
     * @return the text with all the placeholders replaced
     * @throws UnrecoverableException if a fragment used in a placeholder can't be loaded
     */
    public static @NotNull String replaceFragmentPlaceHolders(final @NotNull String text) {

        final StringBuffer resultString = new StringBuffer();

        final Matcher matcher = Pattern.compile(FRAGMENT_VAR_PATTERN)
                .matcher(text);

        while (matcher.find()) {

            if (matcher.groupCount() < 1) {
                //this should never happen as we declared 1 groups in the ENV_VAR_PATTERN
                log.warn("Found unexpected fragment variable placeholder in config.xml");
                matcher.appendReplacement(resultString, "");
                continue;
            }

            final String fragmentPath = matcher.group(1);

            final String replacement = getFragment(fragmentPath);

            if (replacement == null) {
                log.error("Fragment {} for HiveMQ config.xml can't be loaded.", fragmentPath);
                throw new UnrecoverableException(false);
            }

            //sets replacement for this match
            matcher.appendReplacement(resultString, escapeReplacement(replacement));

        }

        //adds everything except the replacements to the string buffer
        matcher.appendTail(resultString);

        return resultString.toString();
    }

    private static @NotNull String escapeReplacement(final @NotNull String replacement) {
        return replacement
                .replace("\\", "\\\\")
                .replace("$", "\\$");
    }
}
