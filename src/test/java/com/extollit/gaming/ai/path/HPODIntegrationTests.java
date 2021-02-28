package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.IPath;
import com.extollit.gaming.ai.path.model.IPathingEntity;
import com.extollit.gaming.ai.path.model.PathObject;
import com.extollit.gaming.ai.path.persistence.Persistence;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class HPODIntegrationTests {
    private @Mock IInstanceSpace instanceSpace;
    private @Mock IPathingEntity pathingEntity;

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
}
