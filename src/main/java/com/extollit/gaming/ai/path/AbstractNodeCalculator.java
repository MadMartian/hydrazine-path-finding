package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.num.FastMath;

import static com.extollit.num.FastMath.floor;

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
        this.discreteSize = FastMath.floor(subject.width() + 1);
        this.tall = FastMath.floor(subject.height() + 1);
        this.capabilities = subject.capabilities();
    }

    protected final Passibility verticalClearanceAt(FlagSampler sampler, int max, byte flags, Passibility passibility, Vec3i d, int x, int y, int z, float partY) {
        byte clearanceFlags = flags;
        final int
                yMax = y + max,
                yN = Math.max(y, y - d.y) + this.tall;
        int yt = y;

        for (int yNa = yN + floor(partY);

             yt < yNa && yt < yMax;

             clearanceFlags = sampler.flagsAt(x, ++yt, z)
                )
            passibility = passibility.between(clearance(clearanceFlags));

        if (yt < yN && yt < yMax && (insufficientHeadClearance(clearanceFlags, partY, x, yt, z)))
            passibility = passibility.between(clearance(clearanceFlags));

        return passibility;
    }

    protected final boolean insufficientHeadClearance(byte flags, float partialY0, int x, int yN, int z) {
        return bottomOffsetAt(flags, x, yN, z) + partialY0 > 0;
    }

    private float bottomOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
                || Logic.climbable(flags)
                || Element.earth.in(flags) && Logic.nothing.in(flags)
                || swimmingRequiredFor(flags)
                )
            return 0;

        final IBlockObject block = this.instanceSpace.blockObjectAt(x, y, z);
        if (!block.isImpeding())
            return 0;

        return (float) block.bounds().min.y;
    }

    private final Passibility clearance(byte flags) {
        return clearance(flags, this.capabilities);
    }

    static final Passibility clearance(byte flags, IPathingEntity.Capabilities capabilities) {
        if (Element.earth.in(flags))
            if (Logic.ladder.in(flags))
                return Passibility.passible;
            else if (Logic.fuzzy.in(flags))
                return Passibility.risky;
            else
                return Passibility.impassible;
        else if (Element.water.in(flags)) {
            if (capabilities.fireResistant())
                return Passibility.dangerous;
            else if (capabilities.aquatic() && capabilities.swimmer())
                return Passibility.passible;
            else
                return Passibility.risky;
        } else if (Element.fire.in(flags))
            return capabilities.fireResistant() ? Passibility.risky : Passibility.dangerous;
        else if (capabilities.aquatic())
            return Passibility.risky;
        else
            return Passibility.passible;
    }

    protected final float topOffsetAt(FlagSampler sampler, int x, int y, int z) {
        return topOffsetAt(sampler.flagsAt(x, y, z), x, y, z);
    }

    protected final float topOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
                || Logic.climbable(flags)
                || Element.earth.in(flags) && Logic.nothing.in(flags)
                || Element.water.in(flags) && (capabilities.aquatic() || !capabilities.swimmer())
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
                yN = minY + this.tall,
                yNa = yN + floor(minPartY);

        for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
            for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z)
                for (int y = origin.y + this.tall; y < yNa; ++y)
                    passibility = passibility.between(clearance(sampler.flagsAt(x, y, z)));

        if (yNa < yN)
            for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
                for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z) {
                    final byte flags = sampler.flagsAt(x, yNa, z);
                    if (insufficientHeadClearance(flags, minPartY, x, yNa, z))
                        passibility = passibility.between(clearance(flags));
                }

        return passibility;
    }
}
