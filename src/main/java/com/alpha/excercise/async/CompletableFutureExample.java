package com.alpha.excercise.async;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.System.out;

public class CompletableFutureExample {

    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void callbackHell(String param) {
        performSomeTimeConsumingWork(param, s -> {
            transformResults(s,
                    this::printFinalResult);
            return null;
        });

    }

    private void noHell(String param) {

        CompletableFuture.completedFuture(param)
                .thenCompose(this::performSomeTimeConsumingWorkNewStyle)
                .thenCompose(this::transformResultsNewStyle)
                .thenAccept(this::printFinalResult);
    }

    private void printFinalResult(String r) {
        System.out.println("Finally complete, this is end " + r);
    }

    public static void main(String[] args) throws InterruptedException {
        new CompletableFutureExample().callbackHell("callbackHell");
        new CompletableFutureExample().noHell("NoHell");


        if (!executorService.awaitTermination(50000, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        } else {
            executorService.shutdownNow();
            System.out.println("Exiting normally...");
            System.exit(1);
        }
    }

    private <T,R> void performSomeTimeConsumingWork(T queryParam, Function<T, R> handler) {
        //doing something in async way
        Callable<R> dbWork = () -> {
            sleep(2000);
            out.println(Thread.currentThread().getName() + " DB Work completed happily for " + queryParam);
            return handler.apply(queryParam);
        };
        executorService.submit(dbWork);
    }

    private <T> CompletableFuture<T> performSomeTimeConsumingWorkNewStyle(T queryParam) {
        //doing something in async way
        Callable<T> dbWork = () -> {
            sleep(2000);
            out.println(Thread.currentThread().getName() + " DB Work completed happily for query param " + queryParam);
            return queryParam;
        };
        Future<T> submit = executorService.submit(dbWork);
        return makeCompletableFuture(submit);
    }

    private static <T> CompletableFuture<T> makeCompletableFuture(Future<T> future) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException|ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
    private void transformResults(String input, Consumer<String> handler) {
        Runnable transformWork = () -> {
            sleep(2000);
            String reversedStr = new StringBuilder(input).reverse().toString();
            out.println(Thread.currentThread().getName() + " Transformed input " + input + " to " + reversedStr);
            handler.accept(reversedStr);
        };

        executorService.execute(transformWork);
    }

    private CompletableFuture<String> transformResultsNewStyle(String input) {
        Callable<String> transformWork = () -> {
            sleep(2000);
            String reversedStr = new StringBuilder(input).reverse().toString();
            out.println(Thread.currentThread().getName() + " Transformed input " + input + " to " + reversedStr);
            return reversedStr;
        };

        Future<String> submit = executorService.submit(transformWork);
        return makeCompletableFuture(submit);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
