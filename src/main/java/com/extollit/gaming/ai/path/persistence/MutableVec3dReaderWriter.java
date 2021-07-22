package com.extollit.gaming.ai.path.persistence;

import com.extollit.linalg.mutable.Vec3d;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MutableVec3dReaderWriter implements PartialObjectReader<Vec3d>, PartialObjectWriter<Vec3d> {
    public static final MutableVec3dReaderWriter INSTANCEz = new MutableVec3dReaderWriter();

    protected MutableVec3dReaderWriter() {}

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
