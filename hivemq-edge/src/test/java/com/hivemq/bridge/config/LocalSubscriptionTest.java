package com.hivemq.bridge.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LocalSubscriptionTest {

    @Test
    void calculateUniqueId_whenOrderWithinListsIsChanged_thenSameUniqueIdMustResult() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        final LocalSubscription topicSwitched = new LocalSubscription(List.of("topicC/#", "topicB/#", "topicA/+"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        assertEquals(localSubscription.calculateUniqueId(), topicSwitched.calculateUniqueId());


        final LocalSubscription excludesSwitched = new LocalSubscription(List.of("topicC/#", "topicB/#", "topicA/+"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        assertEquals(localSubscription.calculateUniqueId(), excludesSwitched.calculateUniqueId());

        final LocalSubscription customPropertiesSwitched =
                new LocalSubscription(List.of("topicC/#", "topicB/#", "topicA/+"),
                        "destinationTopic",
                        List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                        List.of(CustomUserProperty.of("key2", "value2"),
                                CustomUserProperty.of("key1", "value1"),
                                CustomUserProperty.of("key3", "value3")),
                        true,
                        2);

        assertEquals(localSubscription.calculateUniqueId(), customPropertiesSwitched.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInTopic_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        // "topicA/+" => "topicB/+"
        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicB/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInDestinationTopic_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        // "topicA/+" => "topicB/+"
        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic2",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInExcludeTopic_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        // "topicA/topic/" => "topicA/topic/B"
        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/B", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInCustomProperties_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        // "key1", "value1" => "key4", "value1"
        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key4", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInPreserveRetain_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                false,
                2);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenChangeInMaxQoS_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);

        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                1);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }

    @Test
    void calculateUniqueId_whenStringInDifferentFieldsAreSwapped_thenUniqueIdsAreDifferent() {
        final LocalSubscription localSubscription = new LocalSubscription(List.of("topicA/+", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/topic/", "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                2);


        // "topicA/+" and "topicA/topic/" are swapped
        final LocalSubscription otherSubscription = new LocalSubscription(List.of("topicA/topic/", "topicB/#", "topicC/#"),
                "destinationTopic",
                List.of("topicA/+",  "topicA/topic/", "otherTopic"),
                List.of(CustomUserProperty.of("key1", "value1"),
                        CustomUserProperty.of("key2", "value2"),
                        CustomUserProperty.of("key3", "value3")),
                true,
                1);
        assertNotEquals(localSubscription.calculateUniqueId(), otherSubscription.calculateUniqueId());
    }
}
