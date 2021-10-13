package com.extollit.gaming.ai.path.persistence;

import com.extollit.gaming.ai.path.model.IDynamicMovableObject;
import com.extollit.linalg.immutable.Vec3d;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

final class DummyDynamicMovableObject implements IDynamicMovableObject {
    public static final class ReaderWriter implements PartialObjectWriter<IDynamicMovableObject>, PartialObjectReader<DummyDynamicMovableObject> {
        private final Vec3dReaderWriter vec3dReaderWriter;

        public ReaderWriter(Vec3dReaderWriter vec3dReaderWriter) {
            this.vec3dReaderWriter = vec3dReaderWriter;
        }

        @Override
        public DummyDynamicMovableObject readPartialObject(ObjectInput in) throws IOException {
            if (!in.readBoolean())
                return null;

            final DummyDynamicMovableObject dummyDynamicMovableObject = new DummyDynamicMovableObject();
            dummyDynamicMovableObject.coordinates = this.vec3dReaderWriter.readPartialObject(in);
            dummyDynamicMovableObject.width = in.readFloat();
            dummyDynamicMovableObject.height = in.readFloat();
            return dummyDynamicMovableObject;
        }

        @Override
        public void writePartialObject(IDynamicMovableObject targetEntity, ObjectOutput out) throws IOException {
            out.writeBoolean(targetEntity != null);
            if (targetEntity == null)
                return;

            this.vec3dReaderWriter.writePartialObject(targetEntity.coordinates(), out);
            out.writeFloat(targetEntity.width());
            out.writeFloat(targetEntity.height());
        }
    }

    private Vec3d coordinates;
    private float width, height;
    
    @Override
    public Vec3d coordinates() {
        return this.coordinates;
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }
}
