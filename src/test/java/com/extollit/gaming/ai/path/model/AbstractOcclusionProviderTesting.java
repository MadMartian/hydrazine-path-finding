package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.TestingBlocks;
import org.junit.Before;
import org.mockito.Mock;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class AbstractOcclusionProviderTesting {
    @Mock
    protected IColumnarSpace centerSpace;
    @Mock
    protected IInstanceSpace instanceSpace;

    public static void assertDoorway(IOcclusionProvider occlusionProvider, boolean open, int x, int y, int z) {
        final byte
                bottom = occlusionProvider.elementAt(x, y, z),
                top = occlusionProvider.elementAt(x, y + 1, z);

        assertTrue(Logic.doorway.in(top));
        assertTrue(Logic.doorway.in(bottom));
        final Element expectedElement = open ? Element.air : Element.earth;
        assertTrue(expectedElement.in(top));
        assertTrue(expectedElement.in(bottom));
    }

    @Before
    public void setup() {
        when(centerSpace.instance()).thenReturn(instanceSpace);
    }

    protected void door(boolean open, final int x, final int y, final int z) {
        final TestingBlocks.Door door = TestingBlocks.door;

        blockAt(x, y, z, door);
        blockAt(x, y + 1, z, door);

        door.open = open;
    }

    protected void blockAt(int x, int y, int z, IBlockObject blockObject) {
        when(this.centerSpace.blockAt(x, y, z)).thenReturn(blockObject);
        when(this.instanceSpace.blockObjectAt(x, y, z)).thenReturn(blockObject);
    }
}
