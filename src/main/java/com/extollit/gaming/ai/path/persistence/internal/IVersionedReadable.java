package com.extollit.gaming.ai.path.persistence.internal;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public interface IVersionedReadable {
    void readVersioned(byte version, ReaderWriters readerWriters, ObjectInput input) throws IOException;
}
