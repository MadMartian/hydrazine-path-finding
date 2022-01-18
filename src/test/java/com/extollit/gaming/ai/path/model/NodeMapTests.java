package com.extollit.gaming.ai.path.model;

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

    @Mock private IInstanceSpace instanceSpace;
    @Mock private IOcclusionProviderFactory occlusionProviderFactory;
    @Mock private INodeCalculator calculator;
    @Mock private IGraphNodeFilter filter;

    @Before
    public void setup() {
        this.nodeMap = new NodeMap(this.instanceSpace, new TestNodeCalculatorDecorator(this.calculator), this.occlusionProviderFactory);
    }

    @Test
    public void getGet() {
        final Node
            node = this.nodeMap.cachedPointAt(1, 2, 3),
            fetched = this.nodeMap.cachedPointAt(1, 2, 3);

        assertSame(node, fetched);
    }

    @Test
    public void getRemoveGet() {
        final Node node = this.nodeMap.cachedPointAt(1, 2, 3);

        node.index(42);

        final boolean removed = this.nodeMap.remove(1, 2, 3);

        final Node fetched = this.nodeMap.cachedPointAt(1, 2, 3);

        assertFalse(node.assigned());
        assertTrue(removed);
        assertNotSame(node, fetched);
    }

    @Test
    public void passibleControl() {
        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(new Node(1, 2, 3));

        this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        verify(this.calculator).passibleNodeNear(eq(1), eq(2), eq(3), isNull(Coords.class), any());
    }

    @Test
    public void passibleCached() {
        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(new Node(1, 2, 3));

        final Node
            node = this.nodeMap.cachedPassiblePointNear(1, 2, 3),
            cached = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(node, cached);
    }

    @Test
    public void redundancyExistency() {
        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(new Node(1, 2, 3));

        final Node
            node = this.nodeMap.cachedPassiblePointNear(1, 2, 3),
            existing = this.nodeMap.cachedPassiblePointNear(3, 4, 5);

        assertSame(node, existing);
    }

    @Test
    public void redundancyRemoveOriginal() {
        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(new Node(1, 2, 3));

        Node node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        final boolean removed = this.nodeMap.remove(1, 2, 3);

        assertTrue(removed);

        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(new Node(1, 2, 3));

        final Node existing = this.nodeMap.cachedPassiblePointNear(3, 4, 5);

        assertNotSame(node, existing);

        node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(existing, node);
    }

    @Test
    public void alwaysVolatile() {
        final Node initial = new Node(1, 2, 3);

        initial.passibility(Passibility.risky);
        initial.volatile_(true);

        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(initial);

        final Node result0 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        final Node second = new Node(1, 2, 3);

        second.passibility(Passibility.dangerous);
        second.volatile_(true);

        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(second);

        final Node result1 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result0, result1);
        assertSame(result1, initial);
        assertEquals(Passibility.dangerous, result1.passibility());
        assertTrue(result1.volatile_());
    }

    @Test
    public void becomeStable() {
        final Node initial = new Node(1, 2, 3);

        initial.passibility(Passibility.risky);
        initial.volatile_(true);

        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(initial);

        final Node result0 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        final Node second = new Node(1, 2, 3);

        second.passibility(Passibility.dangerous);
        second.volatile_(false);

        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(second);

        final Node result1 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result0, result1);
        assertSame(result1, initial);
        assertEquals(Passibility.dangerous, result1.passibility());
        assertFalse(result1.volatile_());

        final Node third = new Node(1, 2, 3);

        third.passibility(Passibility.risky);
        third.volatile_(true);
        when(this.calculator.passibleNodeNear(anyInt(), anyInt(), anyInt(), any(), any())).thenReturn(third);

        final Node result2 = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        assertSame(result2, result1);
        assertFalse(result2.volatile_());
    }

    @Test
    public void filter() {
        when(this.filter.mapPassibility(any(INode.class))).thenReturn(Passibility.impassible);

        this.nodeMap.filter(this.filter);

        final INode node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        verify(this.filter).mapPassibility(node);

        assertEquals(Passibility.impassible, node.passibility());
    }


    @Test
    public void filterNoMap() {
        this.nodeMap.filter(this.filter);

        final INode node = this.nodeMap.cachedPassiblePointNear(1, 2, 3);

        verify(this.filter).mapPassibility(node);

        assertEquals(Passibility.passible, node.passibility());
    }
}
