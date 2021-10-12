package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.model.octree.Iteratee;
import com.extollit.gaming.ai.path.model.octree.VoxelOctTreeMap;
import com.extollit.gaming.ai.path.persistence.IdentityMapper;
import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.gaming.ai.path.persistence.*;
import com.extollit.linalg.immutable.Vec3i;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public final class NodeMap {
    private final VoxelOctTreeMap<Node> it = new VoxelOctTreeMap<>(Node.class);
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

    private static final class ResetIteratee implements Iteratee<Node> {
        public static final Iteratee<Node> INSTANCE = new ResetIteratee();

        private ResetIteratee() {}

        @Override
        public void visit(Node element, int x, int y, int z) {
            element.rollback();
        }
    }
    public final void reset(SortedPointQueue queue) {
        this.it.forEach(ResetIteratee.INSTANCE);

        queue.clear();
    }

    public final void cullBranchAt(Vec3i coords, SortedPointQueue queue) {
        final Node
                node = this.it.get(coords);

        if (node == null)
            return;

        final Node
                parent = node.up();

        queue.cullBranch(node);
        if (parent != null && !parent.assigned()) {
            parent.visited(false);
            queue.add(parent);
        }

        this.it.remove(coords);
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
        final List<Node> list = new LinkedList<>();
        this.it.forEach(new Iteratee<Node>() {
            @Override
            public void visit(Node element, int x, int y, int z) {
                list.add(element);
            }
        });
        return list;
    }

    public Collection<Vec3i> keys() {
        final List<Vec3i> list = new LinkedList<>();
        this.it.forEach(new Iteratee<Node>() {
            @Override
            public void visit(Node element, int x, int y, int z) {
                list.add(new Vec3i(x, y, z));
            }
        });
        return list;
    }

    public final void cullOutside(int x0, int z0, int xN, int zN) {
        this.it.cullOutside(
            new IntAxisAlignedBox(x0, Integer.MIN_VALUE, z0, xN, Integer.MAX_VALUE, zN),
            new Iteratee<Node>() {
                @Override
                public void visit(Node element, int x, int y, int z) {
                    element.rollback();
                }
            }
        );
    }

    public final Node cachedPointAt(int x, int y, int z)
    {
        final Vec3i coords = new Vec3i(x, y, z);
        return cachedPointAt(coords);
    }

    public final Node cachedPointAt(Vec3i coords) {
        Node point = this.it.get(coords);

        if (point == null) {
            point = passibleNodeNear(coords, null);
            if (!point.key.equals(coords))
                point = new Node(coords, Passibility.impassible, false);

            this.it.put(coords, point);
        }

        return point;
    }

    public Node cachedPassiblePointNear(int x, int y, int z) {
        return cachedPassiblePointNear(x, y, z, null);
    }

    public final Node cachedPassiblePointNear(final int x0, final int y0, final int z0, final Vec3i origin) {
        final Vec3i coords0 = new Vec3i(x0, y0, z0);
        return cachedPassiblePointNear(coords0, origin);
    }

    public final Node cachedPassiblePointNear(Vec3i coords, Vec3i origin) {
        final VoxelOctTreeMap<Node> nodeMap = this.it;
        final Node point0 = nodeMap.get(coords);
        Node point = point0;

        if (point == null)
            point = passibleNodeNear(coords, origin);
        else if (point.volatile_()) {
            point = passibleNodeNear(coords, origin);
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

    private Node passibleNodeNear(Vec3i coords, Vec3i origin) {
        final Node node = this.calculator.passibleNodeNear(coords, origin, new FlagSampler(this.occlusionProvider));
        final IGraphNodeFilter filter = this.filter;
        if (filter != null) {
            final Passibility newPassibility = filter.mapPassibility(node);
            if (newPassibility != null)
                node.passibility(newPassibility);
        }

        return node;
    }

    public boolean remove(int x, int y, int z) {
        return remove(new Vec3i(x, y, z));
    }

    public boolean remove(Vec3i coords) {
        final Node existing = this.it.remove(coords);
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
            it.put(object, in.readRef());
        }

        @Override
        public void writeLinkages(Vec3i object, ReferableObjectOutput<Node> out) throws IOException {
            out.writeRef(it.get(object));
        }
    }

    public void writeTo(ObjectOutput out, IdentityMapper<Node, Node.ReaderWriter> nodeIdentityMap) throws IOException {
        out.writeInt(this.cx0);
        out.writeInt(this.cz0);
        out.writeInt(this.cxN);
        out.writeInt(this.czN);

        nodeIdentityMap.initialize(Node.ReaderWriter.INSTANCE, all(), out);
        nodeIdentityMap.writeWith(new com.extollit.gaming.ai.path.model.NodeMap.MapReaderWriter(), keys(), out);
    }

    public void readFrom(ObjectInput in, IdentityMapper<Node, Node.ReaderWriter> nodeIdentityMap) throws IOException {
        this.cx0 = in.readInt();
        this.cz0 = in.readInt();
        this.cxN = in.readInt();
        this.czN = in.readInt();

        final Iterable<Node> nodes = nodeIdentityMap.readAll(in);
        nodeIdentityMap.readLinks(Node.ReaderWriter.INSTANCE, nodes, in);
        nodeIdentityMap.readWith(new com.extollit.gaming.ai.path.model.NodeMap.MapReaderWriter(), in);
    }
}
