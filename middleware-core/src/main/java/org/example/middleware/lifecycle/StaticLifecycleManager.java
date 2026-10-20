package org.example.middleware.lifecycle;

import org.example.middleware.identification.ObjectID;

public class StaticLifecycleManager implements LifecycleManager {

    private Object instance;
    private final Class<?> targetClass;

    public StaticLifecycleManager(Class<?> targetClass, boolean isLazy) {
        this.targetClass = targetClass;
        
        if (!isLazy) {
            this.instance = instantiateEagerly();
        }
    }

    private synchronized Object instantiateEagerly() {
        try {
            System.out.println("[StaticLifecycleManager] Created static instance (Eager): " + targetClass.getSimpleName());
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("[StaticLifecycleManager] Failed while instantiating static service", e);
        }
    }

    @Override
    public synchronized Object acquire(ObjectID objectID) throws Exception {
        if (instance == null) {
            System.out.println("[StaticLifecycleManager] Lazy Acquisition activated for " + targetClass.getSimpleName());
            instance = targetClass.getDeclaredConstructor().newInstance();
            if (instance instanceof LifecycleCallback) {
                ((LifecycleCallback) instance).activate();
            }
        }
        return instance;
    }

    @Override
    public void release(ObjectID objectID, Object instance) {
        // static instances live forever, so do nothing
    }

}