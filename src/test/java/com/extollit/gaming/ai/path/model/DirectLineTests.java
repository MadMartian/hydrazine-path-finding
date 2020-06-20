package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DirectLineTests {
    @Test
    public void advanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 2),
                new Vec3i(4, 0, 2),
                new Vec3i(3, 0, 3)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(7, i);
    }

    @Test
    public void fatAdvanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(3, 0, 2),
                new Vec3i(3, 0, 3),
                new Vec3i(4, 0, 3)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(6, i);
    }

    @Test
    public void veryFatAdvanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(4, 0, 3),
                new Vec3i(3, 0, 4),
                new Vec3i(3, 0, 3),
                new Vec3i(3, 0, 4)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(4, i);
    }

    @Test
    public void fullyAdvanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 2),
                new Vec3i(6, 0, 2),
                new Vec3i(6, 0, 3)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(9, i);
    }

    @Test
    public void smallAdvanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(1, 0, 0),
                new Vec3i(1, 0, -1),
                new Vec3i(2, 0, -1),
                new Vec3i(2, 0, 0),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 3),
                new Vec3i(6, 0, 4),
                new Vec3i(5, 0, 5),
                new Vec3i(4, 0, 6)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(2, i);
    }

    @Test
    public void noAdvanceDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, -1),
                new Vec3i(2, 0, -1),
                new Vec3i(2, 0, 0),
                new Vec3i(3, 0, 1),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 3),
                new Vec3i(6, 0, 4),
                new Vec3i(5, 0, 5),
                new Vec3i(4, 0, 6)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(1, i);
    }

    @Test
    public void angleThenUp() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, -2),
                new Vec3i(-1, 0, -2),
                new Vec3i(-1, 0, -3),
                new Vec3i(-1, 0, -4),
                new Vec3i(-2, 0, -4),
                new Vec3i(-2, 0, -5),
                new Vec3i(-2, 0, -6),
                new Vec3i(-2, 0, -7),
                new Vec3i(-2, 0, -8),
                new Vec3i(-2, 0, -9),
                new Vec3i(-2, 0, -10),
                new Vec3i(-2, 0, -11),
                new Vec3i(-2, 0, -12)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(8, i);
    }

    @Test
    public void softAngleThenHardAngle() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, -2),
                new Vec3i(-1, 0, -2),
                new Vec3i(-1, 0, -3),
                new Vec3i(-1, 0, -4),
                new Vec3i(-2, 0, -4),
                new Vec3i(-2, 0, -5),
                new Vec3i(-2, 0, -6),
                new Vec3i(-3, 0, -6),
                new Vec3i(-3, 0, -7),
                new Vec3i(-4, 0, -7),
                new Vec3i(-4, 0, -8),
                new Vec3i(-5, 0, -8),
                new Vec3i(-5, 0, -9)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(10, i);
    }

    @Test
    public void noAdvanceEll() {
        final PathObject path = new PathObject(
                1,
                new Vec3i(-2, 4, 9),
                new Vec3i(-2, 4, 8),
                new Vec3i(-2, 4, 7),
                new Vec3i(-2, 4, 6),
                new Vec3i(-2, 4, 5),
                new Vec3i(-3, 4, 5),
                new Vec3i(-4, 4, 5),
                new Vec3i(-5, 4, 5),
                new Vec3i(-6, 4, 5)
        );

        final int i = path.directLine(0, 8);
        assertEquals(4, i);
    }

    @Test
    public void nonTaxiCab() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(1, 0, 1),
                new Vec3i(2, 0, 1),
                new Vec3i(3, 0, 2),
                new Vec3i(4, 0, 2),
                new Vec3i(5, 0, 3),
                new Vec3i(6, 0, 4),
                new Vec3i(7, 0, 5)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(4, i);
    }

    @Test
    public void diagonal() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(1, 0, 1),
                new Vec3i(2, 0, 2),
                new Vec3i(3, 0, 3),
                new Vec3i(4, 0, 4),
                new Vec3i(4, 0, 5),
                new Vec3i(5, 0, 6)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(2, i);
    }

    @Test
    public void slenderDirectLine() {
        final PathObject pathObject = new PathObject(
                1,
                new Vec3i(+1, 0, 0),
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(-2, 0, 0),
                new Vec3i(-3, 0, 0),
                new Vec3i(-3, 0, 1),
                new Vec3i(-4, 0, 1),
                new Vec3i(-5, 0, 1),
                new Vec3i(-6, 0, 1),
                new Vec3i(-7, 0, 1),
                new Vec3i(-7, 0, 2),
                new Vec3i(-8, 0, 2),
                new Vec3i(-9, 0, 2),
                new Vec3i(-10, 0, 2),
                new Vec3i(-10, 0, 3),
                new Vec3i(-9, 0, 3),
                new Vec3i(-8, 0, 3),
                new Vec3i(-7, 0, 3)
        );

        final int i = pathObject.directLine(0, pathObject.length());

        assertEquals(13, i);
    }

    @Test
    public void horseshoe() {
        final PathObject path = new PathObject(
            1,
            new Vec3i(1, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, -2)
        );

        final int i = path.directLine(0, path.length());

        assertEquals(3, i);
    }

    @Test
    public void subtleEll () {
        final PathObject path = new PathObject(
                1,
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, 1),
                new Vec3i(2, 0, 2),
                new Vec3i(3, 0, 2),
                new Vec3i(4, 0, 2)
        );

        final int i = path.directLine(0, path.length());

        assertEquals(2, i);
    }
}
