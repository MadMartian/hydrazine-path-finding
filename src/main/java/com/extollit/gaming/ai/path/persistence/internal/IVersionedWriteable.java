package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface IVersionedWriteable {
    void writeVersioned(byte version, ReaderWriters readerWriters, ObjectOutput output) throws IOException;
}
