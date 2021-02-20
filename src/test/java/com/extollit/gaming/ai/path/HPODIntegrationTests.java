package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.persistence.Persistence;
import com.extollit.linalg.immutable.Vec3i;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class HPODIntegrationTests {
    private @Mock IInstanceSpace instanceSpace;

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
}
