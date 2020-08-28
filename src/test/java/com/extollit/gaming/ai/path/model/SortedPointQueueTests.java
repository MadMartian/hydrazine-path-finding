package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SortedPointQueueTests {
    @Mock private IInstanceSpace instanceSpace;
    @Mock private IOcclusionProviderFactory occlusionProviderFactory;
    @Mock private INodeCalculator calculator;
    @Mock private IOcclusionProvider occlusionProvider;

    private SortedPointQueue q;
    private NodeMap graph;
    private Node
        target,
        source;

    @Before
    public void setup() {
        when(occlusionProviderFactory.fromInstanceSpace(any(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(occlusionProvider);
        this.q = new SortedPointQueue();
        this.graph = new NodeMap(instanceSpace, new TestNodeCalculatorDecorator(this.calculator), occlusionProviderFactory);
        this.target = this.graph.cachedPointAt(0, 0, 7);
        (this.source = visited(0, 0, 0)).target(this.target.key);
    }

    @Test
    public void control() {
        this.q.clear();
        (this.source = add(0, 0, 3)).target(this.target.key);

        final Node middle = source;

        add(add(middle, 0, 0, 4), 0, 0, 5);
        add(add(middle, 1, 0, 3), 2, 0, 3);

        assertQueuePoints(
                new Vec3i(0, 0, 3),
                new Vec3i(0, 0, 4),
                new Vec3i(0, 0, 5),
                new Vec3i(1, 0, 3),
                new Vec3i(2, 0, 3)
        );
    }

    @Test
    public void trimFrom() {
        final Node
            lower = add(source, 0, 0, 1),
            up = add(lower, 0, 0, 2),
            middle = add(up, 0, 0, 3);

        add(add(middle, 0, 0, 4), 0, 0, 5);
        add(add(middle, 1, 0, 3), 2, 0, 3);
        add(add(lower, 1, 0, 1), 2, 0, 1);

        q.trimFrom(middle);

        assertFalse(up.assigned());
        assertFalse(lower.assigned());
        assertFalse(source.assigned());

        assertTrue(up.infecund() && up.orphaned());

        assertQueuePoints(
            new Vec3i(0, 0, 3),
            new Vec3i(0, 0, 4),
            new Vec3i(0, 0, 5),
            new Vec3i(1, 0, 3),
            new Vec3i(2, 0, 3)
        );
    }

    @Test
    public void trimVisited() {
        final Node
                lower = visited(source, 0, 0, 1),
                up = add(lower, 0, 0, 2),
                middle = visited(up, 0, 0, 3);

        final Node
            alphaChild = visited(middle, 0, 0, 4),
            betaChild = visited(middle, 1, 0, 3);

        add(alphaChild, 0, 0, 5);
        add(betaChild, 2, 0, 3);

        final Node branch = add(visited(lower, 1, 0, 1), 2, 0, 1);

        q.trimFrom(middle);

        assertFalse(up.visited());
        assertFalse(lower.visited());
        assertFalse(branch.visited());
        assertFalse(source.visited());
        assertTrue(alphaChild.visited());
        assertTrue(betaChild.visited());
        assertTrue(middle.visited());
    }

    @Test
    public void trimFromEnsureAssigned() {
        final Node
                lower = add(source, 0, 0, 1),
                up = add(lower, 0, 0, 2),
                middle = visited(up, 0, 0, 3);

        add(add(middle, 0, 0, 4), 0, 0, 5);
        add(add(middle, 1, 0, 3), 2, 0, 3);
        add(add(lower, 1, 0, 1), 2, 0, 1);

        q.trimFrom(middle);

        assertTrue(middle.assigned());

        assertQueuePoints(
                new Vec3i(0, 0, 4),
                new Vec3i(0, 0, 5),
                new Vec3i(1, 0, 3),
                new Vec3i(2, 0, 3),
                new Vec3i(0, 0, 3)
        );
    }

    @Test
    public void trimIncorrectDequeUsage() {
        final Node
            middle = visited(visited(source, 0, 0, 1), 0, 0, 2),
            adjacent = visited(visited(source, 1, 0, 1), 1, 0, 2),
            trunk = add(visited(adjacent, 1, 0, 3), 1, 0, 4);

        add(visited(middle, 0, 0, 3), 0, 0, 4);

        assertSame(trunk, q.dequeue());
        q.trimFrom(middle);

        assertTrue(adjacent.visited());
    }

    @Test
    public void addLimit() {
        int c;
        for (c = 0; c < Node.MAX_INDICES; ++c) {
            final Node node = new Node(new Vec3i(0, 0, c));
            node.remaining((c % (Node.MAX_PATH_DISTANCE - 1)) + 1);
            q.add(node);
        }

        final Node pivot = new Node(new Vec3i(1, 0, c));
        pivot.remaining(Node.MAX_PATH_DISTANCE);
        q.add(pivot);

        assertEquals(Node.MAX_INDICES - (int)(Node.MAX_INDICES * 0.1f), q.size());

        while (q.size() > 2)
            q.dequeue();

        assertSame(pivot, q.dequeue());
        q.dequeue();
        assertTrue(q.isEmpty());
    }

    @Test
    public void cullBranch() {
        final Node
            left = visited(source, -1, 0, 0),
            right = visited(source, +1, 0, 0),
            leftRoot = visited(left, -2, 0, 1),
            rightHead = add(right, +1, 0, 1),
            rightOutlier = add(right, +2, 0, 1),
            leftOutlier = add(leftRoot, -3, 0, 1),
            leftTertiary = visited(leftRoot, -3, 0, 2),
            leftHead = add(leftTertiary, -4, 0, 3);

        graph.cullBranchAt(leftRoot.key, q);

        assertQueuePoints(
            new Vec3i(-1, 0, 0),
            new Vec3i(2, 0, 1),
            new Vec3i(1, 0, 1)
        );

        assertTrue(rightHead.assigned());
        assertTrue(rightOutlier.assigned());

        assertFalse(left.orphaned());
        assertTrue(left.infecund());
        assertTrue(leftRoot.orphaned() && leftRoot.infecund());
        assertTrue(leftOutlier.orphaned() && leftOutlier.infecund());
        assertTrue(leftTertiary.orphaned() && leftTertiary.infecund());
        assertTrue(leftHead.orphaned() && leftHead.orphaned());

        assertFalse(leftRoot.visited());
        assertFalse(leftOutlier.visited());
        assertFalse(leftTertiary.visited());
        assertFalse(leftHead.visited());

        assertTrue(left.assigned());
        assertFalse(left.visited());
    }

    @Test
    public void cullBranchNoOp() {
        final Node
                right = visited(source, +1, 0, 0),
                rightHead = add(right, +1, 0, 1),
                rightOutlier = add(right, +2, 0, 1);

        final Vec3i candidate = new Vec3i(5, 6, 7);
        graph.cullBranchAt(candidate, q);

        assertQueuePoints(
                new Vec3i(+1, 0, 1),
                new Vec3i(2, 0, 1)
        );

        assertTrue(rightHead.assigned());
        assertTrue(rightOutlier.assigned());
    }

    @Test
    public void cullBranchRemoveNode() {
        final Node
                node = visited(source, +1, 0, 0);

        assertSame(node, graph.cachedPointAt(node.key));

        graph.cullBranchAt(node.key, q);

        assertNotSame(node, graph.cachedPointAt(node.key));
    }

    protected Node visited(int x, int y, int z) {
        Node node = graph.cachedPointAt(x, y, z);
        node.visited(true);
        return node;
    }

    protected Node visited(Node parent, int x, int y, int z) {
        Node cached = visited(x, y, z);
        cached.appendTo(parent, 0, 0);
        return cached;
    }

    protected Node add(int x, int y, int z) {
        Node p = visited(x, y, z);
        p.visited(false);
        q.add(p);
        return p;
    }

    protected Node add(Node parent, int x, int y, int z) {
        Node p = visited(x, y, z);
        p.visited(false);
        q.appendTo(p, parent, target.key);
        return p;
    }

    protected void assertQueueIndices() {
        List<Node> view = q.view();
        final int[]
            actual = new int[view.size()],
            expected = new int[actual.length];
        {
            int c = 0;
            for (Node p : view) {
                expected[c] = c;
                actual[c++] = p.index();
            }
        }

        assertArrayEquals(expected, actual);
    }

    protected void assertQueuePoints(Vec3i... expected) {
        assertQueueIndices();

        List<Node> view = q.view();
        final Vec3i[] actual = new Vec3i[view.size()];
        {
            int c = 0;
            for (Node p : view)
                actual[c++] = p.key;
        }

        assertArrayEquals(expected, actual);
    }
}
