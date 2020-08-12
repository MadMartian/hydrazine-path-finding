package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SortedPointQueueTests {
    @Mock private NodeMap.IPointPassibilityCalculator calculator;

    private SortedPointQueue q;
    private NodeMap graph;
    private Node
        target,
        source;

    @Before
    public void setup() {
        this.q = new SortedPointQueue();
        this.graph = new NodeMap(calculator);
        this.target = this.graph.cachedPointAt(0, 0, 7);
        (this.source = visited(0, 0, 0)).target(this.target);
    }

    @Test
    public void trimFrom_Control() {
        this.q.clear();
        (this.source = add(0, 0, 3)).target(this.target);

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

        assertTrue(up.deleted());

        assertQueuePoints(
            new Vec3i(0, 0, 3),
            new Vec3i(0, 0, 4),
            new Vec3i(0, 0, 5),
            new Vec3i(1, 0, 3),
            new Vec3i(2, 0, 3)
        );
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
        q.appendTo(p, parent, target);
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
