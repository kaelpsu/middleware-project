package org.example.middleware.lifecycle;

import org.example.middleware.identification.ObjectID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ClientDependentLifecycleManager implements LifecycleManager {

    private record ClientSession(Object instance, AtomicLong lastAccessTime) {}

    private final Class<?> targetClass;
    private final Map<ObjectID, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private final long leaseTimeoutMs;

    public ClientDependentLifecycleManager(Class<?> targetClass, long leaseTimeoutMs) {
        this.targetClass = targetClass;
        this.leaseTimeoutMs = leaseTimeoutMs > 0 ? leaseTimeoutMs : 300000; // 5 min
        startLeaseSweeper();
    }

    @Override
    public Object acquire(ObjectID objectID) throws Exception {
        long now = System.currentTimeMillis();

        ClientSession session = activeSessions.computeIfAbsent(objectID, id -> {
            try {
                System.out.println("[ClientDependentManager] Creating new instance for client ID: " + id);
                Object newInstance = targetClass.getDeclaredConstructor().newInstance();
                return new ClientSession(newInstance, new AtomicLong(now));
            } catch (Exception e) {
                throw new RuntimeException("Failed while creating instance for client.", e);
            }
        });

        session.lastAccessTime.set(now);
        return session.instance;
    }

    @Override
    public void release(ObjectID objectID, Object instance) {
        // session is kept in memory until leaseTimeout
    }

    private void startLeaseSweeper() {
        Thread sweeper = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // 10s
                    long now = System.currentTimeMillis();
                    activeSessions.entrySet().removeIf(entry -> {
                        if (now - entry.getValue().lastAccessTime.get() > leaseTimeoutMs) {
                            System.out.println("[LeaseSweeper] Lease expired for client ID: " + entry.getKey() + ". Removing instance...");
                            
                            Object instance = entry.getValue().instance;

                            if (instance instanceof LifecycleCallback) {
                              ((LifecycleCallback) instance).deactivate();  
                            }
                            
                            System.out.println("[Lease Sweeper] Deactivated ID: " + entry.getKey());
                            return true;
                        }
                        return false;
                    });
                } catch (InterruptedException e) { break; }
            }
        });
        sweeper.setDaemon(true);
        sweeper.start();
    }
}