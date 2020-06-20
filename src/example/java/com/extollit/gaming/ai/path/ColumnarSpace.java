package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockDescription;
import com.extollit.gaming.ai.path.model.IColumnarSpace;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.OcclusionField;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private final OcclusionField [] occlusionFields = new OcclusionField[16];
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
    public OcclusionField occlusionFieldAt(int cx, int cy, int cz) {
        final OcclusionField[] occlusionFields = this.occlusionFields;

        if (occlusionFields[cy] == null)
            occlusionFields[cy] = new OcclusionField();

        return occlusionFields[cy];
    }

    @Override
    public OcclusionField optOcclusionFieldAt(int cy) {
        return this.occlusionFields[cy];
    }

    @Override
    public Iterator<OcclusionField> iterateOcclusionFields() {
        List<OcclusionField> list = new ArrayList<OcclusionField>();
        for (OcclusionField field : this.occlusionFields)
            if (field != null)
                list.add(field);

        return list.iterator();
    }

    @Override
    public IInstanceSpace instance() {
        return this.container;
    }
}
