package com.extollit.gaming.ai.path.model;

public class TestNodeCalculatorDecorator implements INodeCalculator {
    public final INodeCalculator delegate;

    public TestNodeCalculatorDecorator(INodeCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void applySubject(IPathingEntity subject) {
        this.delegate.applySubject(subject);
    }

    @Override
    public Node passibleNodeNear(int x0, int y0, int z0, Coords origin, FlagSampler flagSampler) {
        final Node node = this.delegate.passibleNodeNear(x0, y0, z0, origin, flagSampler);
        return node == null ? new Node(x0, y0, z0) : node;
    }

    @Override
    public boolean omnidirectional() {
        return this.delegate.omnidirectional();
    }
}
