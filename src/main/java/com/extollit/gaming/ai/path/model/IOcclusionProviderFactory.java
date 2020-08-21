package com.extollit.gaming.ai.path.model;

public interface IOcclusionProviderFactory {
    IOcclusionProvider fromInstanceSpace(IInstanceSpace instance, int cx0, int cz0, int cxN, int czN);
}
