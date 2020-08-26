package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class BuoyantPassibilityCalculatorTests extends AbstractPassibilityCalculatorTests {

    @Override
    public void setup() {
        when(super.capabilities.gilled()).thenReturn(true);
        when(super.capabilities.swimmer()).thenReturn(true);

        super.setup();
    }

    @Override
    protected FluidicPassibilityCalculator createCalculator(IInstanceSpace instanceSpace) {
        return new FluidicPassibilityCalculator(instanceSpace, Element.water);
    }

    @Test
    public void upWeGo() {
        solid(0, -1, 0);
        water(0, 0, 0);
        water(0, 1, 0);
        water(0, 2, 0);

        final Node actual = calculator.passiblePointNear(new Vec3i(0, 1, 0), ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.passible, actual.passibility());
        assertEquals(1, actual.key.y);
    }

    @Test
    public void gilledSuffocation() {
        solid(0, -1, 0);
        water(0, 0, 0);
        water(0, 1, 0);

        final Node actual = calculator.passiblePointNear(new Vec3i(0, 1, 0), ORIGIN, this.flagSampler);
        assertEquals(Passibility.impassible, actual.passibility());
        assertEquals(1, actual.key.y);
    }

    @Test
    public void impassibleNeighbor() {
        solid(0, -1, 0);
        solid(0, 0, 1);

        final Node actual = calculator.passiblePointNear(new Vec3i(0, 0, 1), ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.impassible, actual.passibility());
        assertEquals(1, actual.key.z);
    }

    @Test
    public void gravitationAll() {
        solid(0, -1, 1);
        water(0, -1, 2);

        final Node grounded = calculator.passiblePointNear(new Vec3i(0, 0, 1), ORIGIN, this.flagSampler);
        final Node buoyant = calculator.passiblePointNear(new Vec3i(0, -1, 2), new Vec3i(0, 0, 2), this.flagSampler);
        final Node airborne = calculator.passiblePointNear(new Vec3i(0, 0, 3), new Vec3i(0, 0, 2), this.flagSampler);

        assertEquals(Gravitation.grounded, grounded.gravitation());
        assertEquals(Gravitation.buoyant, buoyant.gravitation());
        assertEquals(Gravitation.airborne, airborne.gravitation());
    }

    @Test
    public void buoyantButGrounded() {
        when(capabilities.swimmer()).thenReturn(true);
        when(capabilities.gilled()).thenReturn(true);
        when(capabilities.aquaphobic()).thenReturn(false);

        solid(0, -2, 0);
        water(0, -1, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(0, -1, 0), ORIGIN, this.flagSampler);

        assertEquals(Gravitation.grounded, node.gravitation());
    }

    @Test
    public void grounded() {
        solid(0, -2, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(0, -1, 0), ORIGIN, this.flagSampler);

        assertEquals(Gravitation.grounded, node.gravitation());
    }

    @Test
    public void buoyantFull() {
        solid(0, -3, 0);
        water(0, -2, 0);
        water(0, -1, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(0, -1, 0), ORIGIN, this.flagSampler);

        assertEquals(Gravitation.buoyant, node.gravitation());
    }

    @Test
    public void buoyantFloating() {
        solid(0, -3, 0);
        water(0, -1, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(0, -1, 0), ORIGIN, this.flagSampler);

        assertEquals(Gravitation.buoyant, node.gravitation());
    }

    @Test
    public void airborne() {
        solid(0, -3, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(0, -1, 0), ORIGIN, this.flagSampler);

        assertEquals(Gravitation.airborne, node.gravitation());
    }

    @Test
    public void noBeaching() {
        when(super.capabilities.cautious()).thenReturn(false);

        water(0, -1, 0);
        solid(1, -1, 0);

        final Node node = calculator.passiblePointNear(new Vec3i(1, 0, 0), new Vec3i(0, -1, 0), this.flagSampler);
        assertEquals(Passibility.impassible, node.passibility());
    }
}
