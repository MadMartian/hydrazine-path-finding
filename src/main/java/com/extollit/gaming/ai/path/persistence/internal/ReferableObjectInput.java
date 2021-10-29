package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface ReferableObjectInput<T> extends ObjectInput {
    T readRef() throws IOException;
    T readNullableRef() throws IOException;
}
