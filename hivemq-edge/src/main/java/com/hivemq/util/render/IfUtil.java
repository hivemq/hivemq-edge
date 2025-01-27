package com.hivemq.util.render;

import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * This is a temporary solution until we can get a proper template engine or a XML-parser based approach in place.
 */
public class IfUtil {
    private static final Logger log = LoggerFactory.getLogger(EnvVarUtil.class);

    private static final @NotNull String FRAGMENT_VAR_PATTERN = "\\$\\{IF:(.*?)}";

    public static final @NotNull List<String> SUPPORTED_ENVS = List.of(
            "HIVEMQ_HTTPS_ENABLED",
            "HIVEMQ_MQTTS_ENABLED",
            "HIVEMQ_MQTTS_CLIENTAUTH_ENABLED");

    /**
     * Get a Java system property or system environment variable with the specified name.
     * If a variable with the same name exists in both targets the Java system property is returned.
     *
     * @param name the name of the environment variable
     * @return the value of the environment variable with the specified name
     */
    public static @Nullable String getValue(final @NotNull String name) {
        //also check java properties if system variable is not found
        final String systemProperty = System.getProperty(name);
        if (systemProperty != null) {
            return systemProperty;
        }

        return System.getenv(name);
    }

    /**
     * Replaces if markers like '${IF:HIVEMQ_HTTPS_ENABLED}' depending on the existence of an env variable.
     *
     * @param text the text which contains placeholders (or not)
     * @return replacement result containing the actual string
     * @throws UnrecoverableException
     */
    public static @NotNull String replaceIfPlaceHolders(final @NotNull String text) {

        final List<String> activations = SUPPORTED_ENVS.stream()
                .map(envName -> Boolean.parseBoolean(getValue(envName)) ? envName : "!" + envName)
                .collect(Collectors.toList());

        final Matcher matcher = Pattern.compile(FRAGMENT_VAR_PATTERN)
                .matcher(text);

        final Stack<IfToken> matchers = new Stack<>();


        while (matcher.find()) {
            if (matcher.groupCount() < 1) {
                //this should never happen as we declared 1 groups in the ENV_VAR_PATTERN
                log.warn("Found unexpected if placeholder in config.xml");
                continue;
            }
            matchers.push(new IfToken(matcher.group(1), matcher.start(), matcher.end()));
        }

        //going through in reverse order to not mess up the positions in the string
        String result = text;
        while(!matchers.empty()) {
            final IfToken closingMatch = matchers.pop();
            if (activations.contains(closingMatch.content)) {
                //this is an active block, just remove the placeholders
                result = result.substring(0, closingMatch.getStart()) + result.substring(closingMatch.getStop());
                final IfToken openingMatch = matchers.pop();
                //also remove the opening placeholder
                result = (result.substring(0, openingMatch.getStart())
                        + result.substring(openingMatch.getStop()));
            } else {
                //this is an inactive block, remove the whole block, including placeholders
                final IfToken openingMatch = matchers.pop();
                result = result.substring(0, openingMatch.getStart()) + result.substring(closingMatch.getStop());
            }
        }

        return result;
    }

    private static class IfToken{
        private final String content;
        private final int start;
        private final int stop;

        public IfToken(final String content, final int start, final int stop) {
            this.content = content;
            this.start = start;
            this.stop = stop;
        }

        public String getContent() {
            return content;
        }

        public int getStart() {
            return start;
        }

        public int getStop() {
            return stop;
        }

        @Override
        public String toString() {
            return "IfToken{" + "content='" + content + '\'' + ", start=" + start + ", stop=" + stop + '}';
        }
    }
}
