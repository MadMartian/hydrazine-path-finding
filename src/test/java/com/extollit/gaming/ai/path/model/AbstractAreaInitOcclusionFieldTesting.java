package com.extollit.gaming.ai.path.model;

import org.junit.Before;

import static com.extollit.gaming.ai.path.TestingBlocks.air;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAreaInitOcclusionFieldTesting extends AbstractOcclusionProviderTesting {
    private final IColumnarSpace [] columnarSpaces = new IColumnarSpace[OcclusionField.AreaInit.values().length - 2];
    private final ColumnarOcclusionFieldList[] fieldLists = new ColumnarOcclusionFieldList[OcclusionField.AreaInit.values().length - 2];

    protected ColumnarOcclusionFieldList centerFieldList;
    protected AreaOcclusionProvider areaOcclusionProvider;

    protected final int cy0;

    protected AbstractAreaInitOcclusionFieldTesting(int cy0) {
        this.cy0 = cy0;
    }

    @Before
    public void setup () {
        super.setup();

        this.centerFieldList = new TestColumnarOcclusionFieldList(centerSpace);

        for (OcclusionField.AreaInit area : OcclusionField.AreaInit.values()) {
            final IColumnarSpace columnarSpace;
            final ColumnarOcclusionFieldList fieldList;

            if (area == OcclusionField.AreaInit.up || area == OcclusionField.AreaInit.down) {
                columnarSpace = centerSpace;
                fieldList = centerFieldList;
            } else {
                columnarSpace = columnarSpaces[area.ordinal()] = mock(IColumnarSpace.class);
                fieldList = fieldLists[area.ordinal()] = new TestColumnarOcclusionFieldList(columnarSpace);
            }

            when(columnarSpace.instance()).thenReturn(instanceSpace);
            when(columnarSpace.blockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
            when(columnarSpace.toString()).thenReturn("Chunk to the " + area.name());
            when(columnarSpace.occlusionFields()).thenReturn(fieldList);

            fieldList.occlusionFieldAt(area.offset.dx, area.offset.dy + cy0, area.offset.dz);

            when(instanceSpace.columnarSpaceAt(area.offset.dx, area.offset.dz)).thenReturn(columnarSpace);
        }
        when(centerSpace.blockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(centerSpace.toString()).thenReturn("Center space");
        when(centerSpace.occlusionFields()).thenReturn(centerFieldList);
        when(centerSpace.instance()).thenReturn(instanceSpace);

        when(instanceSpace.columnarSpaceAt(0, 0)).thenReturn(centerSpace);
        centerFieldList.occlusionFieldAt(0, cy0, 0);

        this.areaOcclusionProvider = new AreaOcclusionProvider(
            new IColumnarSpace[][] {
                new IColumnarSpace[] { columnarSpaces[OcclusionField.AreaInit.northWest.ordinal()], columnarSpaces[OcclusionField.AreaInit.north.ordinal()], columnarSpaces[OcclusionField.AreaInit.northEast.ordinal()] },
                new IColumnarSpace[] { columnarSpaces[OcclusionField.AreaInit.west.ordinal()], centerSpace, columnarSpaces[OcclusionField.AreaInit.east.ordinal()] },
                new IColumnarSpace[] { columnarSpaces[OcclusionField.AreaInit.southWest.ordinal()], columnarSpaces[OcclusionField.AreaInit.south.ordinal()], columnarSpaces[OcclusionField.AreaInit.southEast.ordinal()] }
            },
            -1,
            -1
        );
    }

    protected final OcclusionField field(final OcclusionField.AreaInit area) {
        return occlusionFieldList(area).occlusionFieldAt(area.offset.dx, area.offset.dy, area.offset.dz);
    }

    private ColumnarOcclusionFieldList occlusionFieldList(OcclusionField.AreaInit area) {
        final ColumnarOcclusionFieldList columnarOcclusionFieldList;
        switch (area) {
            case up:
            case down:
                columnarOcclusionFieldList = centerFieldList;
                break;
            default:
                columnarOcclusionFieldList = fieldLists[area.ordinal()];
        }
        return columnarOcclusionFieldList;
    }

    protected final IColumnarSpace columnarSpace(final OcclusionField.AreaInit area) {
        return area == OcclusionField.AreaInit.up || area == OcclusionField.AreaInit.down ? centerSpace : columnarSpaces[area.ordinal()];
    }

    protected final void set(final OcclusionField.AreaInit area, final int x, final int y, final int z, final IBlockObject block) {
        field(area).set(columnarSpace(area), x, y, z, block);
        when(instanceSpace.blockObjectAt(x, y, z)).thenReturn(block);
    }
    protected final void set(final int x, final int y, final int z, final IBlockObject block) {
        blockAt(x, y, z, block);
        centerFieldList.occlusionFieldAt(x >> 4, y >> 4, z >> 4).set(centerSpace, x, y, z, block);
    }

    protected OcclusionField centerField() {
        return centerFieldAt(cy0);
    }
    protected OcclusionField centerFieldAt(int cy) {
        return centerFieldList.occlusionFieldAt(0, cy, 0);
    }
}
