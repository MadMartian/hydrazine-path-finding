package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.pathObject;
import static org.junit.Assert.*;

public class IncompletePathTests {
    @Test
    public void truncateState() {
        final IncompletePath path = new IncompletePath(new Node(1, 2, 3));

        path.truncateTo(0);

        assertEquals(0, path.length());
        assertTrue(path.done());
        assertFalse(path.iterator().hasNext());

        path.untruncate();

        assertEquals(1, path.length());
        assertFalse(path.done());
        assertTrue(path.iterator().hasNext());
    }

    @Test
    public void truncatedComparisons() {
        final Node node = new Node(1, 2, 3);
        final IncompletePath
            alpha = new IncompletePath(node),
            beta = new IncompletePath(node);

        assertEquals(alpha, beta);
        alpha.truncateTo(0);
        assertNotEquals(alpha, beta);
        assertFalse(alpha.sameAs(beta));
        alpha.untruncate();
        assertEquals(alpha, beta);
        assertTrue(alpha.sameAs(beta));
        beta.truncateTo(0);
        assertNotEquals(alpha, beta);
        assertFalse(alpha.sameAs(beta));
        beta.untruncate();
        assertEquals(alpha, beta);
        assertTrue(alpha.sameAs(beta));
        alpha.truncateTo(0);
        beta.truncateTo(0);
        assertEquals(alpha, beta);
        assertTrue(alpha.sameAs(beta));
    }

    @Test
    public void sameDisjoint() {
        final PathObject path = pathObject(new Vec3i(7, 8, 9));
        final IncompletePath single = new IncompletePath(new Node(7, 8, 9));

        path.i = 2;
        assertTrue(single.sameAs(path));
        path.i++;
        single.truncateTo(0);
        assertTrue(single.sameAs(path));
    }
}
