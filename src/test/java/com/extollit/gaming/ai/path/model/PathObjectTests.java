package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;
import com.extollit.linalg.immutable.Vec3d;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PathObjectTests {
    private final PathObject
        pathAlpha = new PathObject(
            1,
            new Vec3i(-10, 42, -10),
            new Vec3i(-10, 42, -9),
            new Vec3i(-10, 41, -8),
            new Vec3i(-10, 41, -7),
            new Vec3i(-10, 41, -6),
            new Vec3i(-10, 41, -5),
            new Vec3i(-10, 40, -4),
            new Vec3i(-10, 40, -3),
            new Vec3i(-10, 39, -2),
            new Vec3i(-10, 39, -1),
            new Vec3i(-10, 39, 0),
            new Vec3i(-10, 38, 1),
            new Vec3i(-10, 38, 2),
            new Vec3i(-10, 37, 3),
            new Vec3i(-9, 37, 3),
            new Vec3i(-8, 37, 3),
            new Vec3i(-8, 37, 4),
            new Vec3i(-8, 36, 5)
        ),
        pathBeta = new PathObject(
            1,
            new Vec3i(-10, 42, -10),
            new Vec3i(-10, 42, -9),
            new Vec3i(-10, 41, -8),
            new Vec3i(-10, 41, -7),
            new Vec3i(-10, 41, -6),
            new Vec3i(-10, 41, -5),
            new Vec3i(-10, 40, -4),
            new Vec3i(-9, 40, -4),
            new Vec3i(-8, 40, -4),
            new Vec3i(-8, 40, -3),
            new Vec3i(-8, 39, -2),
            new Vec3i(-7, 39, -2),
            new Vec3i(-7, 39, -1),
            new Vec3i(-6, 39, -1),
            new Vec3i(-6, 38, 0),
            new Vec3i(-6, 38, 1),
            new Vec3i(-6, 37, 2),
            new Vec3i(-6, 37, 3)
        );

    @Mock private IPathingEntity pathingEntity;

    @Before
    public void setup() {
        when(pathingEntity.width()).thenReturn(0.6f);
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(0.5, 0, 0.5));

        pathAlpha.i = pathBeta.i = 0;
    }

    @Test
    public void updateMutationState() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1)
        );
        when(pathingEntity.age()).thenReturn(42);
        assertEquals(0, pathObject.stagnantFor(pathingEntity), 0.01);

        pathObject.update(pathingEntity);
        when(pathingEntity.age()).thenReturn(94);
        assertEquals(94 - 42, pathObject.stagnantFor(pathingEntity), 0.01);

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(2.5, 0.2, 1.5));

        pathObject.update(pathingEntity);

        assertEquals(0, pathObject.stagnantFor(pathingEntity), 0.01);
    }

    @Test
    public void update() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 1, 1),
                new Vec3i(4, 1, 2),
                new Vec3i(5, 1, 2),
                new Vec3i(4, 1, 2),
                new Vec3i(3, 1, 3)
        );
        pathObject.update(pathingEntity);

        assertEquals(4, pathObject.i);
    }

    @Test
    public void updateLateStage() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 2),
                new Vec3i(6, 0, 2),
                new Vec3i(6, 0, 3)
        );
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(4.5, 0, 1.5));

        pathObject.update(pathingEntity);

        assertEquals(6, pathObject.i);
    }

    @Test
    public void waterStuck() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(6, 4, 7),
                new Vec3i(7, 4, 7),
                new Vec3i(8, 4, 7),
                new Vec3i(9, 4, 7)
        );
        when(pathingEntity.width()).thenReturn(0.3f);
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(6.5, 4.1, 7.5));

        pathObject.update(pathingEntity);

        assertEquals(3, pathObject.i);
    }

    @Test
    public void truncation() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(2, 5, 3),
                new Vec3i(7, 8, 2),
                new Vec3i(9, 2, 6),
                new Vec3i(5, 7, 9),
                new Vec3i(1, 5, 3)
        );

        pathObject.truncateTo(3);

        assertEquals(3, pathObject.length());
        assertEquals(
            Arrays.asList(
                new Vec3i(2, 5, 3),
                new Vec3i(7, 8, 2),
                new Vec3i(9, 2, 6)
            ),
            CollectionsExt.toList(pathObject)
        );
    }

    @Test
    public void untruncation() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(2, 5, 3),
                new Vec3i(7, 8, 2),
                new Vec3i(9, 2, 6),
                new Vec3i(5, 7, 9),
                new Vec3i(1, 5, 3)
        );

        pathObject.truncateTo(3);
        pathObject.untruncate();

        assertEquals(5, pathObject.length());
        assertEquals(
                Arrays.asList(pathObject.points),
                CollectionsExt.toList(pathObject)
        );
    }

    @Test
    public void positionFor1() {
        when(pathingEntity.width()).thenReturn(0.8f);

        final Vec3d pos = PathObject.positionFor(pathingEntity, new Vec3i(1, 2, 3));

        assertEquals(new Vec3d(1.5, 2, 3.5), pos);
    }


    @Test
    public void positionFor2() {
        when(pathingEntity.width()).thenReturn(1.4f);

        final Vec3d pos = PathObject.positionFor(pathingEntity, new Vec3i(1, 2, 3));

        assertEquals(new Vec3d(1, 2, 3), pos);
    }

    @Test
    public void positionFor3() {
        when(pathingEntity.width()).thenReturn(2.3f);

        final Vec3d pos = PathObject.positionFor(pathingEntity, new Vec3i(1, 2, 3));

        assertEquals(new Vec3d(1.5, 2, 3.5), pos);
    }

    @Test
    public void dontAdvanceBigBoiTooMuch() {
        when(pathingEntity.width()).thenReturn(1.4f);
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-0.45f, 0, 0));
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(-1, 0, 1),
                new Vec3i(0, 1, 1)
        );

        pathObject.update(pathingEntity);

        assertEquals(2, pathObject.i);
    }

    @Test
    public void dontDoubleBack() {
        PathObject pathObject = new PathObject(
                1,
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(3, 0, 0),
                new Vec3i(4, 0, 0),
                new Vec3i(5, 0, 0)
        );
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(3.5, 0 ,0.5));
        pathObject.update(pathingEntity);

        assertEquals(4, pathObject.i);
        verify(pathingEntity).moveTo(new Vec3d(5.5, 0, 0.5));

        when(pathingEntity.age()).thenReturn(100);
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(3.5, 0 ,1.5));

        pathObject.update(pathingEntity);

        assertEquals(2, pathObject.i);
        verify(pathingEntity).moveTo(new Vec3d(3.5, 0, 0.5));
    }


    @Test
    public void nonRepudiantUpdate() {
        PathObject path = new PathObject(
                1,
                new Vec3i(-2, 4, 11),
                new Vec3i(-3, 4, 11),
                new Vec3i(-4, 4, 11),
                new Vec3i(-5, 4, 11),
                new Vec3i(-5, 4, 10),
                new Vec3i(-4, 4, 10),
                new Vec3i(-3, 4, 10),
                new Vec3i(-2, 4, 10),
                new Vec3i(-1, 4, 10),
                new Vec3i(0, 4, 10)
        );

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-1.5, 4, 11.5));
        path.update(pathingEntity);
        final int first = path.i;

        path.update(pathingEntity);
        assertEquals(first, path.i);
    }

    @Test
    public void approximateAdjacent() {
        PathObject path = new PathObject(
                1,
                new Vec3i(0, 1, 0),
                new Vec3i(1, 1, 0)
        );
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(0.4, 0.5, 0.4));

        path.update(pathingEntity);
        assertEquals(1, path.i);
    }

    @Test
    public void stairMaster() {
        PathObject path = new PathObject(
                1,
                new Vec3i(13, 4, 6),
                new Vec3i(12, 5, 6),
                new Vec3i(11, 5, 6),
                new Vec3i(11, 4, 7),
                new Vec3i(10, 4, 7)
        );
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(13.5, 4, 6.5));
        path.update(pathingEntity);

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(11.4, 5, 7.3));
        path.update(pathingEntity);

        assertEquals(4, path.i);
    }

    @Test
    public void disparatePathAdjustment() {
        pathAlpha.i = 11;

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-9.5, 38.0, 1.5));
        pathBeta.adjustPathPosition(pathAlpha, pathingEntity);

        assertEquals(10, pathBeta.i);
    }

    @Test
    public void unreachablePath() {
        pathAlpha.i = 11;
        assertFalse(pathBeta.reachableFrom(pathAlpha));
    }
}
