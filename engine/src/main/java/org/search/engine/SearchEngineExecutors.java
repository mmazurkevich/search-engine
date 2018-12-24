package org.search.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Class which store executor services for running tasks and scheduler jobs concurrently
 */
public class SearchEngineExecutors {

    private static final int EXECUTOR_THREADS;
    private static ScheduledExecutorService scheduledExecutor;
    private static ExecutorService executorService;
    static {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int remainingCores = availableProcessors / 3;
        if (remainingCores < 1) {
            EXECUTOR_THREADS = 1;
        } else {
            EXECUTOR_THREADS = remainingCores;
        }
    }

    private SearchEngineExecutors() {
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        if (scheduledExecutor == null) {
            synchronized (SearchEngineExecutors.class) {
                if (scheduledExecutor == null) {
                    scheduledExecutor = Executors.newScheduledThreadPool(2);
                }
            }
        }
        return scheduledExecutor;
    }

    public static ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (SearchEngineExecutors.class) {
                if (executorService == null) {
                    executorService = Executors.newFixedThreadPool(EXECUTOR_THREADS);
                }
            }
        }
        return executorService;
    }
}
