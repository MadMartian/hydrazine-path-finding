package com.extollit.gaming.ai.path.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.TestingBlocks.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AreaInitOcclusionFieldTests extends AbstractAreaInitOcclusionFieldTesting {
    public AreaInitOcclusionFieldTests() {
        super(3);
    }

    @Test
    public void center() {
        final IColumnarSpace columnarSpace = centerSpace;
        when(columnarSpace.blockAt(5, 5, 5)).thenReturn(lava);

        final OcclusionField main = centerField();
        main.loadFrom(columnarSpace, 0, 0, 0);

        assertFalse(Logic.fuzzy.in(main.elementAt(5, 5, 5)));
        assertTrue(Logic.fuzzy.in(main.elementAt(6, 5, 5)));
        assertTrue(Logic.fuzzy.in(main.elementAt(4, 5, 5)));
        assertTrue(Logic.fuzzy.in(main.elementAt(5, 5, 6)));
        assertTrue(Logic.fuzzy.in(main.elementAt(5, 5, 4)));
        assertFalse(Logic.fuzzy.in(main.elementAt(6, 5, 6)));
        assertFalse(Logic.fuzzy.in(main.elementAt(4, 5, 4)));
        assertFalse(Logic.fuzzy.in(main.elementAt(4, 5, 6)));
        assertFalse(Logic.fuzzy.in(main.elementAt(6, 5, 4)));
    }

    @Test
    public void west() {
        set(OcclusionField.AreaInit.west, -1, 5, 5, lava);
        final OcclusionField field = centerField();
        field.areaInitWest(field(OcclusionField.AreaInit.west));

        assertTrue(Logic.fuzzy.in(field.elementAt(0, 5, 5)));
        assertFalse(Logic.fuzzy.in(field.elementAt(0, 5, 4)));
        assertFalse(Logic.fuzzy.in(field.elementAt(0, 5, 6)));
        assertFalse(Logic.fuzzy.in(field.elementAt(1, 5, 5)));
    }

    @Test
    public void north() {
        set(OcclusionField.AreaInit.north, 5, 5, -1, lava);
        final OcclusionField field = centerField();
        field.areaInitNorth(field(OcclusionField.AreaInit.north));

        assertTrue(Logic.fuzzy.in(field.elementAt(5, 5, 0)));
        assertFalse(Logic.fuzzy.in(field.elementAt(4, 5, 0)));
        assertFalse(Logic.fuzzy.in(field.elementAt(6, 5, 0)));
        assertFalse(Logic.fuzzy.in(field.elementAt(5, 5, 1)));
    }

    @Test
    public void east() {
        set(OcclusionField.AreaInit.east, 16, 5, 5, lava);
        final OcclusionField main = centerField();
        main.areaInitEast(field(OcclusionField.AreaInit.east));

        assertTrue(Logic.fuzzy.in(main.elementAt(15, 5, 5)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 5, 4)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 5, 6)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 5, 5)));
    }

    @Test
    public void south() {
        set(OcclusionField.AreaInit.south, 5, 5, 16, lava);
        final OcclusionField main = centerField();
        main.areaInitSouth(field(OcclusionField.AreaInit.south));

        assertTrue(Logic.fuzzy.in(main.elementAt(5, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(4, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(6, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(5, 5, 14)));
    }

    @Test
    public void northWest() {
        set(OcclusionField.AreaInit.west, -1, 5, 0, lava);
        set(OcclusionField.AreaInit.north, 0, 5, -1, lava);
        set(OcclusionField.AreaInit.west, -1, 6, 0, lava);
        set(OcclusionField.AreaInit.north, 0, 7, -1, lava);
        final OcclusionField main = centerField();
        main.areaInitNorthWest(field(OcclusionField.AreaInit.west), field(OcclusionField.AreaInit.north));

        assertTrue(Logic.fuzzy.in(main.elementAt(0, 5, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 5, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 5, 1)));
        assertTrue(Logic.fuzzy.in(main.elementAt(0, 6, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 6, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 6, 1)));
        assertTrue(Logic.fuzzy.in(main.elementAt(0, 7, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 7, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 7, 1)));

        assertFalse(Logic.fuzzy.in(main.elementAt(0, 8, 0)));
    }

    @Test
    public void northEast() {
        set(OcclusionField.AreaInit.east, 16, 5, 0, lava);
        set(OcclusionField.AreaInit.north, 15, 5, -1, lava);
        set(OcclusionField.AreaInit.east, 16, 6, 0, lava);
        set(OcclusionField.AreaInit.north, 15, 7, -1, lava);
        final OcclusionField main = centerField();
        main.areaInitNorthEast(field(OcclusionField.AreaInit.east), field(OcclusionField.AreaInit.north));

        assertTrue(Logic.fuzzy.in(main.elementAt(15, 5, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 5, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 5, 1)));
        assertTrue(Logic.fuzzy.in(main.elementAt(15, 6, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 6, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 6, 1)));
        assertTrue(Logic.fuzzy.in(main.elementAt(15, 7, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 7, 0)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 7, 1)));

        assertFalse(Logic.fuzzy.in(main.elementAt(15, 8, 0)));
    }

    @Test
    public void southWest() {
        set(OcclusionField.AreaInit.west, -1, 5, 15, lava);
        set(OcclusionField.AreaInit.south, 0, 5, 16, lava);
        set(OcclusionField.AreaInit.west, -1, 6, 15, lava);
        set(OcclusionField.AreaInit.south, 0, 7, 16, lava);
        final OcclusionField main = centerField();
        main.areaInitSouthWest(field(OcclusionField.AreaInit.west), field(OcclusionField.AreaInit.south));

        assertTrue(Logic.fuzzy.in(main.elementAt(0, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 5, 14)));
        assertTrue(Logic.fuzzy.in(main.elementAt(0, 6, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 6, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 6, 14)));
        assertTrue(Logic.fuzzy.in(main.elementAt(0, 7, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(1, 7, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(0, 7, 14)));

        assertFalse(Logic.fuzzy.in(main.elementAt(0, 8, 15)));
    }

    @Test
    public void southEast() {
        set(OcclusionField.AreaInit.east, 16, 5, 15, lava);
        set(OcclusionField.AreaInit.south, 15, 5, 16, lava);
        set(OcclusionField.AreaInit.east, 16, 6, 15, lava);
        set(OcclusionField.AreaInit.south, 15, 7, 16, lava);
        final OcclusionField main = centerField();
        main.areaInitSouthEast(field(OcclusionField.AreaInit.east), field(OcclusionField.AreaInit.south));

        assertTrue(Logic.fuzzy.in(main.elementAt(15, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 5, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 5, 14)));
        assertTrue(Logic.fuzzy.in(main.elementAt(15, 6, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 6, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 6, 14)));
        assertTrue(Logic.fuzzy.in(main.elementAt(15, 7, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(14, 7, 15)));
        assertFalse(Logic.fuzzy.in(main.elementAt(15, 7, 14)));

        assertFalse(Logic.fuzzy.in(main.elementAt(15, 8, 15)));
    }

    @Test
    public void truncatedFence() {
        final IColumnarSpace columnarSpace = centerSpace;
        blockAt(5, 63, 5, wall);

        final OcclusionField main = centerField(), up = field(OcclusionField.AreaInit.up);
        main.loadFrom(columnarSpace, 0, 3, 0);
        up.loadFrom(columnarSpace, 0, 4, 0);

        assertFalse(main.areaInitAt(OcclusionField.AreaInit.up));

        final byte [] wall = {
            areaOcclusionProvider.elementAt(5, 63, 5),
            areaOcclusionProvider.elementAt(5, 64, 5)
        };

        assertTrue(main.areaInitAt(OcclusionField.AreaInit.up));

        assertTrue(Element.earth.in(wall[0]));
        assertTrue(Element.earth.in(wall[1]));
        assertTrue(Logic.fuzzy.in(wall[0]));
        assertTrue(Logic.fuzzy.in(wall[1]));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 62, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 65, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5 + 1, 63, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5 - 1, 63, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 63, 5 + 1)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 63, 5 - 1)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5 + 1, 64, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5 - 1, 64, 5)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 64, 5 + 1)));
        assertFalse(Element.earth.in(areaOcclusionProvider.elementAt(5, 64, 5 - 1)));
    }

    @Test
    public void truncatedFenceReverseQuery() {
        final IColumnarSpace columnarSpace = centerSpace;
        blockAt(5, 63, 5, wall);

        final OcclusionField
                up = field(OcclusionField.AreaInit.up),
                main = centerField();

        up.loadFrom(columnarSpace, 0, 4, 0);
        main.loadFrom(centerSpace, 0, 3, 0);

        final byte flags = areaOcclusionProvider.elementAt(5, 64, 5);
        assertTrue(Element.earth.in(flags) && Logic.fuzzy.in(flags));
    }

    @Test
    public void tooLow() {
        areaOcclusionProvider.elementAt(5, 0, 5);
        final OcclusionField bottomField = centerSpace.occlusionFields().optOcclusionFieldAt(0);
        assertNotNull(bottomField);
        assertTrue(bottomField.areaInitAt(OcclusionField.AreaInit.down));
    }

    @Test
    public void tooHigh() {
        areaOcclusionProvider.elementAt(5, 255, 5);
        final OcclusionField topField = centerSpace.occlusionFields().optOcclusionFieldAt(15);
        assertNotNull(topField);
        assertTrue(topField.areaInitAt(OcclusionField.AreaInit.up));
    }

    @Test
    public void doorOpened() {
        door(true, 4, 63, 6);

        final OcclusionField main = centerField(), up = field(OcclusionField.AreaInit.up);
        final IColumnarSpace columnarSpace = this.centerSpace;

        main.loadFrom(columnarSpace, 0, 3, 0);
        up.loadFrom(columnarSpace, 0, 4, 0);

        assertDoorway(areaOcclusionProvider, true, 4, 63, 6);
    }

    @Test
    public void doorClosed() {
        door(false, 4, 63, 6);

        final OcclusionField main = centerField(), up = field(OcclusionField.AreaInit.up);
        final IColumnarSpace columnarSpace = this.centerSpace;

        main.loadFrom(columnarSpace, 0, 3, 0);
        up.loadFrom(columnarSpace, 0, 4, 0);

        assertDoorway(areaOcclusionProvider, false, 4, 63, 6);
    }

    @Test
    public void swimmableLava() {
        for (int k = 0; k < 3; ++k)
            for (int i = 0; i < 3; ++i)
                for (int j = 0; j < 2; ++j)
                    blockAt(i, j, k, stone);

        blockAt(1, 1, 1, lava);

        final IColumnarSpace columnarSpace = centerSpace;
        final OcclusionField main = centerField();
        main.loadFrom(columnarSpace, 0, 0, 0);

        final byte lava = main.elementAt(1, 1, 1);
        assertTrue(Element.fire.in(lava));
        assertFalse(Logic.fuzzy.in(lava));
    }
}
