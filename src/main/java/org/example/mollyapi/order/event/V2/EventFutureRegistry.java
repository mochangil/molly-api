package org.example.mollyapi.order.event.V2;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class EventFutureRegistry {

    private final ConcurrentMap<String, CompletableFuture<Void>> futureMap = new ConcurrentHashMap<>();

    public CompletableFuture<Void> registerFuture(EventFutureType futureType, String tossOrderId) {
        System.out.println("Registering future " + futureType + " to " + tossOrderId);
        String key = futureType.getKey(tossOrderId);
        return futureMap.computeIfAbsent(key, k -> new CompletableFuture<>());
    }

    public void completeFuture(EventFutureType futureType, String tossOrderId) {
        String key = futureType.getKey(tossOrderId);
        System.out.println(futureMap.isEmpty());

        System.out.println(key);
        CompletableFuture<Void> future = futureMap.get(key);
        System.out.println("Completed future " + future + " to " + tossOrderId);
        if (future != null){
            future.complete(null);
        }
    }

    public CompletableFuture<Void> getFuture(EventFutureType futureType, String tossOrderId) {
        String key = futureType.getKey(tossOrderId);
        return futureMap.remove(key);
    }
}
