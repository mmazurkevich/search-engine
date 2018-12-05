package org.search.engine;

import java.util.concurrent.*;

public class SearchEngineExecutors {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static ScheduledExecutorService notificationManagerExecutor;
    private static ExecutorService documentIndexingExecutor;

    private SearchEngineExecutors() {
    }

    public static ScheduledExecutorService getNotificationManagerExecutor() {
        if (notificationManagerExecutor == null) {
            synchronized (SearchEngineExecutors.class) {
                if (notificationManagerExecutor == null) {
                    notificationManagerExecutor = Executors.newSingleThreadScheduledExecutor();
                }
            }
        }
        return notificationManagerExecutor;
    }

    public static ExecutorService getDocumentIndexingExecutor() {
        if (documentIndexingExecutor == null) {
            synchronized (SearchEngineExecutors.class) {
                if (documentIndexingExecutor == null) {
                    documentIndexingExecutor = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);
                }
            }
        }
        return documentIndexingExecutor;
    }
}
