package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3d;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTests {
    @Mock
    private IPathingEntity pathingEntity;

    @Before
    public void setup() {
        when(pathingEntity.width()).thenReturn(0.6f);
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(0.5, 0, 0.5));
    }

    @Test
    public void jackknife() {
        PathObject path = new PathObject(
                1,
                new Vec3i(-1, 4, 10),
                new Vec3i(-2, 4, 11),
                new Vec3i(-3, 4, 11),
                new Vec3i(-4, 4, 11)
        );
        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-0.5, 4, 10.5));

        path.update(pathingEntity);
        assertEquals(1, path.i);

        verify(pathingEntity).moveTo(new Vec3d(-1.5, 4, 11.5));

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-1.5, 4, 11.5));

        path = new PathObject(
                1,
                new Vec3i(-1, 4, 10),
                new Vec3i(-2, 4, 11),
                new Vec3i(-3, 4, 11),
                new Vec3i(-4, 4, 11),
                new Vec3i(-4, 4, 10),
                new Vec3i(-4, 4, 9),
                new Vec3i(-3, 4, 9),
                new Vec3i(-2, 4, 9),
                new Vec3i(-1, 4, 9),
                new Vec3i(0, 4, 9),
                new Vec3i(1, 4, 9)
        );

        path.update(pathingEntity);
        assertEquals(3, path.i);

        verify(pathingEntity).moveTo(new Vec3d(-3.5, 4, 11.5));

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-3.5, 4, 11.5));
        path.update(pathingEntity);
        assertEquals(5, path.i);

        verify(pathingEntity).moveTo(new Vec3d(-3.5, 4, 9.5));

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(-3.5, 4, 9.5));
        path.update(pathingEntity);
        assertEquals(10, path.i);
    }

    @Test
    public void taxi() {
        PathObject path = new PathObject(
                1,
                new Vec3i(0, 4, 2),
                new Vec3i(0, 4, 3),
                new Vec3i(1, 4, 3),
                new Vec3i(1, 4, 4),
                new Vec3i(2, 4, 4)
        );

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(0.4, 4.5, 2.4));

        path.taxiUntil(2);

        path.update(pathingEntity);
        assertEquals(1, path.i);

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(0.4, 4.5, 3.4));
        path.update(pathingEntity);
        assertEquals(2, path.i);

        when(pathingEntity.coordinates()).thenReturn(new Vec3d(1.4, 4.5, 3.4));
        path.update(pathingEntity);
        assertEquals(4, path.i);
    }
}
