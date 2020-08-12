package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NodeTests {
    @Test
    public void copyPreserveState() {
        final Node original = new Node(new Vec3i(0, 1, 2));
        original.volatile_(true);
        original.passibility(Passibility.risky);
        original.index(42);
        original.length(79);
        original.delta(10);
        original.journey(49);
        original.delete();

        final Node copy = original.pointCopy();

        assertEquals(copy.passibility(), original.passibility());
        assertEquals(copy.volatile_(), original.volatile_());

        assertFalse(copy.assigned());
        assertEquals(0, copy.length());
        assertEquals(0, copy.delta());
        assertEquals(0, copy.journey());
        assertFalse(copy.deleted());
    }
}
