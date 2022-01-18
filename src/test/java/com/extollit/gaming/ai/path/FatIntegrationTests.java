package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.Coords;
import com.extollit.gaming.ai.path.model.IPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.extollit.gaming.ai.path.model.PathObjectUtil.assertPath;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class FatIntegrationTests extends AbstractHydrazinePathFinderTests {
    @Test
    public void cornerStepDown() {
        diver();

        solid(0, -1, 0);
        solid(-1, -1, 0);
        solid(+1, -1, 0);
        solid(0, -2, -1);
        solid(-1, -2, -1);
        solid(+1, -1, -1);
        solid(0, -2, -2);
        solid(-1, -2, -2);
        solid(+1, -2, -2);

        pos(super.pathingEntity, 0, 0, 0);

        final IPath path = pathFinder.initiatePathTo(0, 0, -2);
        assertNotNull(path);
        final Coords[] expectedPath = {
                ORIGIN,
                new Coords(0, -1, -1),
                new Coords(0, -1, -2)
        };
        assertPath(path, expectedPath);
    }
}
