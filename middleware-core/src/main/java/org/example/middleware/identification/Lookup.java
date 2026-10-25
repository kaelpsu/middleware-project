package org.example.middleware.identification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Lookup {

    private static final long LEASE_TIMEOUT_MS = 5000; // heartbeat timeout

    private final Map<ObjectID, List<AbsoluteObjectReference>> registry = new ConcurrentHashMap<>();
    private final Map<AbsoluteObjectReference, Long> leases = new ConcurrentHashMap<>();

    private final RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer();


    public Lookup() {
        ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Lookup-Lease-Sweeper");
            t.setDaemon(true);
            return t;
        });
        janitor.scheduleAtFixedRate(this::removeExpiredReferences, 2, 2, TimeUnit.SECONDS);
    }

    public void registerReference(String host, int port, String serviceName) {
        AbsoluteObjectReference reference = new AbsoluteObjectReference(host, port, new ObjectID(serviceName));
        ObjectID serviceId = reference.objectID();
        List<AbsoluteObjectReference> references = registry.computeIfAbsent(
                serviceId, k -> new CopyOnWriteArrayList<>()
        );

        if (!references.contains(reference)) {
            references.add(reference);
            System.out.println("[Lookup] New replica registered: " + reference);
        }

        leases.put(reference, System.currentTimeMillis());
    }

    public AbsoluteObjectReference lookup(String serviceName) {
        ObjectID serviceId = new ObjectID(serviceName);
        List<AbsoluteObjectReference> activeRefs = registry.getOrDefault(serviceId, List.of());
        return loadBalancer.select(serviceId, activeRefs);
    }

    public List<AbsoluteObjectReference> lookupAll(String serviceName) {
        return registry.getOrDefault(new ObjectID(serviceName), List.of());
    }

    private void removeExpiredReferences() {
        long now = System.currentTimeMillis();
        registry.forEach((serviceId, references) -> {
            references.removeIf(ref -> {
                boolean expired = (now - leases.getOrDefault(ref, 0L)) > LEASE_TIMEOUT_MS;
                if (expired) {
                    leases.remove(ref);
                    System.err.println("[Lookup] Inactive replica removed: " + ref);
                }
                return expired;
            });
        });
    }
}