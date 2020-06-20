package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.IBlockDescription;
import com.extollit.gaming.ai.path.model.IBlockObject;
import com.extollit.linalg.immutable.AxisAlignedBBox;

public class BlockObject implements IBlockObject {
    public AxisAlignedBBox bounds;
    public boolean fenceLike, climbable, door, impeding, fullyBounded, liquid, incinerating;

    @Override
    public AxisAlignedBBox bounds() {
        return this.bounds;
    }

    @Override
    public boolean isFenceLike() {
        return this.fenceLike;
    }

    @Override
    public boolean isClimbable() {
        return this.climbable;
    }

    @Override
    public boolean isDoor() {
        return this.door;
    }

    @Override
    public boolean isImpeding() {
        return this.impeding;
    }

    @Override
    public boolean isFullyBounded() {
        return this.fullyBounded;
    }

    @Override
    public boolean isLiquid() {
        return this.liquid;
    }

    @Override
    public boolean isIncinerating() {
        return this.incinerating;
    }
}
