package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

public class TestPointPassibilityCalculatorDecorator implements NodeMap.IPointPassibilityCalculator {
    public final NodeMap.IPointPassibilityCalculator delegate;

    public TestPointPassibilityCalculatorDecorator(NodeMap.IPointPassibilityCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Node passiblePointNear(Vec3i coords0, Vec3i origin) {
        final Node node = this.delegate.passiblePointNear(coords0, origin);
        return node == null ? new Node(coords0) : node;
    }
}
