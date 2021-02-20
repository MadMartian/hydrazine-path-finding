package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;

public interface LinkableReader<A, B> {
    void readLinkages(A object, ReferableObjectInput<B> in) throws IOException;
}
