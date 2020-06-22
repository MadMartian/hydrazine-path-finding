package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.AreaOcclusionProvider;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.IOcclusionProvider;
import com.extollit.gaming.ai.path.model.IPathingEntity;

public class TestableHydrazinePathFinder extends HydrazinePathFinder {
    private final IOcclusionProvider occlusionProvider;

    public TestableHydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace, IOcclusionProvider occlusionProvider) {
        super(entity, instanceSpace);
        this.occlusionProvider = occlusionProvider;
    }

    @Override
    public void occlusionProvider(IOcclusionProvider occlusionProvider) {
        super.occlusionProvider(occlusionProvider);
    }

    @Override
    protected IOcclusionProvider occlusionProviderFor(int cx0, int cz0, int cxN, int czN) {
        return this.occlusionProvider;
    }
}
