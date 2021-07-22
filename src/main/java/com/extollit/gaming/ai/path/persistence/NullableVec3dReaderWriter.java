package com.extollit.gaming.ai.path.persistence;

import com.extollit.linalg.immutable.Vec3d;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class NullableVec3dReaderWriter extends Vec3dReaderWriter {
    public static final NullableVec3dReaderWriter INSTANCE = new NullableVec3dReaderWriter();

    private NullableVec3dReaderWriter() {}

    @Override
    public Vec3d readPartialObject(ObjectInput in) throws IOException {
        if (in.readBoolean())
            return super.readPartialObject(in);
        else
            return null;
    }

    @Override
    public void writePartialObject(Vec3d object, ObjectOutput out) throws IOException {
        final boolean nonNull = object != null;
        out.writeBoolean(nonNull);
        if (nonNull)
            super.writePartialObject(object, out);
    }
}
