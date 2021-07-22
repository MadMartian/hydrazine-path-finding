package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3d;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPathNot;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HydrazinePathFinderTests extends AbstractHydrazinePathFinderTests {
    @Mock
    private IPathProcessor pathProcessor;

    @Test
    public void refinePassibilityGate() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(-1, -1, 0);
        pos(0.2f, 0, 0);

        longFence(0, 0, 0);
        when(occlusionProvider.elementAt(0, 0, 0)).thenReturn(Logic.doorway.to(Element.earth.mask));

        final IPath path = pathFinder.initiatePathTo(1, 0, 0);

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

        final IPath path = pathFinder.initiatePathTo(1, 0, 0);

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

        final IPath path = pathFinder.initiatePathTo(-1, 0, 0);

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

        final IPath path = pathFinder.initiatePathTo(0, 0, 1);

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

        final IPath path = pathFinder.initiatePathTo(0, 0, -1);

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

        IPath path = pathFinder.initiatePathTo(0, 0, 7);
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

        IPath path = pathFinder.initiatePathTo(0, 0, 7);

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
        IPath path = pathFinder.initiatePathTo(2, 1, 3);
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
        IPath path = pathFinder.initiatePathTo(2, 1, 3);
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

        final IPath path = pathFinder.initiatePathTo(3, 4, 0);
        assertNull(path);
    }

    @Test
    public void fenceDownButGood() {
        solid(3, 2, 0);
        solid(3, 1, 0);
        solid(3, 0, 0);

        latFence(2, 1, 0);

        pos(2, 3, 0);

        final IPath path = pathFinder.initiatePathTo(3, 3, 0);
        assertPath(path,
                new Vec3i(2, 3, 0),
                new Vec3i(3, 3, 0)
        );
    }

    @Test
    public void volatileDoorway() {
        when(super.capabilities.avoidsDoorways()).thenReturn(false);
        when(super.capabilities.opensDoors()).thenReturn(false);

        solid(0, -1, -1);
        solid(0, -1, 0);
        solid(0, -1, 1);

        door(0, 0, 0, false);
        door(0, 1, 0, false);

        pos(0, 0, -1);

        IPath path = pathFinder.initiatePathTo(0, 0, 1);
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

        final IPath path = pathFinder.initiatePathTo(1, 1, 0);
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

        final IPath path = pathFinder.initiatePathTo(1, 1, 0);
        assertNull(path);
    }

    @Test
    public void noPathIntoAir() {
        pos(0, 0, 0);

        final IPath path = pathFinder.initiatePathTo(0, 5, 3);
        assertNull(path);
    }


    @Test
    public void noPathJustAbove() {
        pos(0, 0, 0);

        final IPath path = pathFinder.initiatePathTo(0, 1, 3);
        assertNull(path);
    }

    @Test
    public void pathProcessor() {
        pos(0, 0, 0);

        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(0, -1, 2);

        final IPath path =
            pathFinder
                .withPathProcessor(pathProcessor)
                .initiatePathTo(0, 0, 2);

        verify(pathProcessor).processPath(path);
    }

    @Test
    public void dontNeuterSatisfactoryQueuedNodes() {
        defaultGround();

        pathFinder.schedulingPriority(SchedulingPriority.low);

        pathFinder.initiatePathTo(0, 0, 4);
        final List<Node> q = pathFinder.queue.view();
        pathFinder.applyPointOptions(q.get(7), q.get(5));
        assertEquals(1, pathFinder.queue.roots().size());
    }

    @Test
    public void dontNeuterUndesirableQueuedNodes() {
        cautious(false);
        defaultGround();

        pathFinder.schedulingPriority(SchedulingPriority.low);

        pathFinder.initiatePathTo(0, 0, 4);
        final List<Node> q = pathFinder.queue.view();
        final Node
            parent = q.get(7),
            enigma = new Node(new Vec3i(-20, 0, 1));

        boolean lengthSetResult = parent.length(109);
        assertTrue(lengthSetResult);

        enigma.passibility(Passibility.risky);
        pathFinder.applyPointOptions(parent, enigma);
        assertEquals(1, pathFinder.queue.roots().size());
    }

    @Test
    public void strictlyReachable() {
        cautious(false);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        final IPath path = pathFinder.initiatePathTo(0, 10, 4, PathOptions.NONE);

        assertNull(path);
    }

    @Test
    public void bestEffort() {
        cautious(false);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        final IPath path = pathFinder.initiatePathTo(0, 10, 4, PathOptions.BEST_EFFORT);

        assertNotNull(path);
    }

    @Test
    public void bestDistanceEffort() {
        cautious(false);
        when(pathingEntity.searchRange()).thenReturn(1000F);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        final IPath path = pathFinder.initiatePathTo(0, 0, 400, PathOptions.BEST_EFFORT);

        assertNotNull(path);
    }

    @Test
    public void tooFar() {
        cautious(false);
        when(pathingEntity.searchRange()).thenReturn(1000F);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        final IPath path = pathFinder.initiatePathTo(0, 0, 400, PathOptions.NONE);

        assertNull(path);
    }

    @Test
    public void nearestPassible() {
        cautious(true);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        IPath path = pathFinder.initiatePathTo(0, 50, 4, new PathOptions().targetingStrategy(PathOptions.TargetingStrategy.gravitySnap));

        assertNotNull(path);
        path = pathFinder.updatePathFor(pathingEntity);

        assertEquals(new Vec3i(0, 0, 4), path.last().coordinates());
    }

    @Test
    public void nearestPassibleTooFar() {
        cautious(true);
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        IPath path = pathFinder.initiatePathTo(0, 50, 500, new PathOptions().targetingStrategy(PathOptions.TargetingStrategy.gravitySnap));

        assertNull(path);
    }

    private void setupBoundTests() {
        defaultGround();
        pathFinder.schedulingPriority(SchedulingPriority.low);

        when(destinationEntity.coordinates()).thenReturn(new Vec3d(0, 0, 20));
        final IPath path = pathFinder.trackPathTo(destinationEntity);

        advance(pathingEntity, path);
        path.update(pathingEntity);

        when(destinationEntity.coordinates()).thenReturn(new Vec3d(0, 0, 30));
    }

    @Test
    public void boundControl() {
        when(pathingEntity.bound()).thenReturn(true);
        setupBoundTests();
        pos(0, 0, 2);

        final IPath path = pathFinder.updatePathFor(pathingEntity);

        // This is a round-about way to detect that resetTriage was NOT called
        assertPath(
            path,

            new Vec3i(0, 0, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, 2),
            new Vec3i(0, 0, 3),
            new Vec3i(0, 0, 4)
        );
    }

    @Test
    public void boundReset() {
        when(pathingEntity.bound()).thenReturn(true);
        setupBoundTests();
        pos(0, 0, 5);

        final IPath path = pathFinder.updatePathFor(pathingEntity);

        // This is a round-about way to detect that resetTriage WAS called
        assertPath(path, new Vec3i(0, 0, 5), new Vec3i(0, 0, 6));
    }

    @Test
    public void unboundControl() {
        setupBoundTests();
        pos(0, 0, 5);

        final IPath path = pathFinder.updatePathFor(pathingEntity);

        // This is a round-about way to detect that resetTriage WAS called
        assertPath(
                path,

                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 0, 2),
                new Vec3i(0, 0, 3),
                new Vec3i(0, 0, 4)
        );
    }

    @Test
    public void closedDoorCapabilitiesControl() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(false);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        door(0, 0, 1, false);
        door(0, 1, 1, false);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertNull(path);
    }

    @Test
    public void openDoorCapabilitiesControl() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(false);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        door(0, 0, 1, true);
        door(0, 1, 1, true);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertNotNull(path);
    }

    @Test
    public void doorControl() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(true);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        door(0, 0, 1, false);
        door(0, 1, 1, false);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, 2));
    }

    @Test
    public void openDoorControl() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(true);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        door(0, 0, 1, true);
        door(0, 1, 1, true);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, 2));
    }

    @Test
    public void intractableDoor() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(true);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        intractableDoor(0, 0, 1, false);
        intractableDoor(0, 1, 1, false);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertNull(path);
    }

    @Test
    public void openIntractableDoor() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(true);

        pos(0, 0, 0);
        solid(0, -1, 0);
        solid(0, -1, 1);
        intractableDoor(0, 0, 1, true);
        intractableDoor(0, 1, 1, true);
        solid(0, -1, 2);

        IPath path = pathFinder.initiatePathTo(0, 0, 2);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, 2));
    }
}
