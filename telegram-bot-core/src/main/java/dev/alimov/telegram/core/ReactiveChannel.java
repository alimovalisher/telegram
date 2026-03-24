package dev.alimov.telegram.core;

/**
 * Represents a reactive communication channel that supports publishing messages and subscribing to them.
 * It provides an abstraction for non-blocking message queuing operations.
 *
 */
public interface ReactiveChannel extends AutoCloseable {


    /**
     * Close the queue and release any underlying resources.
     *
     */
    void close();
}
