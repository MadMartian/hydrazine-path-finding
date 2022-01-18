package com.extollit.gaming.ai.path.persistence.internal;

import com.extollit.gaming.ai.path.model.Coords;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public class NullableVec3iReaderWriter extends Vec3iReaderWriter {
    public static final NullableVec3iReaderWriter INSTANCE = new NullableVec3iReaderWriter();

    private NullableVec3iReaderWriter() {}

    @Override
    public Coords readPartialObject(ObjectInput in) throws IOException {
        if (in.readBoolean())
            return super.readPartialObject(in);
        else
            return null;
    }

    @Override
    public void writePartialObject(Coords object, ObjectOutput out) throws IOException {
        final boolean nonNull = object != null;
        out.writeBoolean(nonNull);
        if (nonNull)
            super.writePartialObject(object, out);
    }
}
