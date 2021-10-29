package com.extollit.gaming.ai.path.persistence.internal;

import com.extollit.linalg.immutable.Vec3d;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public class Vec3dReaderWriter implements PartialObjectReader<Vec3d>, PartialObjectWriter<Vec3d> {
    public static final Vec3dReaderWriter INSTANCEz = new Vec3dReaderWriter();

    protected Vec3dReaderWriter() {}

    @Override
    public Vec3d readPartialObject(ObjectInput in) throws IOException {
        return new Vec3d(
                in.readDouble(),
                in.readDouble(),
                in.readDouble()
        );
    }

    @Override
    public void writePartialObject(Vec3d object, ObjectOutput out) throws IOException {
        out.writeDouble(object.x);
        out.writeDouble(object.y);
        out.writeDouble(object.z);
    }
}
