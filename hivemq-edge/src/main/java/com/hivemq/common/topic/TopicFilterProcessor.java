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
package com.hivemq.common.topic;

import com.hivemq.client.annotations.Immutable;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hivemq.bridge.mqtt.RemoteMqttForwarder.DEFAULT_DESTINATION_PATTERN;

/**
 * Generates the destination topic for in and outgoing publishes.
 */
public class TopicFilterProcessor {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TopicFilterProcessor.class);

    private static final @NotNull Pattern REGEX_ENV_VAR = Pattern.compile(".*\\$ENV\\{.*}.*");
    private static final @NotNull Pattern REGEX_OUT_TOPIC = Pattern.compile(".*\\\\\\{.*}.*");
    private static final @NotNull String REGEX_OUT_TOPIC_REPLACER = "\\\\\\{";
    private static final @NotNull Pattern REGEX_ARBITRARY_TOKENS = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final @NotNull Pattern REGEX_VAR = Pattern.compile(".*\\{.*}.*");
    private static final @NotNull Pattern REGEX_NUM = Pattern.compile("[0-9]+");


    public static @NotNull MqttTopic modifyTopic(
            final @Nullable String destination,
            final @NotNull MqttTopic topic,
            final @NotNull Map<String, String> tokensAndValues) {

        if (destination == null || destination.equals(DEFAULT_DESTINATION_PATTERN)) {
            return topic;
        }

        return TopicFilterProcessor.applyDestinationModifier(topic, destination, tokensAndValues);
    }


    /**
     * Applies all the destination modifiers.getQosForPubMode
     *
     * @param inTopic             The topic the message was captured from.
     * @param destinationModifier The destination modifier for the specific topic filter.
     * @return The destination topic for the message.
     */
    public static @NotNull MqttTopic applyDestinationModifier(
            final @NotNull MqttTopic inTopic,
            final @NotNull String destinationModifier,
            final @NotNull Map<String, String> tokensAndValues) {
        if (destinationModifier.isEmpty()) {
            return inTopic;
        }
        //We split the destinationModifier in order to retrieve the topics levels. The split limit is set to
        //-1 in order to also include a trailing (empty) /-level as the split will add a trailing empty string.
        final List<String> outTopicWithPlaceholders = List.of(destinationModifier.split("/", -1));
        final List<String> outTopic = new ArrayList<>();

        for (String level : outTopicWithPlaceholders) {

            //-- Parse the intial topicLevel for arbitrary matches before the deferred replacement logic
            if (!tokensAndValues.isEmpty()) {
                level = replaceTokens(level, tokensAndValues);
            }

            if (REGEX_ENV_VAR.matcher(level).find()) {
                replaceEnvVar(level, outTopic);
            } else if (REGEX_OUT_TOPIC.matcher(level).find()) {
                outTopic.add(level.replaceAll(REGEX_OUT_TOPIC_REPLACER, "{"));

            } else if (REGEX_VAR.matcher(level).find()) {
                final int replacementInformationStart = level.indexOf('{') + "{".length();
                final int replacementInformationEnd = level.indexOf('}', replacementInformationStart);
                final String levelInsert = level.substring(replacementInformationStart, replacementInformationEnd);

                if (levelInsert.contains("#") && levelInsert.length() == "#".length()) {
                    replaceMultiWildcard(level,
                            outTopic,
                            inTopic,
                            replacementInformationStart,
                            replacementInformationEnd);

                } else if (levelInsert.contains("-#")) {
                    replaceMultiWildcardWithStart(level,
                            outTopic,
                            inTopic,
                            levelInsert,
                            replacementInformationStart,
                            replacementInformationEnd);

                } else if (levelInsert.contains("-")) {
                    replaceMultilevel(level,
                            outTopic,
                            inTopic,
                            levelInsert,
                            replacementInformationStart,
                            replacementInformationEnd);

                } else if (REGEX_NUM.matcher(levelInsert).find()) {
                    replaceSingleLevel(level, outTopic, inTopic, levelInsert);

                } else {
                    log.warn("Topic Filter Processor: Found definition {} that does not exist!", levelInsert);
                }

            } else {
                outTopic.add(level);
            }
        }
        return MqttTopic.of(String.join("/", outTopic));
    }

    private static void replaceEnvVar(final @NotNull String level, final @NotNull List<String> outTopic) {
        final int envIdStart = level.indexOf("$ENV{") + "$ENV{".length();
        final int envIdEnd = level.indexOf('}', envIdStart);
        final String envVar = level.substring(envIdStart, envIdEnd);
        final String envValue = System.getenv(envVar) != null ? System.getenv(envVar) : System.getProperty(envVar);
        if (envValue != null) {
            outTopic.add(level.replace("$ENV{" + envVar + "}", envValue));
        } else {
            log.warn("Topic-Modifier: Found environment variable definition {} but no Value was set! Ignoring.",
                    envVar);
            final String emptyReplace = level.replace("$ENV{" + envVar + "}", "");
            if (!emptyReplace.isEmpty()) {
                outTopic.add(emptyReplace);
            }
        }
    }

    private static void replaceMultiWildcard(
            final @NotNull String level,
            final @NotNull List<String> outTopic,
            final @NotNull MqttTopic inTopic,
            final int replacementInformationStart,
            final int replacementInformationEnd) {
        outTopic.add(inTopic.toString());
        if (replacementInformationStart != "{".length()) {
            log.warn(
                    "Topic-Replacer: Leading content \"{}\" in MultiLevelWildcard replacement is not allowed! Ignoring.",
                    level.substring(0, replacementInformationStart - 1));
        }
        if (replacementInformationEnd != level.length() - "{".length()) {
            log.warn(
                    "Topic-Replacer: Following content \"{}\" in MultiLevelWildcard replacement is not allowed! Ignoring.",
                    level.substring(replacementInformationEnd + 1));
        }
    }

    private static void replaceMultiWildcardWithStart(
            final @NotNull String level,
            final @NotNull List<String> outTopic,
            final @NotNull MqttTopic inTopic,
            final @NotNull String levelInsert,
            final int replacementInformationStart,
            final int replacementInformationEnd) {
        final int wildcardStartLevel = Integer.parseInt(levelInsert.substring(0, levelInsert.indexOf("-#")));
        final String replacedWildcardLevels =
                inTopic.getLevels().stream().skip(wildcardStartLevel - 1).collect(Collectors.joining("/"));
        if (!replacedWildcardLevels.isEmpty()) {
            outTopic.add(replacedWildcardLevels);
        } else {
            log.warn(
                    "Topic-Modifier: Could not insert topic levels from {{}-#}, because topic \"{}\" is too short! Skipping.",
                    wildcardStartLevel,
                    inTopic);
        }
        if (replacementInformationStart != "{".length()) {
            log.warn(
                    "Topic-Modifier: Leading content \"{}\" in MultiLevelWildcard-With-Start replacement is not allowed! Ignoring.",
                    level.substring(0, replacementInformationStart - 1));
        }
        if (replacementInformationEnd != level.length() - "{".length()) {
            log.warn(
                    "Topic-Modifier: Following content \"{}\" in MultiLevelWildcard-With-Start replacement is not allowed! Ignoring.",
                    level.substring(replacementInformationEnd + 1));
        }
    }

    private static void replaceMultilevel(
            final @NotNull String level,
            final @NotNull List<String> outTopic,
            final @NotNull MqttTopic inTopic,
            final @NotNull String levelInsert,
            final int replacementInformationStart,
            final int replacementInformationEnd) {
        final int startLevel = Integer.parseInt(levelInsert.substring(0, levelInsert.indexOf("-")));
        final int endLevel = Integer.parseInt(levelInsert.substring(levelInsert.indexOf("-") + "-".length()));
        final @Immutable List<String> replacedLevels = inTopic.getLevels();

        if (startLevel > endLevel) {
            log.warn(
                    "Topic-Modifier: Could not insert topic levels from {{}-{}}, because end start level is bigger than end level! Skipping.",
                    startLevel,
                    endLevel);
        } else if (startLevel > replacedLevels.size()) {
            log.warn("Topic-Modifier: Start level from {{}-{}} is bigger than the original topic \"{}\"! Skipping.",
                    startLevel,
                    endLevel,
                    inTopic);
        } else if (endLevel > replacedLevels.size()) {
            log.warn(
                    "Topic-Modifier: End level from {{}-{}} is bigger than the original topic \"{}\"! Only adding existing levels.",
                    startLevel,
                    endLevel,
                    inTopic);
            final String replacedTopicLevelsExisting =
                    replacedLevels.stream().skip(startLevel - 1).collect(Collectors.joining("/"));
            outTopic.add(replacedTopicLevelsExisting);
        } else if (startLevel <= replacedLevels.size()) {
            //String.join() is currently buggy with MqttClient library
            //noinspection SimplifyStreamApiCallChains
            final String replacedTopicLevelsRange =
                    replacedLevels.subList(startLevel - 1, endLevel).stream().collect(Collectors.joining("/"));
            outTopic.add(replacedTopicLevelsRange);
        } else {
            log.warn("Topic-Modifier: Unknown {{}-{}} from topic \"{}\".", startLevel, endLevel, inTopic);
            //Did I forget something?
        }
        if (replacementInformationStart != "{".length()) {
            log.warn("Topic-Modifier: Leading content \"{}\" in MultiLevel replacement is not allowed! Ignoring.",
                    level.substring(0, replacementInformationStart - 1));
        }
        if (replacementInformationEnd != level.length() - "{".length()) {
            log.warn("Topic-Modifier: Following content \"{}\" in MultiLevel replacement is not allowed! Ignoring.",
                    level.substring(replacementInformationEnd + 1));
        }
    }

    private static void replaceSingleLevel(
            final @NotNull String level,
            final @NotNull List<String> outTopic,
            final @NotNull MqttTopic inTopic,
            final @NotNull String levelInsert) {
        final int replaceLevelIndex = Integer.parseInt(levelInsert);
        final @Immutable List<String> replacedLevel = inTopic.getLevels();

        if (replaceLevelIndex <= replacedLevel.size()) {
            final String replaceLevel = replacedLevel.get(replaceLevelIndex - 1);
            final String replaceLevelWithLeadingAndFollowing =
                    level.replace("{" + replaceLevelIndex + "}", replaceLevel);
            outTopic.add(replaceLevelWithLeadingAndFollowing);
        } else {
            log.warn(
                    "Topic-Modifier: Could not insert topic level from {{}}, because level index is bigger than topic's \"{}\" size.",
                    replaceLevelIndex,
                    inTopic);
            final String emptyReplace = level.replace("{" + replaceLevelIndex + "}", "");
            if (!emptyReplace.isEmpty()) {
                outTopic.add(emptyReplace);
            }
        }
    }

    private static String replaceTokens(
            final @NotNull String topicLevel, final @NotNull Map<String, String> tokensMap) {
        Matcher matcher = REGEX_ARBITRARY_TOKENS.matcher(topicLevel);
        String result = topicLevel;
        while (matcher.find()) {
            String token = matcher.group();
            String tokenKey = matcher.group(1);
            String replacementValue;
            if (tokensMap.containsKey(tokenKey)) {
                replacementValue = tokensMap.get(tokenKey);
                result = result.replaceFirst(Pattern.quote(token), replacementValue);
            }
        }
        return result;
    }
}
