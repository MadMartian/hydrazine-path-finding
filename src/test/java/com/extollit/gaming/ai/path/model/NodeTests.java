package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeTests {
    private Node root;

    @Before
    public void setup() {
        this.root = new Node(new Vec3i(1, 2, 3));
    }

    @Test
    public void reset() {
        final Node node = this.root;

        node.length(7);
        node.delta(11);
        node.journey(47);
        node.passibility(Passibility.risky);
        node.index(42);
        node.volatile_(true);
        node.visited(true);

        node.reset();

        assertEquals(0, node.length());
        assertEquals(0, node.delta());
        assertEquals(0, node.journey());
        assertEquals(Passibility.risky, node.passibility());
        assertFalse(node.assigned());
        assertFalse(node.visited());
        assertTrue(node.volatile_());
    }

    @Test
    public void sterilize() {
        final Node
            alphaChild = new Node(new Vec3i(2, 2, 3)),
            betaChild = new Node(new Vec3i(0, 2, 3));

        alphaChild.appendTo(this.root, 0, 0);
        betaChild.appendTo(this.root, 0, 0);

        assertSame(this.root, alphaChild.up());
        assertSame(this.root, betaChild.up());

        assertFalse(alphaChild.orphaned());
        assertFalse(betaChild.orphaned());

        this.root.sterilize();

        assertTrue(alphaChild.orphaned());
        assertTrue(betaChild.orphaned());
    }

    @Test
    public void orphan() {
        final Node
                child = new Node(new Vec3i(2, 2, 3));

        child.appendTo(this.root, 0, 0);

        assertSame(this.root, child.up());
        assertFalse(child.orphaned());

        child.orphan();

        assertTrue(child.orphaned());
        assertNull(child.up());
        assertTrue(this.root.infecund());
    }
}
