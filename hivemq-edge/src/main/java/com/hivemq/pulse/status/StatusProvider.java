package com.hivemq.pulse.status;


import org.jetbrains.annotations.NotNull;

public interface StatusProvider {

    @NotNull Status getStatus();

    /**
     * Adds a listener that will be notified when the status changes.
     * Will be called once with current state when registered.
     *
     * @param listener the listener
     */
    void addStatusChangedListener(@NotNull StatusChangedListener listener);

    void removeStatusChangedListener(@NotNull StatusChangedListener listener);

    interface StatusChangedListener {
        void onStatusChanged(@NotNull Status status);
    }

    /**
     * Activates pulse with the given connection string.
     *
     * @param connectionString the pulse connection string
     * @return if the connection string is valid and the pulse could be activated
     */
    boolean activatePulse(final @NotNull String connectionString);

}
