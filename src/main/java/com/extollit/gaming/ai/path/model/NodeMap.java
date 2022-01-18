package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.persistence.internal.*;
import com.extollit.linalg.immutable.Vec3i;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

public final class NodeMap {
    private final SparseSpatialMap<Node> it = new SparseSpatialMap<>();
    private final IInstanceSpace instanceSpace;
    private final IOcclusionProviderFactory occlusionProviderFactory;

    private INodeCalculator calculator;
    private IGraphNodeFilter filter;
    private IOcclusionProvider occlusionProvider;
    private int cx0, cxN, cz0, czN;

    public NodeMap(IInstanceSpace instanceSpace, IOcclusionProviderFactory occlusionProviderFactory) {
        this(instanceSpace, null, occlusionProviderFactory);
    }
    public NodeMap(IInstanceSpace instanceSpace, INodeCalculator calculator, IOcclusionProviderFactory occlusionProviderFactory) {
        this.calculator = calculator;
        this.instanceSpace = instanceSpace;
        this.occlusionProviderFactory = occlusionProviderFactory;
    }

    public void filter(IGraphNodeFilter filter) {
        this.filter = filter;
        clear();
    }

    public IGraphNodeFilter filter() {
        return this.filter;
    }

    public void calculator(INodeCalculator calculator) {
        this.calculator = calculator;
        clear();
    }

    public final void reset(SortedPointQueue queue) {
        for (Node p : this.it.values())
            p.rollback();

        queue.clear();
    }

    public final void cullBranchAt(Vec3i coords, SortedPointQueue queue) {
        cullBranchAt(coords.x, coords.y, coords.z, queue);
    }
    public final void cullBranchAt(int x, int y, int z, SortedPointQueue queue) {
        final Node
                node = this.it.get(x, y, z);

        if (node == null)
            return;

        final Node
                parent = node.up();

        queue.cullBranch(node);
        if (parent != null && !parent.assigned()) {
            parent.visited(false);
            queue.add(parent);
        }

        this.it.remove(x, y, z);
    }

    public final void reset() {
        clear();
        this.occlusionProvider = null;
    }

    public final void clear() {
        this.it.clear();
    }

    public boolean needsOcclusionProvider() {
        return this.occlusionProvider == null;
    }

    public byte flagsAt(int x, int y, int z) {
        return this.occlusionProvider.elementAt(x, y, z);
    }

    public void updateFieldWindow(int x0, int z0, int xN, int zN, boolean cull) {
        final int
                cx0 = x0 >> 4,
                cz0 = z0 >> 4,
                cxN = xN >> 4,
                czN = zN >> 4;

        final IOcclusionProvider aop = this.occlusionProvider;
        final boolean windowTest;

        if (cull)
            windowTest = cx0 != this.cx0 || cz0 != this.cz0 || cxN != this.cxN || czN != this.czN;
        else
            windowTest = cx0 < this.cx0 || cz0 < this.cz0 || cxN > this.cxN || czN > this.czN;

        if (aop == null || windowTest) {
            this.occlusionProvider = this.occlusionProviderFactory.fromInstanceSpace(this.instanceSpace, cx0, cz0, cxN, czN);
            this.cx0 = cx0;
            this.cz0 = cz0;
            this.cxN = cxN;
            this.czN = czN;

            if (cull)
                cullOutside(x0, z0, xN, zN);
        }
    }

    public Collection<Node> all() {
        return this.it.values();
    }

    public final void cullOutside(int x0, int z0, int xN, int zN) {
        for (Node p : this.it.cullOutside(x0, Integer.MIN_VALUE, z0, xN, Integer.MAX_VALUE, zN))
            p.rollback();
    }

    public final Node cachedPointAt(int x, int y, int z) {
        Node point = this.it.get(x, y, z);

        if (point == null) {
            point = passibleNodeNear(x, y, z, null);
            final Vec3i key = point.key;
            if (key.x != x || key.y != y || key.z != z)
                point = new Node(x, y, z, Passibility.impassible, false);

            this.it.put(x, y, z, point);
        }

        return point;
    }

    public Node cachedPassiblePointNear(int x, int y, int z) {
        return cachedPassiblePointNear(x, y, z, null);
    }

    public final Node cachedPassiblePointNear(int x, int y, int z, Vec3i origin) {
        final SparseSpatialMap<Node> nodeMap = this.it;
        final Node point0 = nodeMap.get(x, y, z);
        Node point = point0;

        if (point == null)
             point = passibleNodeNear(x, y, z, origin);
        else if (point.volatile_()) {
            point = passibleNodeNear(x, y, z, origin);
            if (point.key.equals(point0.key)) {
                point0.passibility(point.passibility());
                point0.volatile_(point.volatile_());
                point = point0;
            } else
                point0.isolate();
        }

        final Vec3i key = point.key;
        if (key.x != x || key.y != y || key.z != z) {
            final Node existing = nodeMap.get(key.x, key.y, key.z);
            if (existing == null)
                nodeMap.put(key.x, key.y, key.z, point);
            else
                point = existing;
        }

        if (point != point0)
            nodeMap.put(x, y, z, point);

        return point;
    }

    private Node passibleNodeNear(int x, int y, int z, Vec3i origin) {
        final Node node = this.calculator.passibleNodeNear(x, y, z, origin, new FlagSampler(this.occlusionProvider));
        final IGraphNodeFilter filter = this.filter;
        if (filter != null) {
            final Passibility newPassibility = filter.mapPassibility(node);
            if (newPassibility != null)
                node.passibility(newPassibility);
        }

        return node;
    }

    public boolean remove(int x, int y, int z) {
        final Node existing = this.it.remove(x, y, z);
        if (existing != null) {
            existing.rollback();
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

    private final class MapReaderWriter extends Vec3iReaderWriter implements LinkableReader<Vec3i, Node>, LinkableWriter<Vec3i, Node> {
        @Override
        public void readLinkages(Vec3i object, ReferableObjectInput<Node> in) throws IOException {
            it.put(object.x, object.y, object.z, in.readRef());
        }

        @Override
        public void writeLinkages(Vec3i object, ReferableObjectOutput<Node> out) throws IOException {
            out.writeRef(it.get(object.x, object.y, object.z));
        }
    }

    public void writeTo(ObjectOutput out, IdentityMapper<Node, Node.ReaderWriter> nodeIdentityMap) throws IOException {
        out.writeInt(this.cx0);
        out.writeInt(this.cz0);
        out.writeInt(this.cxN);
        out.writeInt(this.czN);

        final SparseSpatialMap<Node> it = this.it;
        nodeIdentityMap.initialize(Node.ReaderWriter.INSTANCE, it.values(), out);
        nodeIdentityMap.writeWith(new MapReaderWriter(), it.keySet(), out);
    }

    public void readFrom(ObjectInput in, IdentityMapper<Node, Node.ReaderWriter> nodeIdentityMap) throws IOException {
        this.cx0 = in.readInt();
        this.cz0 = in.readInt();
        this.cxN = in.readInt();
        this.czN = in.readInt();

        final Iterable<Node> nodes = nodeIdentityMap.readAll(in);
        nodeIdentityMap.readLinks(Node.ReaderWriter.INSTANCE, nodes, in);
        nodeIdentityMap.readWith(new MapReaderWriter(), in);
    }
}
