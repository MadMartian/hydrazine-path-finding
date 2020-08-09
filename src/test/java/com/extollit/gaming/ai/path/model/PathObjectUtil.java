package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import java.text.MessageFormat;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class PathObjectUtil {
    public static void assertPathNot(PathObject path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        try {
            assertArrayEquals(coordinates, path.points);
        } catch (AssertionError e) {
            return;
        }
        throw new AssertionError(MessageFormat.format("Path should not equal {0}", Arrays.asList(coordinates)));
    }
    public static void assertPath(PathObject path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        assertArrayEquals(coordinates, path.points);
    }
}
