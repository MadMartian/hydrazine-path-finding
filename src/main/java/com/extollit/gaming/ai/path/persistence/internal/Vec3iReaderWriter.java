package com.extollit.gaming.ai.path.persistence.internal;

import com.extollit.gaming.ai.path.model.Coords;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public class Vec3iReaderWriter implements PartialObjectReader<Coords>, PartialObjectWriter<Coords> {
    public static final Vec3iReaderWriter INSTANCEz = new Vec3iReaderWriter();

    protected Vec3iReaderWriter() {}

    @Override
    public Coords readPartialObject(ObjectInput in) throws IOException {
        return new Coords(
                in.readInt(),
                in.readInt(),
                in.readInt()
        );
    }

    @Override
    public void writePartialObject(Coords object, ObjectOutput out) throws IOException {
        out.writeInt(object.x);
        out.writeInt(object.y);
        out.writeInt(object.z);
    }
}
