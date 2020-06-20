package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec2d;

import java.util.HashMap;
import java.util.Map;

public class InstanceSpace implements IInstanceSpace {
    private final Map<ColumnarSpace.Pointer, ColumnarSpace> columnarSpaces = new HashMap<>();

    @Override
    public IBlockObject blockObjectAt(int x, int y, int z) {
        final ColumnarSpace columnarSpace = columnarSpaceAt(x >> 3, z >> 3);

        if (columnarSpace == null)
            return null;

        return columnarSpace.blockAt(x - (x & 0xF), y, z - (z & 0xF));
    }

    @Override
    public OcclusionField optOcclusionFieldAt(int cx, int cy, int cz) {
        final IColumnarSpace columnarSpace = columnarSpaceAt(cx, cz);
        if (columnarSpace == null)
            return null;

        return columnarSpace.optOcclusionFieldAt(cy);
    }

    @Override
    public IOcclusionProvider occlusionProviderFor(int cx0, int cz0, int cxN, int czN) {
        IColumnarSpace[][] array = new IColumnarSpace[czN - cz0 + 1][cxN - cx0 + 1];

        for (int cz = cz0; cz <= czN; ++cz)
            for (int cx = cx0; cx <= cxN; ++cx) {
                final IColumnarSpace columnarSpace = columnarSpaceAt(cx, cz);
                if (columnarSpace != null)
                    array[cz - cz0][cx - cx0] = columnarSpace;
            }

        return new AreaOcclusionProvider(array, cx0, cz0);
    }

    private ColumnarSpace columnarSpaceAt(int cx, int cz) {
        final ColumnarSpace.Pointer pointer = new ColumnarSpace.Pointer(cx, cz);

        return this.columnarSpaces.get(pointer);
    }
}
