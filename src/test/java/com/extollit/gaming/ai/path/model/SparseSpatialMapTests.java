package com.extollit.gaming.ai.path.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.Unique;

import java.util.*;

public class SparseSpatialMapTests {
    @Property(tries = 50000)
    public boolean complex(@ForAll @Size(min = 10, max = 50) List<@From("value") @Unique TestNodeValue> init,
                           @ForAll @Size(min = 2, max = 50) List<@From("coord") @Unique Coords> remove,
                           @ForAll @Size(min = 2, max = 20)  List<@From("value") @Unique TestNodeValue> more,
                           @ForAll @Size(min = 2, max = 20) List<@From("coord") @Unique Coords> nerf) {
        final SparseSpatialMap<TestNodeValue> sparse = new SparseSpatialMap<>();
        final Map<Coords, TestNodeValue> control = new HashMap<>();

        for (TestNodeValue value : init)
            put(sparse, control, value);

        for (Coords coord : remove)
            remove(sparse, control, coord);

        for (TestNodeValue value : more)
            put(sparse, control, value);

        for (Coords coord : nerf)
            remove(sparse, control, coord);

        final Collection<TestNodeValue>
            actual = new ArrayList<>(sparse.values()),
            expected = control.values();

        return actual.containsAll(expected) && expected.containsAll(actual);
    }

    private void remove(SparseSpatialMap<TestNodeValue> sparse, Map<Coords, TestNodeValue> control, Coords coord) {
        sparse.remove(coord.x, coord.y, coord.z);
        control.remove(coord);
    }

    private void put(SparseSpatialMap<TestNodeValue> sparse, Map<Coords, TestNodeValue> control, TestNodeValue value) {
        sparse.put(value.p.x, value.p.y, value.p.z, value);
        control.put(value.p, value);
    }

    @Provide
    Arbitrary<Coords> coord() {
        return Combinators.combine(
            Arbitraries.integers().between(-100, +200),
            Arbitraries.integers().between(-100, +200),
            Arbitraries.integers().between(-100, +200)
        ).as(Coords::new);
    }

    @Provide
    Arbitrary<TestNodeValue> value() {
        return coord().map(TestNodeValue::new);
    }
}

final class TestNodeValue implements INode {
    public final Coords p;

    public TestNodeValue(Coords coordinates) {
        this.p = coordinates;
    }

    @Override
    public Coords coordinates() {
        return this.p;
    }

    @Override
    public Passibility passibility() {
        return Passibility.impassible;
    }

    @Override
    public Gravitation gravitation() {
        return Gravitation.grounded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestNodeValue value = (TestNodeValue) o;

        return p.equals(value.p);
    }

    @Override
    public int hashCode() {
        return p.hashCode();
    }
}
