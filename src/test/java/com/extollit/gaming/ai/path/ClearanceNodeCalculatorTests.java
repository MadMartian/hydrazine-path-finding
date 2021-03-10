package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.INodeCalculator;
import com.extollit.gaming.ai.path.model.Node;
import com.extollit.gaming.ai.path.model.Passibility;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClearanceNodeCalculatorTests extends AbstractNodeCalculatorTests {
    @Override
    @Before
    public void setup() {
        super.setup();

        cautious(false);
        super.calculator.applySubject(super.pathingEntity);
    }

    @Override
    protected INodeCalculator createCalculator(IInstanceSpace instanceSpace) {
        return new GroundNodeCalculator(instanceSpace);
    }

    @Test
    public void sourceSlabCeilingNope() {
        solid(0, -1, 0);
        slabUp(0, 2, 0);
        solid(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void sourceSlabCeilingYep() {
        solid(0, -1, 0);
        slabUp(0, 2, 0);
        slabDown(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void sourceSlabCeilingControl() {
        solid(0, -1, 0);
        solid(0, 3, 0);
        solid(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void sourceBurningHead() {
        solid(0, -1, 0);
        lava(0, 2, 0);
        solid(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.dangerous, actual.passibility());
    }

    @Test
    public void targetSlabCeilingNope() {
        solid(0, 0, 0);
        solid(0, 0, 1);
        solid(0, 1, 1);
        slabUp(0, 3, 0);
        slabUp(0, 3, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void targetSlabCeilingYep() {
        slabDown(0, 0, 0);
        solid(0, 0, 1);
        slabDown(0, 1, 1);
        slabUp(0, 3, 0);
        slabUp(0, 3, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void targetSlabCeilingControl() {
        solid(0, 0, 0);
        solid(0, 0, 1);
        solid(0, 1, 1);
        solid(0, 4, 0);
        solid(0, 4, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void targetBurningHead() {
        solid(0, 0, 0);
        solid(0, 0, 1);
        solid(0, 1, 1);
        solid(0, 4, 0);
        solid(0, 4, 1);
        lava(0, 3, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.dangerous, actual.passibility());
    }

    @Test
    public void stuckDownHereWithYou() {
        solid(0, 0, 0);
        solid(0, 0, 1);
        solid(0, 1, 1);
        fuzzy(0, 3, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void slabAcross() {
        slabDown(0, -1, 0);
        slabDown(0, -1, 1);
        slabUp(0, 1, 0);
        slabUp(0, 1, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void targetNarrowNope() {
        solid(0, -1, 0);
        slabDown(0, 0, 1);
        solid(0, 2, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void targetNarrowYep() {
        solid(0, -1, 0);
        slabDown(0, 0, 1);
        slabUp(0, 2, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void sourceNarrowNope() {
        slabDown(0, 0, 0);
        solid(0, -1, 1);
        solid(0, 2, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void sourceNarrowYep() {
        slabDown(0, 0, 0);
        solid(0, -1, 1);
        slabUp(0, 2, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
    }
}
