package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.Node;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FatGroundPassibilityCalculatorTests extends AbstractGroundPassibilityCalculatorTests {
    public void setup(IPathingEntity pathingEntity) {
        super.setup(pathingEntity);
        when(pathingEntity.width()).thenReturn(1.4f);
    }

    @Test
    public void stepUpEast() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(2, 0, 0);

        final Node actual = calculator.passiblePointNear(new Vec3i(1, 0, 0), ORIGIN, new FlagSampler(super.occlusionProvider));
        assertNotNull(actual);
        assertEquals(0, actual.key.y);
    }

    @Test
    public void stepUpWest() {
        solid(0, -1, 0);
        solid(-1, -1, 0);
        solid(-2, 0, 0);

        final Node actual = calculator.passiblePointNear(new Vec3i(-1, 0, 0), ORIGIN, new FlagSampler(super.occlusionProvider));
        assertNotNull(actual);
        assertEquals(1, actual.key.y);
    }

    @Test
    public void stepUpSouth() {
        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(0, 0, 2);

        final Node actual = calculator.passiblePointNear(new Vec3i(0, 0, 1), ORIGIN, new FlagSampler(super.occlusionProvider));
        assertNotNull(actual);
        assertEquals(0, actual.key.y);
    }

    @Test
    public void stepUpNorth() {
        solid(0, -1, 0);
        solid(0, -1, -1);
        solid(0, 0, -2);

        final Node actual = calculator.passiblePointNear(new Vec3i(0, 0, -1), ORIGIN, new FlagSampler(super.occlusionProvider));
        assertNotNull(actual);
        assertEquals(1, actual.key.y);
    }
}
