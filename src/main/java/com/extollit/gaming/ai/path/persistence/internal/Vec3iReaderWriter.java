package com.extollit.gaming.ai.path.persistence.internal;

import com.extollit.linalg.immutable.Vec3i;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public class Vec3iReaderWriter implements PartialObjectReader<Vec3i>, PartialObjectWriter<Vec3i> {
    public static final Vec3iReaderWriter INSTANCEz = new Vec3iReaderWriter();

    protected Vec3iReaderWriter() {}

    @Override
    public Vec3i readPartialObject(ObjectInput in) throws IOException {
        return new Vec3i(
                in.readInt(),
                in.readInt(),
                in.readInt()
        );
    }

    @Override
    public void writePartialObject(Vec3i object, ObjectOutput out) throws IOException {
        out.writeInt(object.x);
        out.writeInt(object.y);
        out.writeInt(object.z);
    }
}
