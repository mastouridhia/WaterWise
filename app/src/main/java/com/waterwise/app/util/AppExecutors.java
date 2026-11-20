package com.waterwise.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background thread for database writes and file copies. Android forbids
 * this kind of slow work on the main (UI) thread.
 */
public final class AppExecutors {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private AppExecutors() {
    }

    public static ExecutorService io() {
        return IO;
    }
}
