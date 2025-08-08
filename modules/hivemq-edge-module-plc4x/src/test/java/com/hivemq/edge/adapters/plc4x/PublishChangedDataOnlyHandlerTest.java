package com.hivemq.edge.adapters.plc4x;

import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishChangedDataOnlyHandlerTest {

    @Test
    public void test() {
        final var factory = new AdapterFactoriesImpl();
        final var dataPointFactory = factory.dataPointFactory();
        final var toTest = new PublishChangedDataOnlyHandler();
        final var initial = toTest.areValuesNew("tag1", List.of(dataPointFactory.create("tag1", "value1")));
        final var secondTry = toTest.areValuesNew("tag1", List.of(dataPointFactory.create("tag1", "value1")));

        assertThat(initial).isTrue();
        assertThat(secondTry).isFalse();
    }
}
