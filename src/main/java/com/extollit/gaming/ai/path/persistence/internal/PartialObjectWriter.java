package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface PartialObjectWriter<T> {
    void writePartialObject(T object, ObjectOutput out) throws IOException;
}
