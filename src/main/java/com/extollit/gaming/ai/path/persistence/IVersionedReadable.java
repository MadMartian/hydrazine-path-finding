package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectInput;

public interface IVersionedReadable {
    void readVersioned(byte version, Persistence.ReaderWriters readerWriters, ObjectInput input) throws IOException;
}
