package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface LinkableReader<A, B> {
    void readLinkages(A object, ReferableObjectInput<B> in) throws IOException;
}
