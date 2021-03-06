package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

public interface INodeCalculator {
    void applySubject(IPathingEntity subject);
    Node passibleNodeNear(Vec3i coords0, Vec3i origin, final FlagSampler flagSampler);
    boolean omnidirectional();
}
