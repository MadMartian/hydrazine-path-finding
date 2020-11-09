package com.extollit.gaming.ai.path.model.octree;

abstract class AbstractOctant<T> {
    final FramePointer pointer;

    private ContainerOctant<T> parent;

    protected AbstractOctant(FramePointer pointer, ContainerOctant<T> parent) {
        this.pointer = pointer;
        this.parent = parent;
    }

    public final ContainerOctant<T> parent() { return this.parent; }
    public final void bindTo(ContainerOctant<T> parent) {
        assert this.parent == null;
        this.parent = parent;
    }
    public final boolean orphan() {
        return this.parent == null;
    }

    public abstract boolean empty();
    public abstract void accept(OctantVisitor<T> visitor);

    public AbstractOctant<T> trim() {
        return this;
    }
}
