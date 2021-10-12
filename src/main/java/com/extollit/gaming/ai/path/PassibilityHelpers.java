package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;

import java.text.MessageFormat;

class PassibilityHelpers {
    static boolean impedesMovement(byte flags, IPathingEntity.Capabilities capabilities) {
        return (Element.earth.in(flags) && !passibleDoorway(flags, capabilities) && !Logic.ladder.in(flags))
                || (Element.air.in(flags) && impassibleDoorway(flags, capabilities));
    }

    static Passibility clearance(byte flags, IPathingEntity.Capabilities capabilities) {
        if (Element.earth.in(flags))
            if (Logic.ladder.in(flags) || passibleDoorway(flags, capabilities))
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
        else if (impassibleDoorway(flags, capabilities))
            return Passibility.impassible;
        else if (capabilities.aquatic())
            return Passibility.risky;
        else
            return Passibility.passible;
    }

    static Passibility passibilityFrom(byte flags, IPathingEntity.Capabilities capabilities) {
        if (impassibleDoorway(flags, capabilities))
            return Passibility.impassible;

        final Element kind = Element.of(flags);
        switch (kind) {
            case earth:
                if (Logic.ladder.in(flags) || (passibleDoorway(flags, capabilities)))
                    return Passibility.passible;
                else
                    return Passibility.impassible;

            case air:
                if (capabilities.aquatic())
                    return Passibility.dangerous;
                else
                    return Passibility.passible;

            case water: {
                final boolean gilled = capabilities.aquatic();
                if (capabilities.aquaphobic())
                    return Passibility.dangerous;
                else if (gilled && capabilities.swimmer())
                    return Passibility.passible;
                else
                    return Passibility.risky;
            }
            case fire:
                if (!capabilities.fireResistant())
                    return Passibility.dangerous;
                else
                    return Passibility.risky;
        }

        throw new IllegalArgumentException(MessageFormat.format("Unhandled element type ''{0}''", kind));
    }

    static Gravitation gravitationFrom(byte flags) {
        if (Element.earth.in(flags))
            return Gravitation.grounded;
        if (Element.water.in(flags))
            return Gravitation.buoyant;
        if (Element.air.in(flags))
            return Gravitation.airborne;

        return Gravitation.grounded;
    }

    private static boolean passibleDoorway(byte flags, IPathingEntity.Capabilities capabilities) {
        return Logic.doorway.in(flags) && capabilities.opensDoors() && !capabilities.avoidsDoorways() && !Element.fire.in(flags);
    }

    private static boolean impassibleDoorway(byte flags, IPathingEntity.Capabilities capabilities) {
        return Logic.doorway.in(flags) && (((!capabilities.opensDoors() && !Element.air.in(flags)) || capabilities.avoidsDoorways()) || Element.fire.in(flags));
    }
}
