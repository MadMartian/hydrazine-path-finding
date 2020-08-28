package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;
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
    public static void assertPathNot(IPath path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        try {
            compareCoordinates(coordinates, nodesFrom(path));
        } catch (AssertionError e) {
            return;
        }
        throw new AssertionError(MessageFormat.format("Path should not equal {0}", Arrays.asList(coordinates)));
    }

    public static void assertPath(IPath path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        compareCoordinates(coordinates, nodesFrom(path));
    }

    private static void compareCoordinates(Vec3i[] coordinates, INode[] nodes) {
        final Vec3i[] otherCoords = new Vec3i[nodes.length];
        for (int c = 0; c < otherCoords.length; ++c)
            otherCoords[c] = nodes[c].coordinates();

        assertArrayEquals(coordinates, otherCoords);
    }

    private static INode[] nodesFrom(IPath path) {
        return CollectionsExt.toList(path).toArray(new INode[path.length()]);
    }
}
