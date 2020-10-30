package com.extollit.gaming.ai.path.model.octree;

interface OctantAllocator<T> {
    LeafOctant<T> allocateLeaf();
    ContainerOctant<T> allocateContainer();
}
