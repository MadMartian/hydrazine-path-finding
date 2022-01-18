package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class AirborneNodeCalculatorTests extends AbstractNodeCalculatorTests {
    @Before
    public void setup() {
        when(super.capabilities.avian()).thenReturn(true);

        super.setup();
    }

    @Override
    protected FluidicNodeCalculator createCalculator(IInstanceSpace instanceSpace) {
        return new FluidicNodeCalculator(instanceSpace);
    }

    @Test
    public void upWeGo() {
        solid(0, -1, 0);

        final Node actual = calculator.passibleNodeNear(0, 1, 0, ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.passible, actual.passibility());
        assertEquals(1, actual.key.y);
    }

    @Test
    public void impassibleNeighbor() {
        solid(0, -1, 0);
        solid(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(0, 0, 1, ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.impassible, actual.passibility());
        assertEquals(1, actual.key.z);
    }

    @Test
    public void gravitationAll() {
        solid(0, -1, 1);
        water(0, -1, 2);

        final Node grounded = calculator.passibleNodeNear(0, 0, 1, ORIGIN, this.flagSampler);
        final Node buoyant = calculator.passibleNodeNear(0, -1, 2, new Coords(0, 0, 2), this.flagSampler);
        final Node airborne = calculator.passibleNodeNear(0, 0, 3, new Coords(0, 0, 2), this.flagSampler);

        assertEquals(Gravitation.grounded, grounded.gravitation());
        assertEquals(Gravitation.buoyant, buoyant.gravitation());
        assertEquals(Gravitation.airborne, airborne.gravitation());
    }

    @Test
    public void grounded() {
        solid(0, -2, 0);

        final Node node = calculator.passibleNodeNear(0, -1, 0, ORIGIN, this.flagSampler);

        assertEquals(Gravitation.grounded, node.gravitation());
    }

    @Test
    public void buoyantFull() {
        solid(0, -3, 0);
        water(0, -2, 0);
        water(0, -1, 0);

        final Node node = calculator.passibleNodeNear(0, -1, 0, ORIGIN, this.flagSampler);

        assertEquals(Gravitation.buoyant, node.gravitation());
    }

    @Test
    public void buoyantFloating() {
        solid(0, -3, 0);
        water(0, -1, 0);

        final Node node = calculator.passibleNodeNear(0, -1, 0, ORIGIN, this.flagSampler);

        assertEquals(Gravitation.buoyant, node.gravitation());
    }

    @Test
    public void airborne() {
        solid(0, -3, 0);

        final Node node = calculator.passibleNodeNear(0, -1, 0, ORIGIN, this.flagSampler);

        assertEquals(Gravitation.airborne, node.gravitation());
    }
}
