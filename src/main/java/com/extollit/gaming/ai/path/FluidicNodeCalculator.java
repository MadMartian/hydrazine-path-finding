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
    public Node passibleNodeNear(Vec3i coords0, Vec3i origin, FlagSampler flagSampler) {
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
                    return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0, gravitation);
                else
                    passibility = passibility.between(passibilityFrom(flags, capabilities));

                final float partY0 = topOffsetAt(
                        flagSampler,
                        x - d.x,
                        y0 - d.y - 1,
                        z - d.z
                );
                final float partY = topOffsetAt(flagsBeneath, x, yb, z);
                passibility = verticalClearanceAt(flagSampler, this.tall, flags, passibility, d, x, y0, z, Math.min(partY, partY0));

                if (y0 > minY) {
                    minY = y0;
                    minPartY = partY;
                } else if (partY > minPartY)
                    minPartY = partY;

                if (passibility.impassible(capabilities))
                    return new Node(coords0, Passibility.impassible, flagSampler.volatility() > 0, gravitation);
            }

        if (passibility.impassible(capabilities))
            passibility = Passibility.impassible;
        else if (hasOrigin)
            passibility = originHeadClearance(flagSampler, passibility, origin, minY, minPartY);

        point = new Node(new Vec3i(x0, minY + round(minPartY), z0));
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
