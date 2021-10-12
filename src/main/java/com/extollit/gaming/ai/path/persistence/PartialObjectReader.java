package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectInput;

public interface PartialObjectReader<T> {
    T readPartialObject(ObjectInput in) throws IOException;
}
