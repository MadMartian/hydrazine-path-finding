package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;

import java.text.MessageFormat;

class PassibilityHelpers {
    static boolean impedesMovement(byte flags, IPathingEntity.Capabilities capabilities) {
        return (Element.earth.in(flags) && !(Logic.doorway.in(flags) && capabilities.opensDoors()) && !Logic.ladder.in(flags))
                || (Element.air.in(flags) && Logic.doorway.in(flags) && capabilities.avoidsDoorways());
    }

    static Passibility passibilityFrom(byte flags, IPathingEntity.Capabilities capabilities) {
        if (Logic.doorway.in(flags) && capabilities.avoidsDoorways())
            return Passibility.impassible;

        final Element kind = Element.of(flags);
        switch (kind) {
            case earth:
                if (Logic.ladder.in(flags) || (Logic.doorway.in(flags) && capabilities.opensDoors()))
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
}
