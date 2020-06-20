package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class PathObjectUtil {
    public static void assertPath(PathObject path, Vec3i... coordinates) {
        if (coordinates == null || coordinates.length == 0)
            assertNull(path);

        assertArrayEquals(coordinates, path.points);
    }
}
