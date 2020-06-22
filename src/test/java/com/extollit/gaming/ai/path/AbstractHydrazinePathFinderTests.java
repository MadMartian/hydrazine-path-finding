package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.AxisAlignedBBox;
import com.extollit.linalg.immutable.Vec3d;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractHydrazinePathFinderTests {
    protected static final Vec3i ORIGIN = new Vec3i(0, 0, 0);

    protected HydrazinePathFinder pathFinder;

    @Mock protected IInstanceSpace instanceSpace;
    @Mock protected IOcclusionProvider occlusionProvider;
    @Mock protected IDynamicMovableObject destinationEntity;
    @Mock protected IPathingEntity pathingEntity;

    @Mock protected IPathingEntity.Capabilities capabilities;

    @Before
    public void setup() {
        setup(pathingEntity);

        when(destinationEntity.width()).thenReturn(0.6f);
        when(destinationEntity.height()).thenReturn(1.8f);

        pathFinder = new TestableHydrazinePathFinder(pathingEntity, instanceSpace, occlusionProvider);
        pathFinder.schedulingPriority(SchedulingPriority.high);
        pathFinder.occlusionProvider(occlusionProvider);
        when(capabilities.cautious()).thenReturn(true);
        when(capabilities.aquaphobic()).thenReturn(true);
        when(capabilities.climber()).thenReturn(true);
        when(capabilities.opensDoors()).thenReturn(false);
        when(capabilities.avoidsDoorways()).thenReturn(true);
    }

    protected void setup(final IPathingEntity pathingEntity) {
        when(pathingEntity.capabilities()).thenReturn(capabilities);
        when(pathingEntity.searchRange()).thenReturn(32f);

        when(pathingEntity.width()).thenReturn(0.6f);
        when(pathingEntity.height()).thenReturn(1.8f);
    }

    protected void cautious(boolean flag) {
        when(capabilities.cautious()).thenReturn(flag);
    }

    protected void pos(IDynamicMovableObject mockedMob, double x, double y, double z) {
        final Vec3d pos = new Vec3d(x, y, z);
        when(mockedMob.coordinates()).thenReturn(pos);
    }

    protected void pos(double x, double y, double z) {
        final Vec3d pos = new Vec3d(x, y, z);
        when(pathingEntity.coordinates()).thenReturn(pos);
    }

    protected void pos(IPathingEntity mockedPathing, double x, double y, double z) {
        pos((IDynamicMovableObject)pathingEntity, x, y, z);
    }

    protected void solid(final int x, final int y, final int z) {
        when(occlusionProvider.elementAt(x, y, z)).thenReturn(Element.earth.mask);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(TestingBlocks.stone);
    }

    protected void fuzzy(final int x, final int y, final int z) {
        final IBlockObject block = mock(IBlockObject.class);

        when(occlusionProvider.elementAt(x, y, z)).thenReturn(Logic.fuzzy.to(Element.earth.mask));
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(block);
        when(block.bounds()).thenReturn(new AxisAlignedBBox(0, 0, 0, 1, 1, 1));
        when(block.isImpeding()).thenReturn(true);
    }
    protected void slabDown(final int x, final int y, final int z) {
        fuzzy(x, y, z);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(TestingBlocks.slabDown);
    }
    protected void slabUp(final int x, final int y, final int z) {
        fuzzy(x, y, z);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(TestingBlocks.slabUp);
    }

    protected void climb(final int x, final int y, final int z) {
        when(occlusionProvider.elementAt(x, y, z)).thenReturn(Logic.ladder.to(Element.earth.mask));
    }

    protected void clear(final int x, final int y, final int z) {
        when(occlusionProvider.elementAt(x, y, z)).thenReturn(Element.air.mask);
    }

    protected void water(final int x, final int y, final int z) {
        when(occlusionProvider.elementAt(x, y, z)).thenReturn(Element.water.mask);
    }

    protected void diver() {
        when(capabilities.cautious()).thenReturn(false);
        when(capabilities.swimmer()).thenReturn(true);
        when(capabilities.aquaphobic()).thenReturn(false);
    }

    protected void door(final int x, final int y, final int z, boolean open) {
        byte mask = Logic.doorway.mask;
        if (!open)
            mask = Element.earth.to(mask);

        when(occlusionProvider.elementAt(x, y, z)).thenReturn(mask);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(TestingBlocks.door);
    }

    protected void latFence(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0, 0, 0.45f, 1, 1.5f, 0.55f), x, y, z);
    }

    protected void longFence(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0.45f, 0, 0, 0.55f, 1.5, 1), x, y, z);
    }

    protected void cornerFenceSouthEast(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0.45f, 0, 0.45f, 1, 1.5f, 1), x, y, z);
    }

    protected void cornerFenceSouthWest(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0, 0, 0.45f, 0.55f, 1.5f, 1), x, y, z);
    }

    protected void cornerFenceNorthEast(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0.45f, 0, 0, 1, 1.5f, 0.55f), x, y, z);
    }

    protected void cornerFenceNorthWest(final int x, final int y, final int z) {
        fence(new AxisAlignedBBox(0, 0, 0, 0.55f, 1.5f, 0.55f), x, y, z);
    }

    protected void fence(AxisAlignedBBox bounds, final int x, final int y, final int z) {
        IBlockObject block = mock(IBlockObject.class);

        fuzzy(x, y, z);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(block);
        when(block.bounds()).thenReturn(bounds);
        when(block.isImpeding()).thenReturn(true);

        fuzzy(x, y + 1, z);
        when(instanceSpace.blockObjectAt(x, y + 1, z)).thenReturn(block);
        when(block.bounds()).thenReturn(bounds);
        when(block.isImpeding()).thenReturn(true);
    }
}
