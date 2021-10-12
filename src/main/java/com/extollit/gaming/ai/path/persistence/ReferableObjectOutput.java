package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectOutput;

public interface ReferableObjectOutput<T> extends ObjectOutput {
    void writeRef(T object) throws IOException;
    void writeNullableRef(T object) throws IOException;
}
