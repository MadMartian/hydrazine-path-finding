package com.extollit.gaming.ai.path.model.octree;

interface OctantAllocator<T> {
    LeafOctant<T> allocateLeaf(ContainerOctant<T> parent, FramePointer pointer);
    ContainerOctant<T> allocateContainer(ContainerOctant<T> parent, FramePointer pointer);
}
