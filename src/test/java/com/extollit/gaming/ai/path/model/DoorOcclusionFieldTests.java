package com.extollit.gaming.ai.path.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.TestingBlocks.door;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DoorOcclusionFieldTests extends AbstractOcclusionFieldTesting {
    @Test
    public void offset() {
        final int
                x = 208, y = 64, z = -1389,
                dx = x & 0xF, dy = y & 0xF, dz = z & 0xF;

        door(true, x, y, z);

        occlusionField.set(centerSpace, x, y, z, door);
        final byte element = occlusionField.elementAt(dx, dy, dz);

        verify(instanceSpace, atLeastOnce()).optOcclusionFieldAt(12, 4, -87);

        assertTrue(Logic.doorway.in(element));
    }

    @Test
    public void initOpen() {
        door(true, 4, 5, 6);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        assertDoorway(true, 4, 5, 6);
    }

    @Test
    public void initClosed() {
        door(false, 4, 5, 6);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        assertDoorway(false, 4, 5, 6);
    }

    @Test
    public void opened() {
        door(false, 4, 5, 6);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);
        door(true, 4, 5, 6);

        occlusionField.set(centerSpace, 4, 5, 6, door);
        assertDoorway(true, 4, 5, 6);
    }

    private void assertDoorway(boolean open, final int x, final int y, final int z) {
        final OcclusionField occlusionField = this.occlusionField;
        assertDoorway(occlusionField, open, x, y, z);
    }
}
