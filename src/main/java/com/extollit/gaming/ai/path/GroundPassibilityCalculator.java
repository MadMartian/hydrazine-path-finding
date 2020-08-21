package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.num.FastMath;

import java.text.MessageFormat;

import static com.extollit.num.FastMath.floor;
import static java.lang.Math.round;

class GroundPassibilityCalculator implements IPointPassibilityCalculator {
    private static int
            MAX_SAFE_FALL_DISTANCE = 4,
            MAX_SURVIVE_FALL_DISTANCE = 20,
            CESA_LIMIT = 16;

    private final IInstanceSpace instanceSpace;

    private IPathingEntity.Capabilities capabilities;
    private int discreteSize, tall;

    public GroundPassibilityCalculator(IInstanceSpace instanceSpace) {
        this.instanceSpace = instanceSpace;
    }

    public static void configureFrom(IConfigModel configModel) {
        MAX_SAFE_FALL_DISTANCE = configModel.safeFallDistance();
        MAX_SURVIVE_FALL_DISTANCE = configModel.surviveFallDistance();
        CESA_LIMIT = configModel.cesaLimit();
    }

    @Override
    public void applySubject(IPathingEntity subject) {
        this.discreteSize = FastMath.floor(subject.width() + 1);
        this.tall = FastMath.floor(subject.height() + 1);
        this.capabilities = subject.capabilities();
    }

    @Override
    public Node passiblePointNear(Vec3i coords0, Vec3i origin, final FlagSampler flagSampler) {
        final Node point;
        final IPathingEntity.Capabilities capabilities = this.capabilities;
        final int
                x0 = coords0.x,
                y0 = coords0.y,
                z0 = coords0.z;

        final Vec3i d;

        if (origin != null)
            d = coords0.subOf(origin);
        else
            d = Vec3i.ZERO;

        final boolean hasOrigin = d != Vec3i.ZERO && !d.equals(Vec3i.ZERO);

        final boolean
                climbsLadders = this.capabilities.climber();

        Passibility passibility = Passibility.passible;

        int minY = Integer.MIN_VALUE;
        float minPartY = 0;

        for (int r = this.discreteSize / 2,
             x = x0 - r,
             xN = x0 + this.discreteSize - r;

             x < xN;

             ++x
                )
            for (int z = z0 - r,
                 zN = z0 + this.discreteSize - r;

                 z < zN;

                 ++z
                    ) {
                int y = y0;

                float partY = topOffsetAt(
                        flagSampler,
                        x - d.x,
                        y - d.y - 1,
                        z - d.z
                );

                byte flags = flagSampler.flagsAt(x, y, z);
                if (Element.impassible(flags, capabilities)) {
                    final float partialDisparity = partY - topOffsetAt(flags, x, y++, z);
                    flags = flagSampler.flagsAt(x, y, z);

                    if (partialDisparity < 0 || Element.impassible(flags, capabilities)) {
                        if (!hasOrigin)
                            return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);

                        if (d.x * d.x + d.z * d.z <= 1) {
                            y -= d.y + 1;

                            do
                                flags = flagSampler.flagsAt(x - d.x, y++, z - d.z);
                            while (climbsLadders && Logic.climbable(flags));
                        }

                        if (Element.impassible(flags = flagSampler.flagsAt(x, --y, z), capabilities) && (Element.impassible(flags = flagSampler.flagsAt(x, ++y, z), capabilities) || partY < 0))
                            return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);
                    }
                }
                partY = topOffsetAt(flagSampler, x, y - 1, z);
                final int ys;
                passibility = verticalClearanceAt(flagSampler, this.tall, flags, passibility, d, x, ys = y, z, partY);

                boolean swimable = false;
                for (int j = 0; unstable(flags) && !(swimable = swimable(flags)) && j <= MAX_SURVIVE_FALL_DISTANCE; j++)
                    flags = flagSampler.flagsAt(x, --y, z);

                if (swimable) {
                    final int cesaLimit = y + CESA_LIMIT;
                    final byte flags00 = flags;
                    byte flags0;
                    do {
                        flags0 = flags;
                        flags = flagSampler.flagsAt(x, ++y, z);
                    } while (swimable(flags) && unstable(flags) && y < cesaLimit);
                    if (y >= cesaLimit) {
                        y -= CESA_LIMIT + 1;
                        flags = flags00;
                    } else {
                        y--;
                        flags = flags0;
                    }
                }

                partY = topOffsetAt(flags, x, y++, z);
                passibility = verticalClearanceAt(flagSampler, ys - y, flagSampler.flagsAt(x, y, z), passibility, d, x, y, z, partY);

                if (y > minY) {
                    minY = y;
                    minPartY = partY;
                } else if (y == minY && partY > minPartY)
                    minPartY = partY;

                passibility = passibility.between(passibility(flagSampler.flagsAt(x, y, z)));
                if (passibility.impassible(capabilities))
                    return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);
            }

        if (hasOrigin && !passibility.impassible(capabilities))
            passibility = originHeadClearance(flagSampler, passibility, origin, minY, minPartY);

        passibility = fallingSafety(passibility, y0, minY);

        if (passibility.impassible(capabilities))
            passibility = Passibility.impassible;

        point = new Node(new Vec3i(x0, minY + round(minPartY), z0));
        point.passibility(passibility);
        point.volatile_(flagSampler.volatility() > 0);

        return point;
    }

    private Passibility fallingSafety(Passibility passibility, int y0, int minY) {
        final int dy = y0 - minY;
        if (dy > 1)
            passibility = passibility.between(
                    dy > MAX_SAFE_FALL_DISTANCE ?
                            Passibility.dangerous :
                            Passibility.risky
            );
        return passibility;
    }

    private Passibility verticalClearanceAt(FlagSampler sampler, int max, byte flags, Passibility passibility, Vec3i d, int x, int y, int z, float partY) {
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

    private Passibility originHeadClearance(FlagSampler sampler, Passibility passibility, Vec3i origin, int minY, float minPartY) {
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

    private boolean insufficientHeadClearance(byte flags, float partialY0, int x, int yN, int z) {
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

    private float topOffsetAt(FlagSampler sampler, int x, int y, int z) {
        return topOffsetAt(sampler.flagsAt(x, y, z), x, y, z);
    }

    private float topOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
                || Logic.climbable(flags)
                || Element.earth.in(flags) && Logic.nothing.in(flags)
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

    private boolean swimable(byte flags) {
        return this.capabilities.swimmer() && swimmingRequiredFor(flags) && (Element.water.in(flags) || this.capabilities.fireResistant());
    }

    private static boolean swimmingRequiredFor(byte flags) {
        return Element.water.in(flags) || (Element.fire.in(flags) && !Logic.fuzzy.in(flags));
    }

    private static boolean unstable(byte flags) {
        return (!Element.earth.in(flags) || Logic.ladder.in(flags));
    }

    Passibility clearance(byte flags) {
        if (Element.earth.in(flags))
            if (Logic.ladder.in(flags))
                return Passibility.passible;
            else if (Logic.fuzzy.in(flags))
                return Passibility.risky;
            else
                return Passibility.impassible;
        else if (Element.water.in(flags))
            return this.capabilities.fireResistant() ? Passibility.dangerous : Passibility.risky;
        else if (Element.fire.in(flags))
            return this.capabilities.fireResistant() ? Passibility.risky : Passibility.dangerous;
        else
            return Passibility.passible;
    }

    private Passibility passibility(byte flags) {
        final Element kind = Element.of(flags);
        switch (kind) {
            case earth:
                if (Logic.ladder.in(flags) || (Logic.doorway.in(flags) && this.capabilities.opensDoors()))
                    return Passibility.passible;
                else
                    return Passibility.impassible;

            case air:
                if (Logic.doorway.in(flags) && capabilities.avoidsDoorways())
                    return Passibility.impassible;
                else
                    return Passibility.passible;

            case water:
                if (this.capabilities.aquaphobic() || !this.capabilities.swimmer())
                    return Passibility.dangerous;
                else
                    return Passibility.risky;

            case fire:
                if (!this.capabilities.fireResistant())
                    return Passibility.dangerous;
                else
                    return Passibility.risky;
        }

        throw new IllegalArgumentException(MessageFormat.format("Unhandled element type ''{0}''", kind));
    }
}
