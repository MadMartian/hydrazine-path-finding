package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IPath;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.PathObject;
import com.extollit.gaming.ai.path.persistence.Persistence;
import com.extollit.linalg.immutable.Vec3i;
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

        final Vec3i expectedSource = new Vec3i(-578, 77, 1117);
        assertTrue(pathFinder.unreachableFromSource(expectedSource, new Vec3i(-577, 77, 1116)));
        assertTrue(pathFinder.unreachableFromSource(expectedSource, new Vec3i(-578, 77, 1116)));
        assertTrue(pathFinder.unreachableFromSource(expectedSource, new Vec3i(-579, 77, 1116)));
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
        assertPath(path, new Vec3i(-332, 63, 67), new Vec3i(-333, 63, 67));

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

        assertPath(path, new Vec3i(-7, 4, 11), new Vec3i(-8, 4, 11), new Vec3i(-8, 4, 12));
    }
}
