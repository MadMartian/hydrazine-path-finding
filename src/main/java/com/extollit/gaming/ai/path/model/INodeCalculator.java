package com.extollit.gaming.ai.path.model;

public interface INodeCalculator {
    void applySubject(IPathingEntity subject);
    Node passibleNodeNear(int x, int y, int z, Coords origin, final FlagSampler flagSampler);
    boolean omnidirectional();
}
