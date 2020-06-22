package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;

public class ColumnarSpace implements IColumnarSpace {
    public static final class Pointer {
        public final int cx, cz;

        public Pointer(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pointer)) return false;

            final Pointer other = (Pointer) o;

            return cx == other.cx && cz == other.cz;
        }

        @Override
        public int hashCode() {
            return 31 * (cx >> 3) + (cz >> 3);
        }
    }

    public final IInstanceSpace container;
    public final Pointer location;

    private final ColumnarOcclusionFieldList columnarOcclusionFieldList = new ColumnarOcclusionFieldList(this);
    private final BlockObject [][][] blocks = new BlockObject[16][256][16];
    private final int [][][] metaDatas = new int[16][256][16];

    public ColumnarSpace(IInstanceSpace container, Pointer location) {
        this.container = container;
        this.location = location;
    }

    @Override
    public BlockObject blockAt(int x, int y, int z) {
        return this.blocks[z][y][x];
    }

    @Override
    public int metaDataAt(int x, int y, int z) {
        return this.metaDatas[z][y][x];
    }

    @Override
    public ColumnarOcclusionFieldList occlusionFields() {
        return this.columnarOcclusionFieldList;
    }

    @Override
    public IInstanceSpace instance() {
        return this.container;
    }

    public void load() {
        occlusionFields().reset();
    }

    public void unload() {
        occlusionFields().reset();
    }
}
