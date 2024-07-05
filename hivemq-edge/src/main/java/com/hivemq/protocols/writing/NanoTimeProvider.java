package com.hivemq.protocols.writing;


import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple Wrapper used to have an easier time in unit tests when nano time is used in methods and we want to
 * control/mock it.
 */
@Singleton
public class NanoTimeProvider {

    @Inject
    NanoTimeProvider() {
    }

    public long nanoTime() {
        return System.nanoTime();
    }

}
