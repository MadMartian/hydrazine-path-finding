package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;

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
    public Node passibleNodeNear(final int x0, final int y0, final int z0, Coords origin, final FlagSampler flagSampler) {
        final Node point;
        final IPathingEntity.Capabilities capabilities = this.capabilities;

        final int
            dx, dy, dz;

        if (origin != null) {
            dx = x0 - origin.x;
            dy = y0 - origin.y;
            dz = z0 - origin.z;
        } else
            dx = dy = dz = 0;

        final boolean hasOrigin = (dx != 0 || dy != 0 || dz != 0);

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
                        x - dx,
                        y - dy - 1,
                        z - dz
                );

                byte flags = flagSampler.flagsAt(x, y, z);
                final boolean impedesMovement;
                if (impedesMovement = impedesMovement(flags, capabilities)) {
                    final float partialDisparity = partY0 - topOffsetAt(flags, x, y++, z);
                    flags = flagSampler.flagsAt(x, y, z);

                    if (partialDisparity < 0 || impedesMovement(flags, capabilities)) {
                        if (!hasOrigin)
                            return new Node(x0, y0, z0, Passibility.impassible, flagSampler.volatility() > 0);

                        if (dx * dx + dz * dz <= 1) {
                            y -= dy + 1;

                            do
                                flags = flagSampler.flagsAt(x - dx, y++, z - dz);
                            while (climbsLadders && Logic.climbable(flags));
                        }

                        if (impedesMovement(flags = flagSampler.flagsAt(x, --y, z), capabilities) && (impedesMovement(flags = flagSampler.flagsAt(x, ++y, z), capabilities) || partY0 < 0))
                            return new Node(x0, y0, z0, Passibility.impassible, flagSampler.volatility() > 0);
                    }
                }
                float partY = topOffsetAt(flagSampler, x, y - 1, z);
                final int ys;
                passibility = verticalClearanceAt(flagSampler, this.tall, flags, passibility, dy, x, ys = y, z, Math.min(partY, partY0));

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
                passibility = verticalClearanceAt(flagSampler, ys - y, flagSampler.flagsAt(x, y, z), passibility, dy, x, y, z, Math.min(partY, partY0));

                if (y > minY) {
                    minY = y;
                    minPartY = partY;
                } else if (y == minY && partY > minPartY)
                    minPartY = partY;

                passibility = passibility.between(passibilityFrom(flagSampler.flagsAt(x, y, z), capabilities));
                if (passibility.impassible(capabilities))
                    return new Node(x0, y0, z0, Passibility.impassible, flagSampler.volatility() > 0);
            }

        if (hasOrigin && !passibility.impassible(capabilities))
            passibility = originHeadClearance(flagSampler, passibility, origin, minY, minPartY);

        if (origin != null)
            passibility = fallingSafety(passibility, y0, minY);

        if (passibility.impassible(capabilities))
            passibility = Passibility.impassible;

        point = new Node(x0, minY + round(minPartY), z0);
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
