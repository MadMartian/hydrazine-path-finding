package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectInput;

public interface IVersionedReadable {
    void readVersioned(byte version, ObjectInput input) throws IOException;
}
