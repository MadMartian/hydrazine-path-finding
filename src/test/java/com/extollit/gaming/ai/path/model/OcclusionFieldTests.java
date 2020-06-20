package com.extollit.gaming.ai.path.model;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.TestingBlocks.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.leq;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OcclusionFieldTests extends AbstractOcclusionFieldTesting {
    @Test
    public void control() {
        occlusionField.loadFrom(centerSpace,  0, 0, 0);

        assertTrue(Element.air.in(occlusionField.elementAt(1, 2, 3)));
    }

    @Test
    public void point() {
        when(centerSpace.blockAt(anyInt(), leq(7), anyInt())).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);
        occlusionField.set(centerSpace, 4, 14, 8, stone);
        verifyNeighborhood(4, 14, 8,
                Element.earth,
                Element.air,
                Element.air,
                Element.air,
                Element.air
        );
    }

    @Test
    public void point2() {
        when(centerSpace.blockAt(anyInt(), lt(4), anyInt())).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);
        occlusionField.set(centerSpace, 15, 4, 3, lava);
        verifyNeighborhood(15, 4,3,
                Element.fire,
                Element.air,
                Element.air,
                Element.air,
                Element.air
        );
    }

    @Test
    public void xPlane() {
        when(centerSpace.blockAt(leq(7), anyInt(), anyInt())).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);

        for (int z = 0; z < 16; ++z)
            for (int y = 0; y < 16; ++y)
                for (int x = 0; x < 16; ++x) {
                    final byte element = occlusionField.elementAt(x, y, z);
                    if (x <= 7)
                        assertFalse(Element.air.in(element));
                    else
                        assertTrue(Element.air.in(element));
                }
    }


    @Test
    public void yPlane() {
        when(centerSpace.blockAt(anyInt(), leq(7), anyInt())).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);

        for (int z = 0; z < 16; ++z)
            for (int y = 0; y < 16; ++y)
                for (int x = 0; x < 16; ++x) {
                    final byte element = occlusionField.elementAt(x, y, z);
                    if (y <= 7)
                        assertFalse(Element.air.in(element));
                    else
                        assertTrue(Element.air.in(element));
                }
    }

    @Test
    public void zPlane() {
        when(centerSpace.blockAt(anyInt(), anyInt(), leq(7))).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);

        for (int z = 0; z < 16; ++z)
            for (int y = 0; y < 16; ++y)
                for (int x = 0; x < 16; ++x) {
                    final byte element = occlusionField.elementAt(x, y, z);
                    if (z <= 7)
                        assertFalse(Element.air.in(element));
                    else
                        assertTrue(Element.air.in(element));
                }
    }

    @Test
    public void lava() {
        when(centerSpace.blockAt(0, 0, 0)).thenReturn(stone);
        occlusionField.loadFrom(centerSpace,  0, 0, 0);
        occlusionField.set(centerSpace, 3, 9, 2, lava);
        final byte element = this.occlusionField.elementAt(3, 9, 2);
        assertTrue(Element.fire.in(element));
    }

    @Test
    public void wall() {
        blockAt(5, 5, 5, wall);

        occlusionField.loadFrom(centerSpace, 0, 0, 0);
        final byte [] wall = {
                occlusionField.elementAt(5, 5, 5),
                occlusionField.elementAt(5, 6, 5),
        };
        assertTrue(Element.earth.in(wall[0]));
        assertTrue(Element.earth.in(wall[1]));
        assertTrue(Logic.fuzzy.in(wall[0]));
        assertTrue(Logic.fuzzy.in(wall[1]));
        assertFalse(Element.earth.in(occlusionField.elementAt(5, 4, 5)));
        assertFalse(Element.earth.in(occlusionField.elementAt(5, 7, 5)));
        for (int y = 5; y <= 6; ++y) {
            assertFalse(Element.earth.in(occlusionField.elementAt(5 + 1, y, 5)));
            assertFalse(Element.earth.in(occlusionField.elementAt(5 - 1, y, 5)));
            assertFalse(Element.earth.in(occlusionField.elementAt(5, y, 5 + 1)));
            assertFalse(Element.earth.in(occlusionField.elementAt(5, y, 5 - 1)));
        }
    }

    @Test
    public void placeFenceGate() {
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        fenceGate(false, 5, 5, 5);
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));
    }

    @Test
    public void removeFenceGate() {
        fenceGate(false, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);
        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));

        when(centerSpace.blockAt(5, 5, 5)).thenReturn(air);
        occlusionField.set(centerSpace, 5, 5, 5, air);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(bottom));
        assertTrue(Element.air.in(top));
        assertTrue(Logic.nothing.in(bottom));
        assertTrue(Logic.nothing.in(top));
    }

    @Test
    public void openFenceGate() {
        fenceGate(false, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));

        fenceGate(true, 5, 5, 5);
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(bottom));
        assertTrue(Element.air.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));
    }

    @Test
    public void closeFenceGate() {
        fenceGate(true, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(bottom));
        assertTrue(Element.air.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));

        fenceGate.open = false;
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));
    }

    @Test
    public void openCappedFenceGate() {
        fenceGate(false, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(stone);
        occlusionField.set(centerSpace, 5, 6, 5, stone);

        assertTrue(Element.earth.in(occlusionField.elementAt(5, 6, 5)));

        fenceGate.open = true;
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.nothing.in(top));
    }

    @Test
    public void closeCappedFenceGate() {
        fenceGate(true, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(stone);
        occlusionField.set(centerSpace, 5, 6, 5, stone);

        assertTrue(Element.earth.in(occlusionField.elementAt(5, 6, 5)));

        fenceGate.open = false;
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.nothing.in(top));
    }

    @Test
    public void removeClosedCappedFenceGate() {
        fenceGate(false, 5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(stone);
        occlusionField.set(centerSpace, 5, 6, 5, stone);

        when(centerSpace.blockAt(5, 5, 5)).thenReturn(air);
        occlusionField.set(centerSpace, 5, 5, 5, air);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.nothing.in(bottom));
        assertTrue(Logic.nothing.in(top));
    }

    @Test
    public void placeClosedCappedFenceGate() {
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(stone);
        occlusionField.set(centerSpace, 5, 6, 5, stone);

        fenceGate(false, 5, 5, 5);
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.nothing.in(top));
    }

    @Test
    public void fenceGateToWall() {
        fenceGate(true,5, 5, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        when(centerSpace.blockAt(5, 5, 5)).thenReturn(wall);
        occlusionField.set(centerSpace, 5, 5, 5, wall);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.fuzzy.in(bottom));
        assertTrue(Logic.fuzzy.in(top));
    }

    @Test
    public void wallToFenceGate() {
        blockAt(5, 5, 5, wall);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        fenceGate(false, 5, 5, 5);
        occlusionField.set(centerSpace, 5, 5, 5, fenceGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
        assertTrue(Logic.doorway.in(bottom));
        assertTrue(Logic.doorway.in(top));
    }

    @Test
    public void torchUp() {
        blockAt(5, 5, 5, wall);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));

        occlusionField.set(centerSpace, 5, 6, 5, torch);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
    }

    @Test
    public void torchDown() {
        blockAt(5, 5, 5, wall);
        blockAt(5, 6, 5, torch);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));

        occlusionField.set(centerSpace, 5, 6, 5, air);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(bottom));
        assertTrue(Element.earth.in(top));
    }

    @Test
    @Ignore("Insufficient information in the occlusion field to determine this reliably, will need to refactor.  Currently this means that two fence gates, the top one closed, will be considered as if both are open.  This is an acceptable trade-off.")
    public void stackedFenceGates() {
        fenceGate.open = false;

        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        final FenceGate
            topGate = new FenceGate(),
            bottomGate = new FenceGate();

        bottomGate.open = true;
        topGate.open = false;

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(fenceGate);
        when(centerSpace.blockAt(5, 5, 5)).thenReturn(fenceGate);
        when(instanceSpace.blockObjectAt(5, 6, 5)).thenReturn(topGate);
        when(instanceSpace.blockObjectAt(5, 5, 5)).thenReturn(bottomGate);

        occlusionField.set(centerSpace, 5, 6, 5, topGate);
        occlusionField.set(centerSpace, 5, 5, 5, bottomGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(top));
        assertFalse(Element.earth.in(bottom));

        topGate.open = true;
        occlusionField.set(centerSpace, 5, 6, 5, topGate);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertFalse(Element.earth.in(top));
        assertFalse(Element.earth.in(bottom));
    }

    @Test
    public void invertedStackedFenceGates() {
        fenceGate.open = false;

        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        final FenceGate
                topGate = new FenceGate(),
                bottomGate = new FenceGate();

        bottomGate.open = false;
        topGate.open = true;

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(fenceGate);
        when(centerSpace.blockAt(5, 5, 5)).thenReturn(fenceGate);
        when(instanceSpace.blockObjectAt(5, 6, 5)).thenReturn(topGate);
        when(instanceSpace.blockObjectAt(5, 5, 5)).thenReturn(bottomGate);

        occlusionField.set(centerSpace, 5, 6, 5, topGate);
        occlusionField.set(centerSpace, 5, 5, 5, bottomGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(top));
        assertTrue(Element.earth.in(bottom));

        topGate.open = false;
        occlusionField.set(centerSpace, 5, 6, 5, topGate);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(top));
        assertTrue(Element.earth.in(bottom));
    }


    @Test
    public void stackedFenceGateTopOpen() {
        fenceGate(false, 5, 5, 5);
        fenceGate(false, 5, 6, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        final FenceGate
                topGate = new FenceGate(),
                bottomGate = new FenceGate();

        bottomGate.open = false;
        topGate.open = true;

        fenceGate.open = false;

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(fenceGate);
        when(centerSpace.blockAt(5, 5, 5)).thenReturn(fenceGate);
        when(instanceSpace.blockObjectAt(5, 6, 5)).thenReturn(topGate);
        when(instanceSpace.blockObjectAt(5, 5, 5)).thenReturn(bottomGate);

        occlusionField.set(centerSpace, 5, 6, 5, topGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(top));
        assertTrue(Element.earth.in(bottom));

        topGate.open = false;
        occlusionField.set(centerSpace, 5, 6, 5, topGate);

        top = occlusionField.elementAt(5, 6, 5);
        bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.earth.in(top));
        assertTrue(Element.earth.in(bottom));
    }

    @Test
    public void stackedFenceGateOpen() {
        fenceGate(false, 5, 5, 5);
        fenceGate(false, 5, 6, 5);
        occlusionField.loadFrom(centerSpace, 0, 0, 0);

        fenceGate.open = false;

        final FenceGate
                topGate = new FenceGate(),
                bottomGate = new FenceGate();

        bottomGate.open = true;
        topGate.open = true;

        when(centerSpace.blockAt(5, 6, 5)).thenReturn(fenceGate);
        when(centerSpace.blockAt(5, 5, 5)).thenReturn(fenceGate);
        when(instanceSpace.blockObjectAt(5, 6, 5)).thenReturn(topGate);
        when(instanceSpace.blockObjectAt(5, 5, 5)).thenReturn(bottomGate);

        occlusionField.set(centerSpace, 5, 6, 5, topGate);
        occlusionField.set(centerSpace, 5, 5, 5, topGate);

        byte
                top = occlusionField.elementAt(5, 6, 5),
                bottom = occlusionField.elementAt(5, 5, 5);

        assertTrue(Element.air.in(top));
        assertTrue(Element.air.in(bottom));
    }
}
