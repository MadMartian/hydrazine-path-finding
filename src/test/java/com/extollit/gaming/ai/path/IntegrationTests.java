package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3d;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTests extends AbstractHydrazinePathFinderTests {
    @Test
    public void pathRiskyDown() {
        when(super.capabilities.cautious()).thenReturn(false);

        solid(-4, -1, 0);
        solid(-4, 0, 0);
        solid(-4, 1, 0);
        solid(-4, 2, 0);
        solid(-5, 2, 0);
        solid(-3, -1, 0);
        solid(-2, -1, 0);
        solid(-1, -1, 0);
        solid(0, -1, 0);

        pos(super.destinationEntity, 0.5, 0, 0.5);
        pos(super.pathingEntity, -4.5, 3, 0.5);

        IPath path = pathFinder.trackPathTo(destinationEntity);

        final Vec3i[] expectedPath = {
                new Vec3i(-5, 3, 0),
                new Vec3i(-4, 3, 0),
                new Vec3i(-3, 0, 0),
                new Vec3i(-2, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(0, 0, 0)
        };
        assertPath(path, expectedPath);
    }

    @Test
    public void bruteOverTrapdoor() {
        when(super.capabilities.opensDoors()).thenReturn(true);
        when(super.capabilities.avoidsDoorways()).thenReturn(false);

        solid(0, -1, 0);
        door(1, -1, 0, false);
        solid(2, -1, 0);

        pos(super.pathingEntity, 0, 0, 0);

        IPath path = pathFinder.initiatePathTo(2, 0, 0);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(2, 0, 0));
    }

    @Test
    public void trapdoor() {
        when(super.capabilities.opensDoors()).thenReturn(false);
        when(super.capabilities.avoidsDoorways()).thenReturn(false);

        solid(0, -1, 0);
        door(1, -1, 0, false);
        solid(2, -1, 0);

        pos(super.pathingEntity, 0, 0, 0);

        IPath path = pathFinder.initiatePathTo(2, 0, 0);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), new Vec3i(2, 0, 0));
    }

    @Test
    public void openTrapdoor() {
        when(super.capabilities.opensDoors()).thenReturn(false);
        when(super.capabilities.avoidsDoorways()).thenReturn(false);

        solid(0, -1, 0);
        door(1, -1, 0, true);
        solid(2, -1, 0);

        pos(super.pathingEntity, 0, 0, 0);

        IPath path = pathFinder.initiatePathTo(2, 0, 0);
        assertNull(path);
    }

    @Test
    public void solidStart() {
        solid(0, -1, 0);
        solid(0, 0, 0);
        solid(0, 1, 0);

        solid(1, -1, 0);

        pos(super.pathingEntity, 0, 0, 0);

        final IPath path = pathFinder.initiatePathTo(1, 0, 0);

        assertPath(path, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0));
    }

    @Test
    public void groundedStart() {
        when(super.capabilities.cautious()).thenReturn(false);
        pos(super.pathingEntity, 0, 5, 0);

        solid(0, 0, 0);
        solid(1, 0, 0);


        final IPath path = pathFinder.initiatePathTo(1, 1, 0);

        assertPath(path, new Vec3i(0, 1, 0), new Vec3i(1, 1, 0));
    }

    @Test
    public void divingStart() {
        when(super.capabilities.cautious()).thenReturn(false);
        when(super.capabilities.swimmer()).thenReturn(true);

        water(0, 0, 0);
        water(1, 0, 0);
        water(0, -1, 0);
        water(1, -1, 0);
        water(0, -2, 0);
        water(1, -2, 0);
        solid(0, -3, 0);
        solid(1, -3, 0);
        solid(2, 0, 0);
        solid(3, 0, 0);

        pos(super.pathingEntity, 0, 5, 0);

        final PathObject path = (PathObject) pathFinder.initiatePathTo(3, 1, 0);

        assertPath(path,
                new Vec3i(0, 1, 0),
                new Vec3i(1, 1, 0),
                new Vec3i(2, 1, 0),
                new Vec3i(3, 1, 0)
        );

        path.update(super.pathingEntity);

        assertEquals(0, path.i);

        pos(super.pathingEntity, 0.5, 1, 0.5);
        path.update(super.pathingEntity);

        assertEquals(3, path.i);
    }

    @Test
    public void fatOutOfPool() {
        when(super.capabilities.cautious()).thenReturn(false);
        when(super.capabilities.swimmer()).thenReturn(true);
        when(super.pathingEntity.width()).thenReturn(1.4f);

        water(1, 0, -1);
        water(0, 0, -1);
        water(-1, 0, -1);
        water(1, 0, 0);
        water(0, 0, 0);
        water(-1, 0, 0);
        water(-2, 0, 1);
        water(-1, 0, 1);

        solid(1, 1, 0);
        solid(1, 1, +1);
        solid(1, 1, -1);
        solid(1, 0, 0);
        solid(1, 0, +1);
        solid(1, 0, -1);
        solid(1, -1, 0);
        solid(1, -1, +1);
        solid(1, -1, -1);
        solid(-2, -1, -1);
        solid(-1, -1, -1);
        solid(0, -1, -1);
        solid(-2, -1, 0);
        solid(-1, -1, 0);
        solid(0, -1, 0);
        solid(-2, -1, 1);
        solid(1, 0, 1);
        solid(1, 1, 1);
        solid(0, 0, 1);
        solid(0, 0, 2);
        solid(1, 0, 2);
        solid(0, 1, 1);
        solid(0, 1, 2);
        solid(1, 1, 2);
        solid(0, 1, 3);
        solid(1, 1, 3);
        solid(-1, -1, 1);
        solid(-1, 0, 2);

        pos(super.pathingEntity, 0, 0.5, 0);

        final PathObject path = (PathObject) pathFinder.initiatePathTo(0, 2, 3);

        assertPath(
            path,
            new Vec3i(0, 1, 0),
            new Vec3i(-1, 1, 0),
            new Vec3i(-1, 1, 1),
            new Vec3i(-1, 1, 2),
            new Vec3i(-1, 1, 3),
            new Vec3i(0, 2, 3)
        );

        path.update(pathingEntity);
        assertEquals(1, path.i);

        pos(super.pathingEntity, -0.9, 0.5, 0.2);
        path.update(pathingEntity);
        assertEquals(2, path.i);

        pos(super.pathingEntity, -0.9, 0.5, 1.1);
        path.update(pathingEntity);
        assertEquals(3, path.i);

        pos(super.pathingEntity, -0.9, 1, 1.9);
        path.update(pathingEntity);
        assertEquals(4, path.i);

        pos(super.pathingEntity, -0.05, 1, 3.1);
        path.update(pathingEntity);
        assertTrue(path.done());
    }

    @Test
    public void trackEntity() {
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(1, 10, 1));
        when(destinationEntity.coordinates()).thenReturn(new Vec3d(3, 10, 1));
        solid(1, 9, -1);
        solid(1, 9, 0);
        solid(1, 9, 1);
        solid(2, 9, 1);
        solid(3, 9, 1);

        IPath pathObject = pathFinder.trackPathTo(destinationEntity);

        assertNotNull(pathObject);
        INode last = pathObject.last();
        assertNotNull(last);
        assertEquals(new Vec3i(3, 10, 1), last.coordinates());

        solid(1, 9, 0);
        solid(1, 9, -1);

        when(destinationEntity.coordinates()).thenReturn(new Vec3d(1, 10, -1));
        pathObject = pathFinder.update(pathingEntity);

        assertNotNull(pathObject);
        last = pathObject.last();
        assertNotNull(last);
        assertEquals(new Vec3i(1, 10, -1), last.coordinates());
    }

    @Test
    public void stuckFence() {
        pos(1.8f, 1, 1);
        solid(1, 0, 1);
        solid(0, 0, 1);
        longFence(1, 1, 1);

        final IPath path = pathFinder.initiatePathTo(0, 1, 1);

        assertNull(path);
    }

    @Test
    public void unstuckFenceCorner() {
        cautious(true);

        pos(0.8f, 1, 0.8f);
        solid(1, 0, 1);
        solid(0, 0, 1);
        solid(1, 0, 0);
        solid(0, 0, 0);

        solid(0, 0, 2);
        solid(1, 0, 2);
        solid(2, 0, 2);
        solid(2, 0, 0);
        solid(2, 0, 1);

        cornerFenceSouthEast(0, 1, 0);
        latFence(1, 1, 0);
        longFence(0, 1, 1);

        final IPath path = pathFinder.initiatePathTo(2, 1, 2);

        assertNotNull(path);
        assertPath(
                path,

                new Vec3i(0, 1, 0),
                new Vec3i(1, 1, 1),
                new Vec3i(1, 1, 2),
                new Vec3i(2, 1, 2)
        );
    }

    @Test
    public void stuckFenceCorner() {
        cautious(true);

        solid(1, 0, 1);
        solid(0, 0, 1);
        solid(1, 0, 0);
        solid(0, 0, 0);

        solid(0, 0, 2);
        solid(1, 0, 2);
        solid(2, 0, 2);
        solid(2, 0, 0);
        solid(2, 0, 1);

        solid(0, 0, -1);
        solid(1, 0, -1);
        solid(2, 0, -1);

        cornerFenceSouthEast(0, 1, 0);
        latFence(1, 1, 0);
        latFence(2, 1, 0);
        longFence(0, 1, 1);

        pos(0.8f, 1, 0.8f);

        final IPath path = pathFinder.initiatePathTo(1, 1, -1);

        assertNull(path);
    }

    @Test
    public void fencedOut() {
        solid(0, -1, 0);
        solid(0, -1, -1);
        solid(0, -1, +1);
        solid(1, -1, 0);
        solid(1, -1, -1);
        solid(1, -1, +1);
        solid(2, -1, 0);
        solid(2, -1, -1);
        solid(2, -1, +1);
        pos(0, 0, 0);

        longFence(1, 0, 0);
        longFence(1, 0, -1);
        longFence(1, 0, +1);

        final IPath path = pathFinder.initiatePathTo(2, 0, 0);

        assertNull(path);
    }

    @Test
    public void stuckSoTaxi() {
        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(0, -1, 2);
        solid(0, -1, 3);

        when(capabilities.speed()).thenReturn(1.0f);
        pos(0.5, 0, 0.5);
        IPath path = pathFinder.initiatePathTo(0, 0, 3);

        solid(0, 0, 2);
        solid(0, 1, 2);

        path.update(pathingEntity);

        verify(pathingEntity).moveTo(new Vec3d(0.5, 0, 3.5), Passibility.passible, Gravitation.grounded);
        pos(0.5, 0, 1.5);

        IPath path2 = pathFinder.update(pathingEntity);

        assertSame(path, path2);

        when(pathingEntity.age()).thenReturn(100);

        path2 = pathFinder.update(pathingEntity);

        assertNotSame(path, path2);
        path = path2;

        path.update(pathingEntity);
        when(pathingEntity.age()).thenReturn(200);

        path2 = pathFinder.update(pathingEntity);
        assertTrue(path.sameAs(path2));
        path2.update(pathingEntity);

        assertTrue(path2.taxiing());
    }

    @Test
    public void bufferUnderrun() {
        defaultGround();
        pos(1.5, 0, 3.5);
        pathFinder.schedulingPriority(SchedulingPriority.low);

        for (int x = -5; x <= +5; ++x) {
            solid(x, 0, 5);
            solid(x, 1, 5);
        }

        IPath path;

        path = pathFinder.initiatePathTo(1, 0, 7);
        int iterations = 0,
                incompletes = 0;
        for (int i = 0; i < 1000 && PathObject.active(path = pathFinder.updatePathFor(this.pathingEntity)); ++i, iterations++) {
            if (path instanceof IncompletePath)
                incompletes++;

            final Vec3i coords = path.current().coordinates();
            pos(coords.x + 0.5, coords.y, coords.z + 0.5);
        }

        assertEquals(65, iterations);
        assertEquals(5, incompletes);

        verify(pathingEntity).moveTo(new Vec3d(1.5, 0, 7.5), Passibility.passible, Gravitation.grounded);
    }
}
