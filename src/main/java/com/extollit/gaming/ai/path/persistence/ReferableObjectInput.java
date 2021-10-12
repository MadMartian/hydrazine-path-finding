package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectInput;

public interface ReferableObjectInput<T> extends ObjectInput {
    T readRef() throws IOException;
    T readNullableRef() throws IOException;
}
