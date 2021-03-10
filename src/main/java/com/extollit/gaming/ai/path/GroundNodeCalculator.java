package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3i;

import static com.extollit.gaming.ai.path.PassibilityHelpers.impedesMovement;
import static com.extollit.gaming.ai.path.PassibilityHelpers.passibilityFrom;
import static java.lang.Math.round;

class GroundNodeCalculator extends AbstractNodeCalculator {
    private static int
            MAX_SAFE_FALL_DISTANCE = 4,
            MAX_SURVIVE_FALL_DISTANCE = 20,
            MAX_FALL_SEARCH = 1024,
            CESA_LIMIT = 16;

    public GroundNodeCalculator(IInstanceSpace instanceSpace) {
        super(instanceSpace);
    }

    public static void configureFrom(IConfigModel configModel) {
        MAX_SAFE_FALL_DISTANCE = configModel.safeFallDistance();
        MAX_SURVIVE_FALL_DISTANCE = configModel.surviveFallDistance();
        CESA_LIMIT = configModel.cesaLimit();
    }

    @Override
    public Node passibleNodeNear(Vec3i coords0, Vec3i origin, final FlagSampler flagSampler) {
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

                final float partY0 = topOffsetAt(
                        flagSampler,
                        x - d.x,
                        y - d.y - 1,
                        z - d.z
                );

                byte flags = flagSampler.flagsAt(x, y, z);
                final boolean impedesMovement;
                if (impedesMovement = impedesMovement(flags, capabilities)) {
                    final float partialDisparity = partY0 - topOffsetAt(flags, x, y++, z);
                    flags = flagSampler.flagsAt(x, y, z);

                    if (partialDisparity < 0 || impedesMovement(flags, capabilities)) {
                        if (!hasOrigin)
                            return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);

                        if (d.x * d.x + d.z * d.z <= 1) {
                            y -= d.y + 1;

                            do
                                flags = flagSampler.flagsAt(x - d.x, y++, z - d.z);
                            while (climbsLadders && Logic.climbable(flags));
                        }

                        if (impedesMovement(flags = flagSampler.flagsAt(x, --y, z), capabilities) && (impedesMovement(flags = flagSampler.flagsAt(x, ++y, z), capabilities) || partY0 < 0))
                            return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);
                    }
                }
                float partY = topOffsetAt(flagSampler, x, y - 1, z);
                final int ys;
                passibility = verticalClearanceAt(flagSampler, this.tall, flags, passibility, d, x, ys = y, z, Math.min(partY, partY0));

                boolean swimable = false;
                {
                    boolean condition = !impedesMovement || unstable(flags);
                    for (int j = 0,
                            jN = origin == null ? MAX_FALL_SEARCH : MAX_SURVIVE_FALL_DISTANCE;
                         condition && !(swimable = swimable(flags)) && j <= jN;
                         j++, condition = unstable(flags)
                            )
                        flags = flagSampler.flagsAt(x, --y, z);
                }

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
                passibility = verticalClearanceAt(flagSampler, ys - y, flagSampler.flagsAt(x, y, z), passibility, d, x, y, z, Math.min(partY, partY0));

                if (y > minY) {
                    minY = y;
                    minPartY = partY;
                } else if (y == minY && partY > minPartY)
                    minPartY = partY;

                passibility = passibility.between(passibilityFrom(flagSampler.flagsAt(x, y, z), capabilities));
                if (passibility.impassible(capabilities))
                    return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0);
            }

        if (hasOrigin && !passibility.impassible(capabilities))
            passibility = originHeadClearance(flagSampler, passibility, origin, minY, minPartY);

        if (origin != null)
            passibility = fallingSafety(passibility, y0, minY);

        if (passibility.impassible(capabilities))
            passibility = Passibility.impassible;

        point = new Node(new Vec3i(x0, minY + round(minPartY), z0));
        point.passibility(passibility);
        point.volatile_(flagSampler.volatility() > 0);

        return point;
    }

    @Override
    public boolean omnidirectional() {
        return false;
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

    private boolean swimable(byte flags) {
        return this.capabilities.swimmer() && swimmingRequiredFor(flags) && (Element.water.in(flags) || this.capabilities.fireResistant());
    }

    private static boolean unstable(byte flags) {
        return (!Element.earth.in(flags) || Logic.ladder.in(flags));
    }

}
