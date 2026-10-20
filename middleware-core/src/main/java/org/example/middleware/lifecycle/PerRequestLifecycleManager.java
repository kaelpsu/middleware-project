package org.example.middleware.lifecycle;

import org.example.middleware.identification.ObjectID;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PerRequestLifecycleManager implements LifecycleManager {

    private final Class<?> targetClass;
    private final BlockingQueue<Object> pool;
    private final boolean pooled;

    public PerRequestLifecycleManager(Class<?> targetClass, boolean pooled, int maxPoolSize) {
        this.targetClass = targetClass;
        this.pooled = pooled;

        if (pooled) {
            int size = maxPoolSize > 0 ? maxPoolSize : 10;
            this.pool = new ArrayBlockingQueue<>(size);
            initializePool(size);
        } else {
            this.pool = null;
        }
    }

    private void initializePool(int size) {
        try {
            for (int i = 0; i < size; i++) {
                pool.offer(targetClass.getDeclaredConstructor().newInstance());
            }
            System.out.println("[PerRequestManager] Created pool with " + size + " instances of " + targetClass.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Pool", e);
        }
    }

    @Override
    public Object acquire(ObjectID objectID) throws Exception {
        if (pooled) {
            Object instance = pool.take(); 
            
            if (instance instanceof LifecycleCallback) {
                ((LifecycleCallback) instance).activate();
            }
            return instance;
        } else {
            return targetClass.getDeclaredConstructor().newInstance();
        }
    }

    @Override
    public void release(ObjectID objectID, Object instance) {
        if (pooled) {
            try {
                if (instance instanceof LifecycleCallback) {
                    ((LifecycleCallback) instance).deactivate();
                }
                pool.offer(instance);
            } catch (Exception e) {
                System.err.println("[PerRequestLifecycleManager] Failed to return instance to pool: " + e.getMessage());
            }
        }

        // else, GC cleans it from memory
    }
}