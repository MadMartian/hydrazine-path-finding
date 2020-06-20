package com.extollit.gaming.ai.path.model;

import java.util.ArrayList;
import java.util.ListIterator;

public class SortedPointQueue {
    private static final float CULL_THRESHOLD = 0.25f;

    private final ArrayList<HydrazinePathPoint> list = new ArrayList<>(8);

    protected boolean fastAdd(HydrazinePathPoint point) {
        if (point.assigned())
            throw new IllegalStateException("Point is already assigned");

        if (!point.index(this.list.size()))
            return false;

        this.list.add(point);
        sortBack(point.index());
        return true;
    }

    public final void clear() {
        for (HydrazinePathPoint point : this.list)
            point.unassign();
        this.list.clear();
    }

    public final boolean isEmpty() {
        return this.list.isEmpty();
    }

    public HydrazinePathPoint top() {
        return this.list.get(0);
    }
    public HydrazinePathPoint dequeue() {
        final ArrayList<HydrazinePathPoint> list = this.list;
        final HydrazinePathPoint point;
        if (list.size() == 1)
            point = list.remove(0);
        else {
            point = list.set(0, list.remove(list.size() - 1));
            sortForward(0);
        }

        point.unassign();
        return point;
    }

    public boolean modifyDistance(HydrazinePathPoint point, int distance) {
        final int distance0 = point.journey();

        if (point.journey(distance)) {
            if (distance < distance0)
                sortBack(point.index());
            else
                sortForward(point.index());

            return true;
        } else
            return false;
    }

    private void sortBack(int index) {
        final ArrayList<HydrazinePathPoint> list = this.list;
        final HydrazinePathPoint originalPoint = list.get(index);
        final int distanceRemaining = originalPoint.journey();
        final Passibility originalPassibility = originalPoint.passibility();
        while (index > 0) {
            final int i = (index - 1) >> 1;
            final HydrazinePathPoint point = list.get(i);

            final Passibility passibility = point.passibility();
            if ((distanceRemaining >= point.journey() && originalPassibility == passibility) || originalPassibility.worseThan(passibility))
                break;

            list.set(index, point);
            point.index(index);

            index = i;
        }

        list.set(index, originalPoint);
        originalPoint.index(index);
    }

    private void sortForward(int index) {
        final ArrayList<HydrazinePathPoint> list = this.list;
        HydrazinePathPoint originalPoint = list.get(index);
        final int distanceRemaining = originalPoint.journey();
        final Passibility originalPassibility = originalPoint.passibility();

        do {
            final int i = 1 + (index << 1);
            final int j = i + 1;

            if (i >= list.size())
                break;

            final HydrazinePathPoint pointAlpha = list.get(i);
            final int distAlpha = pointAlpha.journey();
            final Passibility passibilityAlpha = pointAlpha.passibility();
            final HydrazinePathPoint pointBeta;
            final int distBeta;
            final Passibility passibilityBeta;

            if (j >= list.size()) {
                pointBeta = null;
                distBeta = Integer.MIN_VALUE;
                passibilityBeta = Passibility.passible;
            } else {
                pointBeta = list.get(j);
                distBeta = pointBeta.journey();
                passibilityBeta = pointBeta.passibility();
            }

            if ((distAlpha < distBeta && passibilityAlpha == passibilityBeta)
                    || passibilityAlpha.betterThan(passibilityBeta)) {
                if ((distAlpha >= distanceRemaining && passibilityAlpha == originalPassibility)
                        || passibilityAlpha.worseThan(originalPassibility))
                    break;

                list.set(index, pointAlpha);
                pointAlpha.index(index);
                index = i;
            } else {
                if (pointBeta == null || (distBeta >= distanceRemaining && passibilityAlpha == originalPassibility)
                        || passibilityBeta.worseThan(originalPassibility))
                    break;

                list.set(index, pointBeta);
                pointBeta.index(index);
                index = j;
            }
        } while (true);

        list.set(index, originalPoint);
        originalPoint.index(index);
    }

    public boolean appendTo(HydrazinePathPoint point, HydrazinePathPoint parent, HydrazinePathPoint target) {
        final int squareDelta = HydrazinePathPoint.squareDelta(parent, point);

        final byte length = point.length();
        if (!point.assigned() || (parent.length() + squareDelta < length*length && !point.passibility().betterThan(parent.passibility()))) {
            if (point.appendTo(parent, (int)Math.sqrt(squareDelta), target)) {
                final int distance = point.length() + point.delta();
                if (point.assigned())
                    return modifyDistance(point, distance);
                else if (point.journey(distance))
                    add(point);
            } else
                point.orphan();
        }

        return false;
    }

    public void add(HydrazinePathPoint point) {
        if (fastAdd(point))
            return;

        final ArrayList<HydrazinePathPoint> list = this.list;
        final int size = list.size();

        final ListIterator<HydrazinePathPoint> i = list.listIterator(size);
        for (int amount = (int)Math.ceil((float)size * CULL_THRESHOLD); amount > 0 && i.hasPrevious(); --amount) {
            i.previous().unassign();
            i.remove();
        }

        fastAdd(point);
    }
}
