package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.Vec3i;

import static com.extollit.gaming.ai.path.PassibilityHelpers.*;
import static java.lang.Math.round;

class FluidicNodeCalculator extends AbstractNodeCalculator implements INodeCalculator {
    public FluidicNodeCalculator(IInstanceSpace instanceSpace)
    {
        super(instanceSpace);
    }

    @Override
    public Node passibleNodeNear(int x0, int y0, int z0, Vec3i origin, FlagSampler flagSampler) {
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

        Passibility passibility = Passibility.passible;
        Gravitation gravitation = Gravitation.airborne;

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

                byte flags = flagSampler.flagsAt(x, y0, z);
                final int yb = y0 - 1;
                final byte flagsBeneath = flagSampler.flagsAt(x, yb, z);

                gravitation = gravitation.between(gravitationFrom(flags));
                gravitation = gravitation.between(gravitationFrom(flagsBeneath));

                if (impedesMovement(flags, capabilities))
                    return new Node(x0, y0, z0, Passibility.impassible, flagSampler.volatility() > 0, gravitation);
                else
                    passibility = passibility.between(passibilityFrom(flags, capabilities));

                final float partY0 = topOffsetAt(
                        flagSampler,
                        x - dx,
                        y0 - dy - 1,
                        z - dz
                );
                final float partY = topOffsetAt(flagsBeneath, x, yb, z);
                passibility = verticalClearanceAt(flagSampler, this.tall, flags, passibility, dy, x, y0, z, Math.min(partY, partY0));

                if (y0 > minY) {
                    minY = y0;
                    minPartY = partY;
                } else if (partY > minPartY)
                    minPartY = partY;

                if (passibility.impassible(capabilities))
                    return new Node(x0, y0, z0, Passibility.impassible, flagSampler.volatility() > 0, gravitation);
            }

        if (passibility.impassible(capabilities))
            passibility = Passibility.impassible;
        else if (hasOrigin)
            passibility = originHeadClearance(flagSampler, passibility, origin, minY, minPartY);

        point = new Node(x0, minY + round(minPartY), z0);
        point.passibility(passibility);
        point.gravitation(gravitation);
        point.volatile_(flagSampler.volatility() > 0);

        return point;
    }

    @Override
    public boolean omnidirectional() {
        return true;
    }
}
