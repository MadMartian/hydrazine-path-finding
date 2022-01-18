package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Coords;
import com.extollit.gaming.ai.path.model.IPath;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.PathObject;
import com.extollit.gaming.ai.path.persistence.Persistence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class HPODIntegrationTests extends AbstractHydrazinePathFinderTests {
    private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    @Test
    public void testEntity() throws IOException {
        final HydrazinePathFinder pathFinder = Persistence.restore(contextClassLoader.getResourceAsStream("entity.hpod"), instanceSpace);

        assertNotNull(pathFinder);

        final Coords expectedSource = new Coords(-578, 77, 1117);
        assertTrue(pathFinder.unreachableFromSource(expectedSource, -577, 77, 1116));
        assertTrue(pathFinder.unreachableFromSource(expectedSource, -578, 77, 1116));
        assertTrue(pathFinder.unreachableFromSource(expectedSource, -579, 77, 1116));
        assertEquals(58, pathFinder.queue.size());
    }

    @Test
    public void indecision() throws IOException {
        final HydrazinePathFinder pathFinder = Persistence.restore(contextClassLoader.getResourceAsStream("indecision.hpod"), instanceSpace);
        final IPathingEntity subject = pathFinder.subject();

        IPath path = null;

        for (int c = 0; c < 8; ++c)
            path = pathFinder.updatePathFor(subject);

        assertTrue(PathObject.active(path));
        assertPath(path, new Coords(-332, 63, 67), new Coords(-333, 63, 67));

        path = pathFinder.updatePathFor(subject);
        assertNull(path);
    }

    @Test
    public void controlEmpty() throws IOException {
        final HydrazinePathFinder pathFinder = Persistence.restore(contextClassLoader.getResourceAsStream("empty.hpod"), instanceSpace);

        assertNull(pathFinder.currentTarget());
    }

    @Test
    public void fenceCornerStuck() throws IOException {
        final HydrazinePathFinder pathFinder = Persistence.restore(contextClassLoader.getResourceAsStream("fence-corner-stuck.hpod"), instanceSpace);
        final IPathingEntity subject = pathFinder.subject();

        IPath path = null;

        cornerFenceSouthEast(-7, 4, 11);
        latFence(-6, 4, 11);
        longFence(-7, 4, 12);
        for (int z = -10; z < 13; ++ z)
            for (int x = -8; x < -5; ++x)
                solid(x, 3, z);

        path = pathFinder.updatePathFor(subject);
        path = pathFinder.updatePathFor(subject);

        assertPath(path, new Coords(-7, 4, 11), new Coords(-8, 4, 11), new Coords(-8, 4, 12));
    }
}
