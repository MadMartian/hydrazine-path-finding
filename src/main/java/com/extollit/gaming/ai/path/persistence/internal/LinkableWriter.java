package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface LinkableWriter<A, B> {
    void writeLinkages(A object, ReferableObjectOutput<B> out) throws IOException;
}
