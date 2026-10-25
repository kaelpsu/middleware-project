package org.example.middleware.identification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer {

    private final Map<ObjectID, AtomicInteger> indexes = new ConcurrentHashMap<>();

    public AbsoluteObjectReference select(ObjectID objectID, List<AbsoluteObjectReference> availableReferences) {
        if (availableReferences == null || availableReferences.isEmpty()) {
            return null;
        }

        AtomicInteger indexHolder = indexes.computeIfAbsent(objectID, k -> new AtomicInteger(0));
        int nextIndex = Math.abs(indexHolder.getAndIncrement()) % availableReferences.size();

        return availableReferences.get(nextIndex);
    }
}