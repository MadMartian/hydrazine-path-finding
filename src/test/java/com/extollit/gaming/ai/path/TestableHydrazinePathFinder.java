package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.IOcclusionProvider;
import com.extollit.gaming.ai.path.model.IPathingEntity;

public class TestableHydrazinePathFinder extends HydrazinePathFinder {
    public TestableHydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace) {
        super(entity, instanceSpace);
    }

    @Override
    public void occlusionProvider(IOcclusionProvider occlusionProvider) {
        super.occlusionProvider(occlusionProvider);
    }
}
