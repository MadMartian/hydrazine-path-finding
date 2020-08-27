package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

public class TestPointPassibilityCalculatorDecorator implements IPointPassibilityCalculator {
    public final IPointPassibilityCalculator delegate;

    public TestPointPassibilityCalculatorDecorator(IPointPassibilityCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void applySubject(IPathingEntity subject) {
        this.delegate.applySubject(subject);
    }

    @Override
    public Node passiblePointNear(Vec3i coords0, Vec3i origin, FlagSampler flagSampler) {
        final Node node = this.delegate.passiblePointNear(coords0, origin, flagSampler);
        return node == null ? new Node(coords0) : node;
    }

    @Override
    public boolean omnidirectional() {
        return this.delegate.omnidirectional();
    }
}
