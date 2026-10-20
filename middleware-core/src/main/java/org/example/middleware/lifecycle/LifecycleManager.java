package org.example.middleware.lifecycle;

import org.example.middleware.identification.ObjectID;

public interface LifecycleManager {
    
    Object acquire(ObjectID objectID) throws Exception;

    void release(ObjectID objectID, Object instance);
}