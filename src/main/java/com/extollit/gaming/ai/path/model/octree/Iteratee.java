package com.extollit.gaming.ai.path.model.octree;

public interface Iteratee<T> {
    void visit(T element, int x, int y, int z);
}
