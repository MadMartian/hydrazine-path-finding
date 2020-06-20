package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.linalg.immutable.AxisAlignedBBox;

public class TestingBlocks {
    public static final IBlockObject
            stone = new Stone(),
            wall = new Wall(),
            lava = new Lava(),
            air = new Air(),
            ladder = new Ladder(),
            slabUp = new SlabUp(),
            slabDown = new SlabDown(),
            torch = air;

    public static final Door
            door = new Door();
    public static final FenceGate
            fenceGate = new FenceGate();

    private static class AbstractBlockDescription implements IBlockObject {
        @Override
        public boolean isFenceLike() {
            return false;
        }

        @Override
        public boolean isClimbable() {
            return false;
        }

        @Override
        public boolean isDoor() {
            return false;
        }

        @Override
        public boolean isImpeding() {
            return false;
        }

        @Override
        public boolean isFullyBounded() {
            return false;
        }

        @Override
        public boolean isLiquid() {
            return false;
        }

        @Override
        public boolean isIncinerating() {
            return false;
        }

        @Override
        public AxisAlignedBBox bounds() {
            return new AxisAlignedBBox(
                0, 0, 0,
                1, isFenceLike() ? 1.5f : 0, 1
            );
        }
    }

    public static class Stone extends AbstractBlockDescription {
        @Override
        public boolean isImpeding() {
            return true;
        }

        @Override
        public boolean isFullyBounded() {
            return true;
        }

        @Override
        public AxisAlignedBBox bounds() {
            return new AxisAlignedBBox(
                    0, 0, 0,
                    1, 1, 1
            );
        }
    }

    public static class Wall extends Stone {
        @Override
        public boolean isFenceLike() {
            return true;
        }

        @Override
        public boolean isFullyBounded() {
            return false;
        }
    }

    public static final class Lava extends AbstractBlockDescription {
        @Override
        public boolean isLiquid() {
            return true;
        }

        @Override
        public boolean isIncinerating() {
            return true;
        }
    }

    public static final class Air extends AbstractBlockDescription {}

    private static abstract class AbstractDoor extends AbstractBlockDescription {
        public boolean open;

        @Override
        public final boolean isDoor() {
            return true;
        }

        @Override
        public final boolean isImpeding() {
            return !open;
        }

        @Override
        public AxisAlignedBBox bounds() {
            return new AxisAlignedBBox(
                    0, 0, 0,
                    1, 1, 1
            );
        }
    }

    public static final class FenceGate extends AbstractDoor {
        @Override
        public boolean isFenceLike() {
            return true;
        }
    }

    public static final class Door extends AbstractDoor {}

    public static final class Ladder extends AbstractBlockDescription {
        @Override
        public boolean isClimbable() {
            return true;
        }
    }

    public static final class SlabDown extends AbstractBlockDescription {
        @Override
        public boolean isImpeding() {
            return true;
        }
        @Override
        public AxisAlignedBBox bounds() {
            return new AxisAlignedBBox(
                    0, 0, 0,
                    1, 0.5, 1
            );
        }
    }

    public static final class SlabUp extends AbstractBlockDescription {
        @Override
        public boolean isImpeding() {
            return true;
        }

        @Override
        public AxisAlignedBBox bounds() {
            return new AxisAlignedBBox(
                    0, 0.5, 0,
                    1, 1, 1
            );
        }
    }
}
