package com.extollit.gaming.ai.path.model;

import com.extollit.collect.SparseSpatialMap;
import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;

public final class NodeMap {
    public interface IPointPassibilityCalculator {
        Node passiblePointNear(Vec3i coords0, Vec3i origin);
    }

    private final SparseSpatialMap<Node> it = new SparseSpatialMap<>(3);
    private final IPointPassibilityCalculator calculator;

    public NodeMap(IPointPassibilityCalculator calculator) {
        this.calculator = calculator;
    }

    public final void reset(SortedPointQueue queue) {
        for (Node p : this.it.values())
            p.reset();

        queue.clear();
    }

    public final void clear() {
        this.it.clear();
    }

    public final void cullOutside(int x0, int z0, int xN, int zN) {
        for (Node p : this.it.cullOutside(new IntAxisAlignedBox(x0, Integer.MIN_VALUE, z0, xN, Integer.MAX_VALUE, zN)))
            p.reset();
    }

    public final Node cachedPointAt(int x, int y, int z)
    {
        final Vec3i coords = new Vec3i(x, y, z);
        return cachedPointAt(coords);
    }

    public final Node cachedPointAt(Vec3i coords) {
        Node point = this.it.get(coords);

        if (point == null)
            this.it.put(coords, point = new Node(coords));

        return point;
    }

    public Node cachedPassiblePointNear(int x, int y, int z) {
        return cachedPassiblePointNear(x, y, z, null);
    }

    public final Node cachedPassiblePointNear(final int x0, final int y0, final int z0, final Vec3i origin) {
        final Vec3i coords0 = new Vec3i(x0, y0, z0);
        return cachedPassiblePointNear(coords0, origin);
    }

    public Node cachedPassiblePointNear(Vec3i coords) {
        return cachedPassiblePointNear(coords, null);
    }

    public final Node cachedPassiblePointNear(Vec3i coords, Vec3i origin) {
        final SparseSpatialMap<Node> nodeMap = this.it;
        final Node point0 = nodeMap.get(coords);
        Node point = point0;

        if (point == null)
            point = this.calculator.passiblePointNear(coords, origin);
        else if (point.volatile_()) {
            point = this.calculator.passiblePointNear(coords, origin);
            if (point.key.equals(point0.key)) {
                point0.passibility(point.passibility());
                point0.volatile_(point.volatile_());
                point = point0;
            } else
                point0.isolate();
        }

        if (!coords.equals(point.key)) {
            final Node existing = nodeMap.get(point.key);
            if (existing == null)
                nodeMap.put(point.key, point);
            else
                point = existing;
        }

        if (point != point0)
            nodeMap.put(coords, point);

        return point;
    }

    public boolean remove(int x, int y, int z) {
        return remove(new Vec3i(x, y, z));
    }

    public boolean remove(Vec3i coords) {
        final Node existing = this.it.remove(coords);
        if (existing != null) {
            existing.reset();
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return this.it.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeMap nodeMap = (NodeMap) o;

        return it.equals(nodeMap.it);
    }

    @Override
    public int hashCode() {
        return it.hashCode();
    }
}
