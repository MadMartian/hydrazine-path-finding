package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Element;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.Logic;
import com.extollit.gaming.ai.path.model.Passibility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.PassibilityHelpers.passibilityFrom;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PassibilityHelpersTests {
    @Mock private IPathingEntity.Capabilities capabilities;

    @Test
    public void earth() {
        final Passibility result = passibilityFrom(Element.earth.mask, capabilities);

        assertEquals(Passibility.impassible, result);
    }

    @Test
    public void opensDoors() {
        when(capabilities.opensDoors()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.earth.to(Logic.doorway.mask), capabilities);

        assertEquals(Passibility.passible, result);
    }

    @Test
    public void ladders() {
        final Passibility result = passibilityFrom(Element.earth.to(Logic.ladder.mask), capabilities);

        assertEquals(Passibility.passible, result);
    }

    @Test
    public void air() {
        final Passibility result = passibilityFrom(Element.air.mask, capabilities);

        assertEquals(Passibility.passible, result);
    }

    @Test
    public void gilledAir() {
        when(capabilities.gilled()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.air.mask, capabilities);

        assertEquals(Passibility.dangerous, result);
    }

    @Test
    public void avoidsDoorways() {
        when(capabilities.avoidsDoorways()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.air.to(Logic.doorway.mask), capabilities);

        assertEquals(Passibility.impassible, result);
    }

    @Test
    public void hydroPhobia() {
        when(capabilities.aquaphobic()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.water.mask, capabilities);

        assertEquals(Passibility.dangerous, result);
    }

    @Test
    public void leadAnchor() {
        final Passibility result = passibilityFrom(Element.water.mask, capabilities);

        assertEquals(Passibility.risky, result);
    }

    @Test
    public void waterBreather() {
        when(capabilities.gilled()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.water.mask, capabilities);

        assertEquals(Passibility.risky, result);
    }

    @Test
    public void doggyPaddler() {
        when(capabilities.swimmer()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.water.mask, capabilities);

        assertEquals(Passibility.risky, result);
    }

    @Test
    public void fish() {
        when(capabilities.gilled()).thenReturn(true);
        when(capabilities.swimmer()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.water.mask, capabilities);

        assertEquals(Passibility.passible, result);
    }

    @Test
    public void fire() {
        final Passibility result = passibilityFrom(Element.fire.mask, capabilities);

        assertEquals(Passibility.dangerous, result);
    }

    @Test
    public void lavaMonster() {
        when(capabilities.fireResistant()).thenReturn(true);

        final Passibility result = passibilityFrom(Element.fire.mask, capabilities);

        assertEquals(Passibility.risky, result);
    }
}
