package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectOutput;

public interface IVersionedWriteable {
    void writeVersioned(byte version, Persistence.ReaderWriters readerWriters, ObjectOutput output) throws IOException;
}
