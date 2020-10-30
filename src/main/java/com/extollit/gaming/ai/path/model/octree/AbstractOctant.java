package com.extollit.gaming.ai.path.model.octree;

abstract class AbstractOctant<T> {
    public abstract boolean empty();
    public abstract void accept(OctantVisitor<T> visitor);

    public AbstractOctant<T> trim(Frame frame) {
        return this;
    }
}
