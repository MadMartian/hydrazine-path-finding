package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Element;
import com.extollit.gaming.ai.path.model.Logic;
import com.extollit.gaming.ai.path.model.PathObject;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPathNot;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HydrazinePathFinderTests extends AbstractHydrazinePathFinderTests {

    @Test
    public void refinePassibilityGate() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(-1, -1, 0);
        pos(0.2f, 0, 0);

        longFence(0, 0, 0);
        when(occlusionProvider.elementAt(0, 0, 0)).thenReturn(Logic.doorway.to(Element.earth.mask));

        final PathObject path = pathFinder.initiatePathTo(1, 0, 0);

        assertNull(path);

        assertTrue(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(+1, 0, 0)));
        assertFalse(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(-1, 0, 0)));
    }

    @Test
    public void refinePassibilityEast() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(-1, -1, 0);
        pos(0.2f, 0, 0);

        longFence(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(1, 0, 0);

        assertNull(path);

        assertTrue(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(+1, 0, 0)));
        assertFalse(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(-1, 0, 0)));
    }

    @Test
    public void refinePassibilityWest() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(-1, -1, 0);
        pos(0.8f, 0, 0);

        longFence(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(-1, 0, 0);

        assertNull(path);

        assertTrue(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(-1, 0, 0)));
        assertFalse(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(+1, 0, 0)));
    }

    @Test
    public void refinePassibilitySouth() {
        solid(0, -1, 0);
        solid(0, -1, +1);
        solid(0, -1, -1);
        pos(0, 0, 0.2f);

        latFence(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(0, 0, 1);

        assertNull(path);

        assertTrue(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(0, 0, +1)));
        assertFalse(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(0, 0, -1)));
    }

    @Test
    public void refinePassibilityNorth() {
        solid(0, -1, 0);
        solid(0, -1, +1);
        solid(0, -1, -1);
        pos(0, 0, 0.8f);

        latFence(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(0, 0, -1);

        assertNull(path);

        assertTrue(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(0, 0, -1)));
        assertFalse(pathFinder.unreachableFromSource(Vec3i.ZERO, new Vec3i(0, 0, +1)));
    }

    @Test
    public void linearRegression() {
        this.pathFinder.schedulingPriority(SchedulingPriority.low);

        defaultGround();

        solid(-1, 0, 3);
        solid(-1, 0, 4);
        solid(-1, 0, 5);
        solid(-1, 0, 6);
        solid(+1, 0, 3);
        solid(+1, 0, 4);
        solid(+1, 0, 5);
        solid(+1, 0, 6);
        solid(-1, 1, 3);
        solid(-1, 1, 4);
        solid(-1, 1, 5);
        solid(-1, 1, 6);
        solid(+1, 1, 3);
        solid(+1, 1, 4);
        solid(+1, 1, 5);
        solid(+1, 1, 6);
        solid(0, 0, 6);
        solid(0, 1, 6);

        pos(0, 0, 0);

        PathObject path = pathFinder.initiatePathTo(0, 0, 7);
        assertPath(path,
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, 2)
        );

        pos(0, 0, 2);
        pathFinder.updatePathFor(this.pathingEntity);
        path = pathFinder.updatePathFor(this.pathingEntity);
        assertPath(path,
            new Vec3i(0, 0, 2),
            new Vec3i(0, 0, 3),
            new Vec3i(0, 0, 4),
            new Vec3i(0, 0, 5)
        );
        pos(0, 0, 4);
        this.pathFinder.schedulingPriority(SchedulingPriority.high);
        path = pathFinder.updatePathFor(this.pathingEntity);
        assertPath(path,
            new Vec3i(0, 0, 4),
            new Vec3i(0, 0, 5)
        );

        path = pathFinder.updatePathFor(this.pathingEntity);
        assertNotNull(path);
        path = pathFinder.updatePathFor(this.pathingEntity);
        assertNotNull(path);
        path = pathFinder.updatePathFor(this.pathingEntity);
        assertNotNull(path);

        path = pathFinder.updatePathFor(this.pathingEntity);
        assertPath(path,
            new Vec3i(0, 0, 4),
            new Vec3i(0, 0, 3),
            new Vec3i(0, 0, 2),
            new Vec3i(1, 0, 2),
            new Vec3i(2, 0, 2),
            new Vec3i(2, 0, 3),
            new Vec3i(2, 0, 4),
            new Vec3i(2, 0, 5),
            new Vec3i(2, 0, 6),
            new Vec3i(2, 0, 7),
            new Vec3i(1, 0, 7),
            new Vec3i(0, 0, 7)
        );
    }

    @Test
    public void longWayRound() {
        defaultGround();

        solid(-1, 0, 3);
        solid(-1, 0, 4);
        solid(-1, 0, 5);
        solid(-1, 0, 6);
        solid(+1, 0, 3);
        solid(+1, 0, 4);
        solid(+1, 0, 5);
        solid(+1, 0, 6);
        solid(-1, 1, 3);
        solid(-1, 1, 4);
        solid(-1, 1, 5);
        solid(-1, 1, 6);
        solid(+1, 1, 3);
        solid(+1, 1, 4);
        solid(+1, 1, 5);
        solid(+1, 1, 6);
        solid(0, 0, 6);
        solid(0, 1, 6);

        pos(0, 0, 5);

        PathObject path = pathFinder.initiatePathTo(0, 0, 7);

        assertNotNull(path);
        assertPath(path,
            new Vec3i(0, 0, 5),
            new Vec3i(0, 0, 4)
        );

        pos(0, 0, 4);

        path = pathFinder.updatePathFor(this.pathingEntity);
        assertPath(path,
                new Vec3i(0, 0, 5),
                new Vec3i(0, 0, 4),
                new Vec3i(0, 0, 3),
                new Vec3i(0, 0, 2),
                new Vec3i(-1, 0, 2),
                new Vec3i(-2, 0, 2),
                new Vec3i(-2, 0, 3),
                new Vec3i(-2, 0, 4),
                new Vec3i(-2, 0, 5),
                new Vec3i(-2, 0, 6)
        );
        path = pathFinder.updatePathFor(this.pathingEntity);
        assertNotNull(path);
        path = pathFinder.updatePathFor(this.pathingEntity);

        assertPath(path,
                new Vec3i(0, 0, 5),
                new Vec3i(0, 0, 4),
                new Vec3i(0, 0, 3),
                new Vec3i(0, 0, 2),
                new Vec3i(1, 0, 2),
                new Vec3i(2, 0, 2),
                new Vec3i(2, 0, 3),
                new Vec3i(2, 0, 4),
                new Vec3i(2, 0, 5),
                new Vec3i(2, 0, 6),
                new Vec3i(2, 0, 7),
                new Vec3i(1, 0, 7),
                new Vec3i(0, 0, 7)
        );
    }

    @Test
    public void closestsOscillator() {
        defaultGround();
        pos(1, 0, 3);
        PathObject path = pathFinder.initiatePathTo(2, 1, 3);
        assertNotNull(path);

        assertPath(path,
            new Vec3i(1, 0, 3),
            new Vec3i(2, 0, 3)
        );

        pos(2, 0, 3);
        path = pathFinder.updatePathFor(this.pathingEntity);

        assertPathNot(path,
            new Vec3i(2, 0, 3),
            new Vec3i(1, 0, 3)
        );
    }

    @Test
    public void closestsDeterminism() {
        defaultGround();
        pos(1, 0, 3);
        PathObject path = pathFinder.initiatePathTo(2, 1, 3);
        assertNotNull(path);

        assertPath(path,
                new Vec3i(1, 0, 3),
                new Vec3i(2, 0, 3)
        );

        path = pathFinder.updatePathFor(this.pathingEntity);

        assertPath(path,
                new Vec3i(1, 0, 3),
                new Vec3i(2, 0, 3)
        );
    }

    @Test
    public void fenceDownToHighUp() {
        solid(3, 3, 0);
        solid(3, 2, 0);
        solid(3, 1, 0);
        solid(3, 0, 0);

        latFence(2, 1, 0);

        pos(2, 3, 0);

        final PathObject path = pathFinder.initiatePathTo(3, 4, 0);
        assertNull(path);
    }

    @Test
    public void fenceDownButGood() {
        solid(3, 2, 0);
        solid(3, 1, 0);
        solid(3, 0, 0);

        latFence(2, 1, 0);

        pos(2, 3, 0);

        final PathObject path = pathFinder.initiatePathTo(3, 3, 0);
        assertPath(path,
                new Vec3i(2, 3, 0),
                new Vec3i(3, 3, 0)
        );
    }

    @Test
    public void volatileDoorway() {
        when(super.capabilities.avoidsDoorways()).thenReturn(false);

        solid(0, -1, -1);
        solid(0, -1, 0);
        solid(0, -1, 1);

        door(0, 0, 0, false);
        door(0, 1, 0, false);

        pos(0, 0, -1);

        PathObject path = pathFinder.initiatePathTo(0, 0, 1);
        assertNull(path);

        door(0, 0, 0, true);
        door(0, 1, 0, true);

        pathFinder.resetTriage();
        path = pathFinder.updatePathFor(pathingEntity);
        assertPath(path,
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, +1)
        );
    }

    @Test
    public void swimOut() {
        when(super.capabilities.swimmer()).thenReturn(true);
        when(super.capabilities.cautious()).thenReturn(false);

        water(0, 0, 0);
        water(0, -1, 0);
        solid(0, -2, 0);
        solid(1, 0, 0);
        solid(1, -1, 0);
        solid(1, -2, 0);

        pos(0, -1, 0);

        final PathObject path = pathFinder.initiatePathTo(1, 1, 0);
        assertNotNull(path);

        assertPath(
                path,
                new Vec3i(0, 1, 0),
                new Vec3i(1, 1, 0)
        );
    }
    
    @Test
    public void sink() {
        when(super.capabilities.swimmer()).thenReturn(false);
        when(super.capabilities.cautious()).thenReturn(false);

        water(0, 0, 0);
        water(0, -1, 0);
        solid(0, -2, 0);
        solid(1, 0, 0);
        solid(1, -1, 0);
        solid(1, -2, 0);

        pos(0, -1, 0);

        final PathObject path = pathFinder.initiatePathTo(1, 1, 0);
        assertNull(path);
    }

    @Test
    public void noPathIntoAir() {
        pos(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(0, 5, 3);
        assertNull(path);
    }


    @Test
    public void noPathJustAbove() {
        pos(0, 0, 0);

        final PathObject path = pathFinder.initiatePathTo(0, 1, 3);
        assertNull(path);
    }
}
