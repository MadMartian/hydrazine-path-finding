package com.extollit.gaming.ai.path.model;

import org.junit.Before;

import static com.extollit.gaming.ai.path.TestingBlocks.air;
import static com.extollit.gaming.ai.path.TestingBlocks.fenceGate;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class AbstractOcclusionFieldTesting extends AbstractOcclusionProviderTesting {
    protected OcclusionField occlusionField;

    @Before
    public void setup () {
        super.setup();

        this.occlusionField = new TestOcclusionField();
        when(centerSpace.blockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(centerSpace.optOcclusionFieldAt(anyInt())).thenReturn(occlusionField);
    }

    protected void verifyNeighborhood(final int x, final int y, final int z,
                                      final Element test,
                                      final Element westElem, final Element eastElem,
                                      final Element northElem, final Element southElem) {
        final byte element = this.occlusionField.elementAt(x, y, z);
        final byte
                west = this.occlusionField.elementAt(x - 1, y, z),
                east = this.occlusionField.elementAt(x + 1, y, z),
                north = this.occlusionField.elementAt(x, y, z - 1),
                south = this.occlusionField.elementAt(x, y, z + 1);

        assertTrue(test.in(element));
        assertTrue(westElem.in(west));
        assertTrue(eastElem.in(east));
        assertTrue(northElem.in(north));
        assertTrue(southElem.in(south));
    }

    protected void fenceGate(boolean open, final int x, final int y, final int z) {
        blockAt(x, y, z, fenceGate);
        fenceGate.open = open;
    }
}
