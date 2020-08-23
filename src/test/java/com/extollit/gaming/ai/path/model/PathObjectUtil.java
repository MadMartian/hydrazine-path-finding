package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import java.text.MessageFormat;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class PathObjectUtil {
    public static PathObject pathObject(Vec3i... coordinates) {
        final Node[] nodes = new Node[coordinates.length];
        for (int c = 0; c < nodes.length; ++c)
            nodes[c] = new Node(coordinates[c], Passibility.passible);

        return new PathObject(1, nodes);
    }
    public static void assertPathNot(PathObject path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        try {
            compareCoordinates(coordinates, path.nodes);
        } catch (AssertionError e) {
            return;
        }
        throw new AssertionError(MessageFormat.format("Path should not equal {0}", Arrays.asList(coordinates)));
    }

    public static void assertPath(PathObject path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        compareCoordinates(coordinates, path.nodes);
    }

    private static void compareCoordinates(Vec3i[] coordinates, Node[] nodes) {
        final Vec3i[] otherCoords = new Vec3i[nodes.length];
        for (int c = 0; c < otherCoords.length; ++c)
            otherCoords[c] = nodes[c].key;

        assertArrayEquals(coordinates, otherCoords);
    }
}
