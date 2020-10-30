package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.tuple.Pair;
import net.jqwik.api.*;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.Unique;
import net.jqwik.api.providers.TypeUsage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.extollit.collect.CollectionsExt.toList;
import static com.extollit.collect.CollectionsExt.zip;
import static java.util.Collections.sort;
import static org.junit.Assert.*;

public class VoxelOctTreeMapProperty {
    @Property
    public void leaf_expand_two(@ForAll @Size(2) List<@From("points") @IntRange(min = -100, max = +100) @Unique Vec3i> list) {
        final Vec3i
                init = list.get(0),
                second = list.get(1);
        final VoxelOctTreeMap<String> map = new VoxelOctTreeMap<>(String.class);

        map.put(init, "foo");
        map.put(second, "bar");

        assertEquals("foo", map.get(init));
        assertEquals("bar", map.get(second));
    }

    @Property
    public void ten_get(@ForAll @Size(10) List<@From("points") @IntRange(min = -100, max = +100) @Unique Vec3i> list,
                        @ForAll @Size(10) List<@From("names") @Unique String> names) {
        final VoxelOctTreeMap<String> map = mapFrom(list, names);

        for (Pair.Sealed<Vec3i, String> pair : zip(list, names))
            assertEquals(pair.right, map.get(pair.left));
    }

    @Property
    public void four_iterate(@ForAll @Size(4) List<@From("points") @IntRange(min = -100, max = +100) @Unique Vec3i> list,
                             @ForAll @Size(4) List<@From("names") @Unique String> names) {
        final VoxelOctTreeMap<String> map = mapFrom(list, names);

        final List<String> existing = toList(map);
        sort(existing);
        sort(names);
        assertEquals(names, existing);
    }

    @Property
    public void removal1(@ForAll @Size(min = 1, max = 50) List<@From("points") @IntRange(min = -100, max = +100) @Unique Vec3i> list,
                         @ForAll @IntRange(min =  1, max = 50) int index) {
        final VoxelOctTreeMap<Vec3i> map = mapFrom(list);

        index = index % list.size();

        final Vec3i p = list.get(index);

        assertEquals(p, map.get(p));
        assertEquals(p, map.remove(p));
        assertNull(map.get(p));
    }

    @Property
    public void removals(@ForAll @Size(min = 5, max = 50) List<@From("points") @IntRange(min = -100, max = +100) @Unique Vec3i> list,
                         @ForAll @Size(min = 2, max = 50) List<@IntRange(min =  1, max = 50) Integer> indices) {
        final VoxelOctTreeMap<Vec3i> map = mapFrom(list);

        for (int index : indices) {
            index = index % list.size();

            final Vec3i p = list.remove(index);

            assertEquals(p, map.get(p));
            assertEquals(p, map.remove(p));
            assertNull(map.get(p));

            if (list.isEmpty())
                return;
        }
    }

    @Property
    public void box_iterate(@ForAll @Size(min = 5, max = 50) List<@From("points") @Unique @IntRange(min = -200, max = +200) Vec3i> list,
                            @ForAll @From("boxes") @IntRange(min = -199, max = +199) IntAxisAlignedBox bounds) {
        final VoxelOctTreeMap<Vec3i> map = mapFrom(list);

        final Iterator<Vec3i> i = map.iterator(bounds);
        while (i.hasNext()) {
            final Vec3i p = i.next();
            assertTrue(bounds.contains(p));
        }
    }

    @Property
    public void outside(@ForAll @Size(10) List<@From("points") @Unique @IntRange(min = -100, max = +100) Vec3i> list,
                        @ForAll @Size(10) List<@From("names") @Unique String> names,
                        @ForAll @From("points") @Outside(x0 = -100, y0 = -100, z0 = -100, xN = +100, yN = +100, zN = +100) Vec3i p) {
        final VoxelOctTreeMap<String> map = mapFrom(list, names);

        assertNull(map.get(p));
    }

    @Property
    public void cull_outside(@ForAll @Size(20) List<@From("points") @Unique @IntRange(min = -200, max = +200) Vec3i> list,
                             @ForAll @From("boxes") @IntRange(min = -199, max = +199) IntAxisAlignedBox bounds) {
        final VoxelOctTreeMap<Vec3i> map = mapFrom(list);

        final List<Vec3i> culled = new LinkedList<>();

        map.cullOutside(
            bounds,
            (element, x, y, z) -> culled.add(new Vec3i(x, y, z))
        );

        for (Vec3i p : culled)
            assertTrue(!bounds.contains(p));
    }

    private VoxelOctTreeMap<Vec3i> mapFrom(final List<Vec3i> list) {
        final VoxelOctTreeMap<Vec3i> map = new VoxelOctTreeMap<>(Vec3i.class);

        for (Vec3i p : list)
            map.put(p, p);
        return map;
    }

    private VoxelOctTreeMap<String> mapFrom(final List<Vec3i> list, final List<String> names) {
        final VoxelOctTreeMap<String> map = new VoxelOctTreeMap<>(String.class);

        for (Pair.Sealed<Vec3i, String> pair : zip(list, names))
            map.put(pair.left, pair.right);
        return map;
    }

    @Provide
    final Arbitrary<Vec3i> points(final TypeUsage usage) {
        final Optional<IntRange> range = usage.findAnnotation(IntRange.class);
        final Optional<Outside> outside = usage.findAnnotation(Outside.class);
        final Arbitrary<Vec3i> baseArbitrary = pointArbitrary(range);

        return outside.map(
            o -> baseArbitrary.filter(
                p ->
                        p.x < o.x0() || p.x > o.xN()
                                ||
                        p.y < o.y0() || p.y > o.yN()
                                ||
                        p.z < o.z0() || p.z > o.zN()
            )
        ).orElse(baseArbitrary);
    }

    @Provide
    final Arbitrary<IntAxisAlignedBox> boxes(final TypeUsage usage) {
        final Optional<IntRange> range = usage.findAnnotation(IntRange.class);
        final Arbitrary<Vec3i> pointArbitrary = pointArbitrary(range);

        return
            Combinators.combine(
                    pointArbitrary,
                    pointArbitrary
            ).as(IntAxisAlignedBox::new);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Arbitrary<Vec3i> pointArbitrary(Optional<IntRange> range) {
        final IntegerArbitrary
                coordinate = range.map(r -> Arbitraries.integers().between(r.min(), r.max()))
                .orElse(Arbitraries.integers());

        return Combinators
                .combine(
                        coordinate,
                        coordinate,
                        coordinate
                )
                .as(Vec3i::new);
    }

    @Provide
    final Arbitrary<String> names() {
        return Arbitraries.of("foo", "bar", "gong", "zip", "tup", "boof", "waow", "jiss", "doyle", "brrr", "ska", "jenz", "koil", "fring", "ol", "dets", "pool", "kre", "io", "pen");
    }
}

@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@interface Outside {
    int x0();
    int y0();
    int z0();
    int xN();
    int yN();
    int zN();
}
