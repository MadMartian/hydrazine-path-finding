package com.extollit.gaming.ai.path.model;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class PathObjectUtil {
    public static PathObject pathObject(Coords... coordinates) {
        final Node[] nodes = new Node[coordinates.length];
        for (int c = 0; c < nodes.length; ++c)
            nodes[c] = new Node(coordinates[c], Passibility.passible);

        return new PathObject(1, nodes);
    }
    public static void assertPathNot(IPath path, Coords... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        try {
            compareCoordinates(coordinates, nodesFrom(path));
        } catch (AssertionError e) {
            return;
        }
        throw new AssertionError(MessageFormat.format("Path should not equal {0}", Arrays.asList(coordinates)));
    }

    public static void assertPath(IPath path, Coords... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        compareCoordinates(coordinates, nodesFrom(path));
    }

    private static void compareCoordinates(Coords[] coordinates, INode[] nodes) {
        final Coords[] otherCoords = new Coords[nodes.length];
        for (int c = 0; c < otherCoords.length; ++c)
            otherCoords[c] = nodes[c].coordinates();

        assertArrayEquals(coordinates, otherCoords);
    }

    public static List<INode> listify(IPath path) {
        final List<INode> result = new LinkedList<>();
        for (INode node : path)
            result.add(node);

        return result;
    }

    private static INode[] nodesFrom(IPath path) {
        final INode[] result = new INode[path.length()];
        int c = 0;
        for (INode node : path)
            result[c++] = node;

        return result;
    }
}
