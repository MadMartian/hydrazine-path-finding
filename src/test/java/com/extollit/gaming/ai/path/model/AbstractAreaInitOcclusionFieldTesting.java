package com.extollit.gaming.ai.path.model;

import org.junit.Before;

import static com.extollit.gaming.ai.path.TestingBlocks.air;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAreaInitOcclusionFieldTesting extends AbstractOcclusionProviderTesting {
    private final IColumnarSpace [] columnarSpaces = new IColumnarSpace[OcclusionField.AreaInit.values().length - 2];
    private final OcclusionField [] fields = new OcclusionField[OcclusionField.AreaInit.values().length];

    protected OcclusionField centerField;
    protected AreaOcclusionProvider areaOcclusionProvider;

    protected final int cy0;

    protected AbstractAreaInitOcclusionFieldTesting(int cy0) {
        this.cy0 = cy0;
    }

    @Before
    public void setup () {
        super.setup();

        this.centerField = new TestOcclusionField();

        for (OcclusionField.AreaInit area : OcclusionField.AreaInit.values()) {
            final IColumnarSpace columnarSpace = area == OcclusionField.AreaInit.up || area == OcclusionField.AreaInit.down ? centerSpace : (columnarSpaces[area.ordinal()] = mock(IColumnarSpace.class));
            final OcclusionField field = fields[area.ordinal()] = new TestOcclusionField(area);

            when(columnarSpace.instance()).thenReturn(instanceSpace);
            when(columnarSpace.blockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
            when(columnarSpace.toString()).thenReturn("Chunk to the " + area.name());

            when(columnarSpace.optOcclusionFieldAt(area.offset.dy)).thenReturn(field);
            when(columnarSpace.occlusionFieldAt(area.offset.dx, area.offset.dy + cy0, area.offset.dz)).thenReturn(field);
            when(instanceSpace.optOcclusionFieldAt(area.offset.dx, area.offset.dy + cy0, area.offset.dz)).thenReturn(field);
        }
        when(centerSpace.blockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(centerSpace.toString()).thenReturn("Center space");

        when(centerSpace.optOcclusionFieldAt(cy0)).thenReturn(centerField);
        when(centerSpace.occlusionFieldAt(0, cy0, 0)).thenReturn(centerField);
        when(instanceSpace.optOcclusionFieldAt(0, cy0, 0)).thenReturn(centerField);

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
        return fields[area.ordinal()];
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
        centerField.set(centerSpace, x, y, z, block);
    }
}
