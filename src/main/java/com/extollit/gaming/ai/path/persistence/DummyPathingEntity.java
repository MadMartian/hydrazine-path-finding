package com.extollit.gaming.ai.path.persistence;

import com.extollit.gaming.ai.path.model.Gravitation;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.Passibility;
import com.extollit.linalg.immutable.Vec3d;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DummyPathingEntity implements IPathingEntity, IPathingEntity.Capabilities {
    public static final class ReaderWriter implements PartialObjectWriter<IPathingEntity>, PartialObjectReader<DummyPathingEntity> {
        private final MutableVec3dReaderWriter mutableVec3dReaderWriter;
        private final Vec3dReaderWriter vec3dReaderWriter;

        public ReaderWriter(MutableVec3dReaderWriter mutableVec3dReaderWriter, Vec3dReaderWriter vec3dReaderWriter) {
            this.mutableVec3dReaderWriter = mutableVec3dReaderWriter;
            this.vec3dReaderWriter = vec3dReaderWriter;
        }

        @Override
        public DummyPathingEntity readPartialObject(ObjectInput in) throws IOException {
            final DummyPathingEntity entity = new DummyPathingEntity();

            entity.coordinates = this.mutableVec3dReaderWriter.readPartialObject(in);
            entity.age = in.readInt();
            entity.searchRange = in.readFloat();
            entity.width = in.readFloat();
            entity.height = in.readFloat();
            entity.speed = in.readFloat();
            entity.fireResistant = in.readBoolean();
            entity.cautious = in.readBoolean();
            entity.climber = in.readBoolean();
            entity.swimmer = in.readBoolean();
            entity.aquatic = in.readBoolean();
            entity.avian = in.readBoolean();
            entity.aquaphobic = in.readBoolean();
            entity.avoidsDoorways = in.readBoolean();
            entity.opensDoors = in.readBoolean();

            return entity;
        }

        @Override
        public void writePartialObject(IPathingEntity entity, ObjectOutput out) throws IOException {
            this.vec3dReaderWriter.writePartialObject(entity.coordinates(), out);
            out.writeInt(entity.age());
            out.writeFloat(entity.searchRange());
            out.writeFloat(entity.width());
            out.writeFloat(entity.height());
            final Capabilities caps = entity.capabilities();
            out.writeFloat(caps.speed());
            out.writeBoolean(caps.fireResistant());
            out.writeBoolean(caps.cautious());
            out.writeBoolean(caps.climber());
            out.writeBoolean(caps.swimmer());
            out.writeBoolean(caps.aquatic());
            out.writeBoolean(caps.avian());
            out.writeBoolean(caps.aquaphobic());
            out.writeBoolean(caps.avoidsDoorways());
            out.writeBoolean(caps.opensDoors());
        }
    }
    
    private int age;
    private float searchRange, width, height, speed;
    private com.extollit.linalg.mutable.Vec3d coordinates;
    private boolean fireResistant, cautious, climber, swimmer, aquatic, avian, aquaphobic, avoidsDoorways, opensDoors, bound;

    @Override
    public int age() {
        return this.age;
    }

    @Override
    public boolean bound() {
        return this.bound;
    }

    @Override
    public float searchRange() {
        return this.searchRange;
    }

    @Override
    public Capabilities capabilities() {
        return this;
    }

    @Override
    public void moveTo(Vec3d position, Passibility passibility, Gravitation gravitation) {
        final com.extollit.linalg.mutable.Vec3d coords = this.coordinates;
        coords.set(position);
    }

    @Override
    public Vec3d coordinates() {
        return new Vec3d(this.coordinates);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }
    @Override
    public float speed() {
        return this.speed;
    }

    @Override
    public boolean fireResistant() {
        return this.fireResistant;
    }

    @Override
    public boolean cautious() {
        return this.cautious;
    }

    @Override
    public boolean climber() {
        return this.climber;
    }

    @Override
    public boolean swimmer() {
        return this.swimmer;
    }

    @Override
    public boolean aquatic() {
        return this.aquatic;
    }

    @Override
    public boolean avian() {
        return this.avian;
    }

    @Override
    public boolean aquaphobic() {
        return this.aquaphobic;
    }

    @Override
    public boolean avoidsDoorways() {
        return this.avoidsDoorways;
    }

    @Override
    public boolean opensDoors() {
        return this.opensDoors;
    }
}
