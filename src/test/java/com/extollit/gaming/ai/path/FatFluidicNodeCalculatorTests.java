package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import com.extollit.gaming.ai.path.model.Node;
import com.extollit.gaming.ai.path.model.Passibility;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class FatFluidicNodeCalculatorTests extends AbstractHydrazinePathFinderTests {
    private FluidicNodeCalculator calculator;
    private FlagSampler flagSampler;

    @Before
    public void setup(){
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        when(capabilities.avian()).thenReturn(true);
        this.calculator = new FluidicNodeCalculator(super.instanceSpace);

        when(super.pathingEntity.width()).thenReturn(1.4f);
        when(super.pathingEntity.height()).thenReturn(1.1f);

        this.calculator.applySubject(super.pathingEntity);
    }

    @Test
    public void impassible() {
        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(1, -1, 0);
        solid(1, -1, 1);

        solid(0, 0, 1);

        final Node actual = calculator.passibleNodeNear(1, 0, 1, ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.impassible, actual.passibility());
        assertEquals(1, actual.key.z);
    }
}
