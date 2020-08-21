package com.extollit.gaming.ai.path.model;

public class AreaOcclusionProvider implements IOcclusionProvider {
    private final IColumnarSpace[][] columnarSpaces;

    private final int cx0, cz0, cxN, czN;

    public AreaOcclusionProvider(IColumnarSpace[][] columnarSpaces, int cx0, int cz0) {
        this.columnarSpaces = columnarSpaces;
        this.cx0 = cx0;
        this.cz0 = cz0;
        this.cxN = columnarSpaces[0].length + cx0 - 1;
        this.czN = columnarSpaces.length + cz0 - 1;
    }

    @Override
    public byte elementAt(int x, int y, int z) {
        final IColumnarSpace[][] columnarSpaces = this.columnarSpaces;
        final int
            cx = x >> 4,
            cz = z >> 4,
            cy = y >> 4;

        if (cx >= cx0 && cx <= cxN && cz >= cz0 && cz <= czN && cy >= 0 && cy < OcclusionField.DIMENSION_SIZE) {
            final int

                czz = cz - cz0,
                cxx = cx - cx0;

            final IColumnarSpace columnarSpace = columnarSpaces[czz][cxx];
            if (columnarSpace != null) {
                final OcclusionField field = columnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz);

                if (!field.areaInitFull())
                    areaInit(field, x, y, z);

                return field.elementAt(x & OcclusionField.DIMENSION_MASK, y & OcclusionField.DIMENSION_MASK, z & OcclusionField.DIMENSION_MASK);
            }
        }

        return 0;
    }

    private void areaInit(OcclusionField field, int x, int y, int z) {
        final IColumnarSpace[][] columnarSpaces = this.columnarSpaces;
        final int
            cx = x >> 4,
            cy = y >> 4,
            cz = z >> 4,

            cxN = columnarSpaces[0].length - 1,
            czN = columnarSpaces.length - 1,

            czz = cz - cz0,
            cxx = cx - cx0,
            xx = x & OcclusionField.DIMENSION_MASK,
            yy = y & OcclusionField.DIMENSION_MASK,
            zz = z & OcclusionField.DIMENSION_MASK;

        final IColumnarSpace
            centerColumnarSpace = columnarSpaces[czz][cxx];

        if (xx == 0 && zz == 0 && !field.areaInitAt(OcclusionField.AreaInit.northWest) && cxx > 0 && czz > 0) {
            final IColumnarSpace
                westColumnarSpace = columnarSpaces[czz - 1][cxx],
                northColumnarSpace = columnarSpaces[czz][cxx - 1];

            if (northColumnarSpace != null && westColumnarSpace != null)
                field.areaInitNorthWest(
                        northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                        westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
                );
        } else if (xx == OcclusionField.DIMENSION_EXTENT && zz == 0 && !field.areaInitAt(OcclusionField.AreaInit.northEast) && cxx < cxN && czz > 0) {
            final IColumnarSpace
                westColumnarSpace = columnarSpaces[czz - 1][cxx],
                eastColumnarSpace = columnarSpaces[czz][cxx + 1];

            if (eastColumnarSpace != null && westColumnarSpace != null)
                field.areaInitNorthEast(
                        eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                        westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1)
                );
        } else if (xx == 0 && zz == OcclusionField.DIMENSION_EXTENT && !field.areaInitAt(OcclusionField.AreaInit.southWest) && cxx > 0 && czz < czN) {
            final IColumnarSpace
                northColumnarSpace = columnarSpaces[czz][cxx - 1],
                southColumnarSpace = columnarSpaces[czz + 1][cxx];

            if (northColumnarSpace != null && southColumnarSpace != null)
                field.areaInitSouthWest(
                        northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz),
                        southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
                );
        } else if (xx == OcclusionField.DIMENSION_EXTENT && zz == OcclusionField.DIMENSION_EXTENT && !field.areaInitAt(OcclusionField.AreaInit.southEast) && cxx < cxN && czz < czN) {
            final IColumnarSpace
                eastColumnarSpace = columnarSpaces[czz][cxx + 1],
                southColumnarSpace = columnarSpaces[czz + 1][cxx];

            if (eastColumnarSpace != null && southColumnarSpace != null)
                field.areaInitSouthEast(
                        eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz),
                        southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1)
                );
        } else if (xx == 0 && !field.areaInitAt(OcclusionField.AreaInit.west) && cxx > 0) {
            final IColumnarSpace
                northColumnarSpace = columnarSpaces[czz][cxx - 1];

            if (northColumnarSpace != null)
                field.areaInitWest(northColumnarSpace.occlusionFields().occlusionFieldAt(cx - 1, cy, cz));
        } else if (xx == OcclusionField.DIMENSION_EXTENT && !field.areaInitAt(OcclusionField.AreaInit.east) && cxx < cxN) {
            final IColumnarSpace
                eastColumnarSpace = columnarSpaces[czz][cxx + 1];

            if (eastColumnarSpace != null)
                field.areaInitEast(eastColumnarSpace.occlusionFields().occlusionFieldAt(cx + 1, cy, cz));
        } else if (zz == 0 && !field.areaInitAt(OcclusionField.AreaInit.north) && czz > 0) {
            final IColumnarSpace
                westColumnarSpace = columnarSpaces[czz - 1][cxx];

            if (westColumnarSpace != null)
                field.areaInitNorth(westColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz - 1));
        } else if (zz == OcclusionField.DIMENSION_EXTENT && !field.areaInitAt(OcclusionField.AreaInit.south) && czz < czN) {
            final IColumnarSpace
                southColumnarSpace = columnarSpaces[czz + 1][cxx];

            if (southColumnarSpace != null)
                field.areaInitSouth(southColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy, cz + 1));
        }

        if (yy == OcclusionField.DIMENSION_EXTENT && !field.areaInitAt(OcclusionField.AreaInit.up)) {
            field.areaInitUp(centerColumnarSpace, cy, cy < OcclusionField.DIMENSION_EXTENT ? centerColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy + 1, cz) : null);
        } else if (yy == 0 && !field.areaInitAt(OcclusionField.AreaInit.down)) {
            field.areaInitDown(centerColumnarSpace, cy, cy > 0 ? centerColumnarSpace.occlusionFields().occlusionFieldAt(cx, cy - 1, cz) : null);
        }
    }

    @Override
    public String visualizeAt(int y) {
        return OcclusionField.visualizeAt(this, y, cx0 << 4, cz0 << 4, (cxN + 1) << 4, (czN + 1) << 4);
    }
}
