package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.AxisAlignedBBox;
import com.extollit.linalg.immutable.Vec3i;

import static com.extollit.gaming.ai.path.PassibilityHelpers.impedesMovement;
import static java.lang.Math.floor;

abstract class AbstractNodeCalculator implements INodeCalculator {
    protected final IInstanceSpace instanceSpace;
    protected IPathingEntity.Capabilities capabilities;
    protected int discreteSize, tall;
    protected float actualSize;

    public AbstractNodeCalculator(IInstanceSpace instanceSpace) {
        this.instanceSpace = instanceSpace;
    }

    protected static boolean swimmingRequiredFor(byte flags) {
        return Element.water.in(flags) || (Element.fire.in(flags) && !Logic.fuzzy.in(flags));
    }

    @Override
    public final void applySubject(IPathingEntity subject) {
        this.actualSize = subject.width();
        this.discreteSize = (int)floor(subject.width() + 1);
        this.tall = (int)floor(subject.height() + 1);
        this.capabilities = subject.capabilities();
    }

    protected final Passibility verticalClearanceAt(FlagSampler sampler, int max, byte flags, Passibility passibility, int dy, int x, int y, int z, final float minPartY) {
        byte clearanceFlags = flags;
        final int
                yMax = y + max,
                yN = Math.max(y, y - dy) + this.tall,
                yNa = yN - 1;
        int yt = y;

        while (yt < yNa && yt < yMax) {
            passibility = passibility.between(clearance(clearanceFlags));

            clearanceFlags = sampler.flagsAt(x, ++yt, z);
        }

        if (yt >= yNa)
            passibility = headClearance(passibility, minPartY, x, yt, z, clearanceFlags);

        return passibility;
    }

    protected final Passibility headClearance(Passibility passibility, float partyY, int x, int y, int z, byte flags) {
        if (insufficientHeadClearance(flags, partyY, x, y, z))
            passibility = Passibility.impassible;
        else if (partyY >= 0)
            passibility = passibility.between(clearance(flags));
        return passibility;
    }

    protected final boolean insufficientHeadClearance(byte flags, float partialY0, int x, int y, int z) {
        return bottomOffsetAt(flags, x, y, z) + partialY0 > 0;
    }

    private float bottomOffsetAt(byte flags, int x, int y, int z) {
        if (!impedesMovement(flags, this.capabilities)
            || swimmingRequiredFor(flags)
        )
            return 0;

        if (Element.earth.in(flags) && Logic.nothing.in(flags))
            return 1;

        final IBlockObject block = this.instanceSpace.blockObjectAt(x, y, z);
        if (!block.isImpeding())
            return 0;

        if (block.isFullyBounded())
            return 1;

        final AxisAlignedBBox bounds = block.bounds();
        return (float)(1 - bounds.min.y);
    }

    private final Passibility clearance(byte flags) {
        return PassibilityHelpers.clearance(flags, this.capabilities);
    }

    protected final float topOffsetAt(FlagSampler sampler, int x, int y, int z) {
        return topOffsetAt(sampler.flagsAt(x, y, z), x, y, z);
    }

    protected final float topOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
            || Logic.climbable(flags)
            || Element.earth.in(flags) && Logic.nothing.in(flags)
            || swimmingRequiredFor(flags) && (capabilities.aquatic() || !capabilities.swimmer())
        )
            return 0;

        if (swimmingRequiredFor(flags))
            return -0.5f;

        final IBlockObject block = this.instanceSpace.blockObjectAt(x, y, z);
        if (!block.isImpeding()) {
            if (Element.earth.in(flags)) {
                final IBlockObject blockBelow = this.instanceSpace.blockObjectAt(x, y - 1, z);
                if (!blockBelow.isFullyBounded()) {
                    float offset = (float) blockBelow.bounds().max.y - 2;
                    if (offset < -1)
                        offset = 0;

                    return offset;
                }
            }
            return 0;
        }

        return (float)block.bounds().max.y - 1;
    }

    protected final Passibility originHeadClearance(FlagSampler sampler, Passibility passibility, Vec3i origin, int minY, float minPartY) {
        final int
                yN0 = origin.y + this.tall,
                yN = Math.max(minY, origin.y) + this.tall,
                yNa = yN - 1;

        for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
            for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z)
                for (int y = yN0; y < yNa; ++y)
                    passibility = passibility.between(clearance(sampler.flagsAt(x, y, z)));

        if (yN > yN0)
            for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
                for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z) {
                    final byte flags = sampler.flagsAt(x, yNa, z);
                    passibility = headClearance(passibility, minPartY, x, yNa, z, flags);
                    if (passibility == Passibility.impassible)
                        return Passibility.impassible;
                }

        return passibility;
    }
}
