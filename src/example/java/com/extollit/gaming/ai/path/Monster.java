package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.linalg.immutable.Vec3d;

public class Monster implements IPathingEntity, IPathingEntity.Capabilities {
    public boolean fireResistant, cautious, climber, swimmer, aquaphobic, avoidsDoorways, opensDoors;

    private final com.extollit.linalg.mutable.Vec3d position = new com.extollit.linalg.mutable.Vec3d(0, 0, 0);

    private int age;

    @Override
    public int age() {
        return this.age;
    }

    @Override
    public float searchRange() {
        return 32;
    }

    @Override
    public Capabilities capabilities() {
        return this;
    }

    @Override
    public void moveTo(Vec3d position) {
        this.position.set(position);
    }

    @Override
    public Vec3d coordinates() {
        return new Vec3d(this.position);
    }

    @Override
    public float width() {
        return 0.6f;
    }

    @Override
    public float height() {
        return 1.8f;
    }

    @Override
    public float speed() {
        return 1.0f;
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

    public void updateTick() {
        this.age++;
    }
}
