package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Element;
import com.extollit.gaming.ai.path.model.IPath;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Before;
import org.junit.Test;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class BuoyantHydrazinePathFinderTests extends AbstractHydrazinePathFinderTests {
    @Before
    public void setup() {
        when(super.capabilities.cautious()).thenReturn(true);

        when(super.capabilities.aquatic()).thenReturn(true);
        when(super.capabilities.swimmer()).thenReturn(true);

        super.setup();

        pathFinder.schedulingPriority(125, 125);
    }

    private void waterEverywhere() {
        when(occlusionProvider.elementAt(anyInt(), anyInt(), anyInt())).thenReturn(Element.water.mask);
    }

    @Test
    public void swimUp() {
        waterEverywhere();

        solid(0, -1, 0);
        solid(0, -1, 1);
        solid(0, -1, 2);
        solid(0, -1, 3);
        solid(0, -1, 4);

        final IPath path = pathFinder.initiatePathTo(0, 5, 4);

        assertNotNull(path);
        assertEquals(new Vec3i(0, 5, 4), path.last().coordinates());
    }

    @Test
    public void headSpace() {
        waterEverywhere();

        solid(0, 1, 2);

        final IPath path = pathFinder.initiatePathTo(0, 0, 4);

        assertPath(
                path,
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(-1, 0, 1),
                new Vec3i(-1, 0, 2),
                new Vec3i(-1, 0, 3),
                new Vec3i(0, 0, 3),
                new Vec3i(0, 0, 4)
        );
    }

    @Test
    public void beachedAggressor() {
        when(super.capabilities.cautious()).thenReturn(false);

        water(0, -1, 0);
        solid(1, -1, 0);

        pos(0, -1, 0);

        final IPath path = pathFinder.initiatePathTo(1, 0, 0);
        assertPath(
                path,
                new Vec3i(0, -1, -0),
                new Vec3i(0, 0, 0),
                new Vec3i(1, 0, 0)
        );
    }
}
