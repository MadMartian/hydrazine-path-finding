package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeMapTests {
    private NodeMap nodeMap;

    @Mock
    private NodeMap.IPointPassibilityCalculator calculator;

    @Before
    public void setup() {
        this.nodeMap = new NodeMap(this.calculator);
    }

    @Test
    public void getGet() {
        final Node
            node = this.nodeMap.cachedPointAt(1, 2, 3),
            fetched = this.nodeMap.cachedPointAt(1, 2, 3);

        assertSame(node, fetched);
    }

    @Test
    public void getFreshen() {
        final Node
            node = this.nodeMap.cachedPointAt(1, 2, 3),
            fetched = this.nodeMap.freshened(node);

        assertSame(node, fetched);
    }

    @Test
    public void getDeleteFreshen() {
        final Node node = this.nodeMap.cachedPointAt(1, 2, 3);

        node.delete();

        final Node fetched = this.nodeMap.freshened(node);

        assertNull(fetched);
    }

    @Test
    public void passibleControl() {
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        verify(this.calculator).passiblePointNear(eq(new Vec3i(1, 2, 3)), isNull(Vec3i.class));
    }

    @Test
    public void passibleCached() {
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        final Node
            node = this.nodeMap.cachedPassiblePointNear(1, 2, 3),
            cached = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(node, cached);
    }

    @Test
    public void passibleCachedDeleteGet() {
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        final Node node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        node.delete();

        final Node cached = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertNotSame(node, cached);
    }

    @Test
    public void redundancyExistency() {
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        final Node
            node = this.nodeMap.cachedPassiblePointNear(1, 2, 3),
            existing = this.nodeMap.cachedPassiblePointNear(3, 4, 5);

        assertSame(node, existing);
    }

    @Test
    public void redundancyDeleteOriginal() {
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        Node node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        node.delete();

        when(this.calculator.passiblePointNear(any(), any())).thenReturn(new Node(new Vec3i(1, 2, 3)));

        final Node existing = this.nodeMap.cachedPassiblePointNear(3, 4, 5);

        assertNotSame(node, existing);

        node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(existing, node);
    }

    @Test
    public void alwaysVolatile() {
        final Node initial = new Node(new Vec3i(1, 2, 3));

        initial.passibility(Passibility.risky);
        initial.volatile_(true);

        when(this.calculator.passiblePointNear(any(), any())).thenReturn(initial);

        final Node result0 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        final Node second = new Node(new Vec3i(1, 2, 3));

        second.passibility(Passibility.dangerous);
        second.volatile_(true);

        when(this.calculator.passiblePointNear(any(), any())).thenReturn(second);

        final Node result1 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result0, result1);
        assertSame(result1, initial);
        assertEquals(Passibility.dangerous, result1.passibility());
        assertTrue(result1.volatile_());
    }

    @Test
    public void becomeStable() {
        final Node initial = new Node(new Vec3i(1, 2, 3));

        initial.passibility(Passibility.risky);
        initial.volatile_(true);

        when(this.calculator.passiblePointNear(any(), any())).thenReturn(initial);

        final Node result0 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        final Node second = new Node(new Vec3i(1, 2, 3));

        second.passibility(Passibility.dangerous);
        second.volatile_(false);

        when(this.calculator.passiblePointNear(any(), any())).thenReturn(second);

        final Node result1 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result0, result1);
        assertSame(result1, initial);
        assertEquals(Passibility.dangerous, result1.passibility());
        assertFalse(result1.volatile_());

        final Node third = new Node(new Vec3i(1, 2, 3));

        third.passibility(Passibility.risky);
        third.volatile_(true);
        when(this.calculator.passiblePointNear(any(), any())).thenReturn(third);

        final Node result2 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result2, result1);
        assertFalse(result2.volatile_());
    }
}
