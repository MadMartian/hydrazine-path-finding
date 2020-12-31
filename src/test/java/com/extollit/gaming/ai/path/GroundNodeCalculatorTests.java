package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Element;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.Node;
import com.extollit.gaming.ai.path.model.Passibility;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GroundNodeCalculatorTests extends AbstractNodeCalculatorTests {
    @Override
    protected GroundNodeCalculator createCalculator(final IInstanceSpace instanceSpace) {
        return new GroundNodeCalculator(instanceSpace);
    }

    @Test
    public void stepUp() {
        solid(0, -1, 0);
        solid(1, 0, 0);
        solid(1, 3, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.passible, actual.passibility());
        assertEquals(1, actual.key.y);
    }

    @Test
    public void noStepUp() {
        solid(0, -1, 0);
        solid(1, -1, 0);
        solid(1, 0, 0);
        solid(1, 1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void narrowAtHead() {
        solid(1, 0, 0);
        solid(1, 2, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void narrowAtFeet() {
        clear(1, 0, 0);
        slabUp(1, 1, 0);
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void narrowAtFeetDown1() {
        solid(0, -1, 0);
        clear(1, 0, 0);
        slabUp(1, 1, 0);
        clear(1, -1, 0);
        solid(1, -2, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void barelyHeadSpace() {
        solid(0, -1, 0);
        clear(0, 0, 0);
        clear(0, 1, 0);
        slabDown(0, 0, 1);
        clear(0, 1, 1);
        slabUp(0, 2, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 0, 1), ORIGIN, super.flagSampler);
        assertEquals(1, actual.key.y);
    }

    @Test
    public void climbLadder() {
        solid(1, 0, 0);
        solid(1, 1, 0);
        solid(1, 2, 0);
        solid(1, 3, 0);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);
        climb(0, 3, 0);
        solid(0, -1, 0);
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(4, actual.key.y);
        assertEquals(1, actual.key.x);
    }

    @Test
    public void noClimbDiagonal() {
        solid(1, 0, 1);
        solid(1, 1, 1);
        solid(1, 2, 1);
        solid(1, 3, 1);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);
        climb(0, 3, 0);
        solid(0, -1, 0);
        solid(1, -1, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 1), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void diagonalFences() {
        solid(0, 0, 0);
        solid(1, 0, 0);
        solid(0, 0, 1);
        solid(1, 0, 1);
        longFence(0, 1, 0);
        longFence(1, 1, 1);
        pos(0, 1, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 1, 0), new Vec3i(0, 1, 1), super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(new Vec3i(1, 1, 0), actual.key);
    }

    @Test
    public void ladderOneUp() {
        solid(1, 0, 0);
        solid(1, 1, 0);
        solid(1, 2, 0);
        solid(1, 3, 0);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);
        solid(0, -1, 0);
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(4, actual.key.y);
        assertEquals(1, actual.key.x);
    }

    @Test
    public void ohSoHoppySlab() {
        slabDown(0, 0, 0);
        solid(0, 0, 1);
        solid(0, 1, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void noHoppySlab() {
        slabDown(0, 0, 0);
        solid(0, 0, 1);
        slabDown(0, 1, 1);

        final Node actual = calculator.passibleNodeNear(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(2, actual.key.y);
    }

    @Test
    public void ladderOneTooManyUp() {
        solid(1, 0, 0);
        solid(1, 1, 0);
        solid(1, 2, 0);
        solid(1, 3, 0);
        slabDown(1, 4, 0);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);
        solid(0, -1, 0);
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void ladderHalfUp() {
        solid(1, 0, 0);
        solid(1, 1, 0);
        solid(1, 2, 0);
        slabDown(1, 3, 0);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);
        solid(0, -1, 0);
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(4, actual.key.y);
        assertEquals(1, actual.key.x);
    }

    @Test
    public void slabUpDown() {
        cautious(false);

        slabDown(0, 0, 0);
        solid(0, -1, 0);
        solid(1, -2, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 1, 0), new Vec3i(0, 1, 0), super.flagSampler);

        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(Passibility.risky, actual.passibility());
        assertEquals(-1, actual.key.y);
    }

    @Test
    public void safeFall() {
        solid(1, -1, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(Passibility.passible, actual.passibility());
    }

    @Test
    public void riskyFall() {
        solid(1, -3, 0);

        cautious(false);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(Passibility.risky, actual.passibility());
    }

    @Test
    public void veryRiskyFall() {
        solid(1, -5, 0);

        cautious(false);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(Passibility.risky, actual.passibility());
    }

    @Test
    public void dangerousFall() {
        solid(1, -21, 0);

        cautious(false);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(Passibility.dangerous, actual.passibility());
    }

    @Test
    public void swimming() {
        solid(1, -3, 0);
        water(1, -2, 0);
        water(1, -1, 0);
        solid(0, -3, 0);
        water(0, -2, 0);
        water(0, -1, 0);

        diver();

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertTrue(actual.passibility().betterThan(Passibility.impassible));
        assertEquals(0, actual.key.y);
    }

    @Test
    public void drown() {
        for (int y = 20; y > -20; --y)
            for (int z = -1; z <= +1; ++z)
                for (int x = -1; x <= +1; ++x)
                    water(x, y, z);

        diver();

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.risky, actual.passibility());
        assertEquals(0, actual.key.y);
    }

    @Test
    public void cesa() {
        for (int y = 12; y > -20; --y)
            for (int z = -1; z <= 1; ++z)
                for (int x = -1; x <= 1; ++x)
                    water(x, y, z);

        diver();

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.risky, actual.passibility());
        assertEquals(13, actual.key.y);
    }

    @Test
    public void dive() {
        for (int y = -5; y > -10; --y)
            for (int z = -1; z <= 1; ++z)
                for (int x = -1; x <= 1; ++x)
                    water(x, y, z);

        diver();

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);
        assertEquals(Passibility.risky, actual.passibility());
        assertEquals(-4, actual.key.y);
    }

    @Test
    public void outPool() {
        when(capabilities.swimmer()).thenReturn(true);

        solid(0, -3, 0);
        water(0, -2, 0);
        water(0, -1, 0);
        clear(0, 0, 0);
        clear(0, 1, 0);
        clear(0, 2, 0);

        solid(1, -3, 0);
        solid(1, -2, 0);
        solid(1, -1, 0);
        solid(1, 0, 0);
        clear(1, 1, 0);
        clear(1, 2, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void climbOutPool() {
        when(capabilities.swimmer()).thenReturn(true);

        solid(0, -3, 0);
        water(0, -2, 0);
        water(0, -1, 0);
        climb(0, 0, 0);
        climb(0, 1, 0);
        climb(0, 2, 0);

        solid(1, -3, 0);
        solid(1, -2, 0);
        solid(1, -1, 0);
        solid(1, 0, 0);
        clear(1, 1, 0);
        clear(1, 2, 0);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
        assertEquals(new Vec3i(1, 1, 0), actual.key);
    }

    @Test
    public void clearanceForFireMonster() {
        when(capabilities.fireResistant()).thenReturn(true);

        final Passibility
                firePassibility = PassibilityHelpers.clearance(Element.fire.mask, super.capabilities),
                waterPassibility = PassibilityHelpers.clearance(Element.water.mask, super.capabilities);

        assertEquals(Passibility.risky, firePassibility);
        assertEquals(Passibility.dangerous, waterPassibility);
    }

    @Test
    public void clearanceForRegularMonster() {
        when(capabilities.fireResistant()).thenReturn(false);

        final Passibility
                firePassibility = PassibilityHelpers.clearance(Element.fire.mask, super.capabilities),
                waterPassibility = PassibilityHelpers.clearance(Element.water.mask, super.capabilities);

        assertEquals(Passibility.dangerous, firePassibility);
        assertEquals(Passibility.risky, waterPassibility);
    }

    @Test
    public void openClosedDoor() {
        when(capabilities.avoidsDoorways()).thenReturn(false);
        when(capabilities.opensDoors()).thenReturn(true);

        solid(1, -1, 0);
        door(1, 0, 0, false);
        door(1, 1, 0, false);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.passible, actual.passibility());
        assertEquals(0, actual.key.y);  // Regression: entities were jumping at 2-block-high doors
    }

    @Test
    public void avoidOpenDoor() {
        when(capabilities.avoidsDoorways()).thenReturn(true);

        solid(1, -1, 0);
        door(1, 0, 0, true);
        door(1, 1, 0, true);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }

    @Test
    public void avoidClosedDoor() {
        when(capabilities.avoidsDoorways()).thenReturn(true);
        when(capabilities.opensDoors()).thenReturn(true);

        solid(1, -1, 0);
        door(1, 0, 0, false);
        door(1, 1, 0, false);

        final Node actual = calculator.passibleNodeNear(new Vec3i(1, 0, 0), ORIGIN, super.flagSampler);

        assertEquals(Passibility.impassible, actual.passibility());
    }
}
