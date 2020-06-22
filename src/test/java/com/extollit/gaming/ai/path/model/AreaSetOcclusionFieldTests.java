package com.extollit.gaming.ai.path.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.TestingBlocks.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AreaSetOcclusionFieldTests extends AbstractAreaInitOcclusionFieldTesting {
    public AreaSetOcclusionFieldTests() {
        super(3);
    }

    @Test
    public void center () {
        set(5, 50, 5, lava);

        assertNeighborhood(5, 50, 5);
    }

    @Test
    public void west() {
        set(0, 50, 5, lava);

        verify(instanceSpace, times(7)).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(0, 50, 5);
    }

    @Test
    public void east() {
        set(15, 50, 5, lava);

        verify(instanceSpace, times(7)).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(15, 50, 5);
    }

    @Test
    public void north() {
        set(5, 50, 0, lava);

        verify(instanceSpace, times(7)).columnarSpaceAt(0, -1);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(5, 50, 0);
    }

    @Test
    public void south() {
        set(5, 50, 15, lava);

        verify(instanceSpace, times(7)).columnarSpaceAt(0, +1);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(5, 50, 15);
    }

    @Test
    public void northWest() {
        set(0, 50, 0, lava);

        verify(instanceSpace, times(2)).columnarSpaceAt(-1, -1);
        verify(instanceSpace, times(5)).columnarSpaceAt(0, -1);
        verify(instanceSpace, times(5)).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(0, 50, 0);
    }

    @Test
    public void northEast() {
        set(15, 50, 0, lava);

        verify(instanceSpace, times(2)).columnarSpaceAt(+1, -1);
        verify(instanceSpace, times(5)).columnarSpaceAt(0, -1);
        verify(instanceSpace, times(5)).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(15, 50, 0);
    }

    @Test
    public void southWest() {
        set(0, 50, 15, lava);

        verify(instanceSpace, times(2)).columnarSpaceAt(-1, +1);
        verify(instanceSpace, times(5)).columnarSpaceAt(0, +1);
        verify(instanceSpace, times(5)).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(0, 50, 15);
    }

    @Test
    public void southEast() {
        set(15, 50, 15, lava);

        verify(instanceSpace, times(2)).columnarSpaceAt(+1, +1);
        verify(instanceSpace, times(5)).columnarSpaceAt(0, +1);
        verify(instanceSpace, times(5)).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(13)).columnarSpaceAt(0, 0);

        assertNeighborhood(15, 50, 15);
    }

    @Test
    public void westInner() {
        set(1, 50, 5, lava);

        verify(instanceSpace).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(4)).columnarSpaceAt(0, 0);

        assertNeighborhood(1, 50, 5);
    }

    @Test
    public void eastInner() {
        set(14, 50, 5, lava);

        verify(instanceSpace).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(4)).columnarSpaceAt(0, 0);

        assertNeighborhood(14, 50, 5);
    }

    @Test
    public void northInner() {
        set(5, 50, 1, lava);

        verify(instanceSpace).columnarSpaceAt(0, -1);
        verify(instanceSpace, times(4)).columnarSpaceAt(0, 0);

        assertNeighborhood(5, 50, 1);
    }

    @Test
    public void southInner() {
        set(5, 50, 14, lava);

        verify(instanceSpace).columnarSpaceAt(0, +1);
        verify(instanceSpace, times(4)).columnarSpaceAt(0, 0);

        assertNeighborhood(5, 50, 14);
    }

    @Test
    public void northWestInner() {
        set(1, 50, 1, lava);

        verify(instanceSpace, never()).columnarSpaceAt(-1, -1);
        verify(instanceSpace).columnarSpaceAt(0, -1);
        verify(instanceSpace).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(8)).columnarSpaceAt(0, 0);

        assertNeighborhood(1, 50, 1);
    }

    @Test
    public void northEastInner() {
        set(14, 50, 1, lava);

        verify(instanceSpace, never()).columnarSpaceAt(+1, -1);
        verify(instanceSpace).columnarSpaceAt(0, -1);
        verify(instanceSpace).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(8)).columnarSpaceAt(0, 0);

        assertNeighborhood(14, 50, 1);
    }

    @Test
    public void southWestInner() {
        set(1, 50, 14, lava);

        verify(instanceSpace, never()).columnarSpaceAt(-1, +1);
        verify(instanceSpace).columnarSpaceAt(0, +1);
        verify(instanceSpace).columnarSpaceAt(-1, 0);
        verify(instanceSpace, times(8)).columnarSpaceAt(0, 0);

        assertNeighborhood(1, 50, 14);
    }

    @Test
    public void southEastInner() {
        set(14, 50, 14, lava);

        verify(instanceSpace, never()).columnarSpaceAt(+1, +1);
        verify(instanceSpace).columnarSpaceAt(0, +1);
        verify(instanceSpace).columnarSpaceAt(+1, 0);
        verify(instanceSpace, times(8)).columnarSpaceAt(0, 0);

        assertNeighborhood(14, 50, 14);
    }

    @Test
    public void wallUp() {
        set(5, 63, 5, wall);

        verify(instanceSpace, atLeastOnce()).columnarSpaceAt(0, 0);

        final byte top = areaOcclusionProvider.elementAt(5, 64, 5);

        assertTrue(Element.earth.in(top) && Logic.fuzzy.in(top));
    }

    @Test
    public void undoWallUp() {
        blockAt(5, 63, 5, wall);
        centerField().loadFrom(centerSpace, 0, 3, 0);

        final byte pre = areaOcclusionProvider.elementAt(5, 64, 5);
        assertTrue(Element.earth.in(pre) && Logic.fuzzy.in(pre));

        set(5, 63, 5, air);

        verify(instanceSpace, atLeastOnce()).columnarSpaceAt(0, 0);

        final byte top = areaOcclusionProvider.elementAt(5, 64, 5);

        assertFalse(Element.earth.in(top) || Logic.fuzzy.in(top));
    }

    @Test
    public void wallDown() {
        set(5, 48, 5, wall);

        blockAt(5, 48, 5, wall);

        final byte top = areaOcclusionProvider.elementAt(5, 49, 5);

        assertTrue(Element.earth.in(top) && Logic.fuzzy.in(top));
    }

    @Test
    public void undoWallDown() {
        blockAt(5, 48, 5, wall);

        centerField().loadFrom(centerSpace, 0, 3, 0);

        final byte pre = areaOcclusionProvider.elementAt(5, 49, 5);
        assertTrue(Element.earth.in(pre) && Logic.fuzzy.in(pre));

        set(5, 48, 5, air);

        final byte top = areaOcclusionProvider.elementAt(5, 49, 5);

        assertFalse(Element.earth.in(top) || Logic.fuzzy.in(top));
    }

    @Test
    public void ladderClimable() {
        set(5, 50, 5, stone);
        set(6, 50, 5, ladder);

        assertTrue(Logic.climbable(areaOcclusionProvider.elementAt(6, 50, 5)));
        assertFalse(Logic.climbable(areaOcclusionProvider.elementAt(5, 50, 5)));
        assertFalse(Logic.climbable(areaOcclusionProvider.elementAt(7, 50, 5)));
        assertFalse(Logic.climbable(areaOcclusionProvider.elementAt(6, 50, 4)));
        assertFalse(Logic.climbable(areaOcclusionProvider.elementAt(6, 50, 6)));
    }

    private void assertNeighborhood(final int dx, final int dy, final int dz) {
        assertTrue(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx - 1), dy, dz)));
        assertTrue(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx + 1), dy, dz)));
        assertTrue(Logic.fuzzy.in(areaOcclusionProvider.elementAt(dx, dy, (dz - 1))));
        assertTrue(Logic.fuzzy.in(areaOcclusionProvider.elementAt(dx, dy, (dz + 1))));
        assertFalse(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx + 1), dy, (dz + 1))));
        assertFalse(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx - 1), dy, (dz - 1))));
        assertFalse(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx - 1), dy, (dz + 1))));
        assertFalse(Logic.fuzzy.in(areaOcclusionProvider.elementAt((dx + 1), dy, (dz - 1))));
    }

    @Test
    public void doorOpened() {
        door(false, 4, 63, 6);
        centerField().loadFrom(centerSpace, 0, 3, 0);
        door(true, 4, 63, 6);

        centerField().set(centerSpace, 4, 63, 6, door);
        assertDoorway(areaOcclusionProvider, true, 4, 63, 6);
    }

    @Test
    public void doorClosed() {
        door(true, 4, 63, 6);
        centerField().loadFrom(centerSpace, 0, 3, 0);
        door(false, 4, 63, 6);

        centerField().set(centerSpace, 4, 63, 6, door);
        assertDoorway(areaOcclusionProvider, false, 4, 63, 6);
    }
}
