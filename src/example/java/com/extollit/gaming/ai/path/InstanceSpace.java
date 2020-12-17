package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.gaming.ai.path.model.IInstanceSpace;

import java.util.HashMap;
import java.util.Map;

public class InstanceSpace implements IInstanceSpace {
    private final Map<ColumnarSpace.Pointer, ColumnarSpace> columnarSpaces = new HashMap<>();

    @Override
    public IBlockObject blockObjectAt(int x, int y, int z) {
        final ColumnarSpace columnarSpace = columnarSpaceAt(x >> 3, z >> 3);

        if (columnarSpace == null)
            return null;

        return columnarSpace.blockAt(x & 0xF, y, z & 0xF);
    }

    @Override
    public ColumnarSpace columnarSpaceAt(int cx, int cz) {
        final ColumnarSpace.Pointer pointer = new ColumnarSpace.Pointer(cx, cz);
        final Map<ColumnarSpace.Pointer, ColumnarSpace> columnarSpaces = this.columnarSpaces;

        final ColumnarSpace columnarSpace;
        if (columnarSpaces.containsKey(pointer))
            columnarSpace = columnarSpaces.get(pointer);
        else
            columnarSpaces.put(pointer, columnarSpace = new ColumnarSpace(this, pointer));

        return columnarSpace;
    }

    public void setBlock(int x, int y, int z, BlockObject block, int metaData) {
        final ColumnarSpace columnarSpace = columnarSpaceAt(x >> 4, z >> 4);

        if (columnarSpace != null) {
            columnarSpace.setBlockAt(x & 0xF, y, z & 0xF, block);
            columnarSpace.occlusionFields().onBlockChanged(x, y, z, block, metaData);
        }
    }
}
