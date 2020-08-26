package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Element;
import com.extollit.gaming.ai.path.model.FlagSampler;
import com.extollit.gaming.ai.path.model.Node;
import com.extollit.gaming.ai.path.model.Passibility;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class FatFluidicPassibilityCalculatorTests extends AbstractHydrazinePathFinderTests {
    private FluidicPassibilityCalculator calculator;
    private FlagSampler flagSampler;

    @Before
    public void setup(){
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        this.calculator = new FluidicPassibilityCalculator(super.instanceSpace, Element.air);

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

        final Node actual = calculator.passiblePointNear(new Vec3i(1, 0, 1), ORIGIN, this.flagSampler);
        assertNotNull(actual);
        assertEquals(Passibility.impassible, actual.passibility());
        assertEquals(1, actual.key.z);
    }
}
