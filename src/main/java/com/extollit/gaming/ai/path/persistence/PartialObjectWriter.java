package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectOutput;

public interface PartialObjectWriter<T> {
    void writePartialObject(T object, ObjectOutput out) throws IOException;
}
