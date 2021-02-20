package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;

public interface LinkableWriter<A, B> {
    void writeLinkages(A object, ReferableObjectOutput<B> out) throws IOException;
}
