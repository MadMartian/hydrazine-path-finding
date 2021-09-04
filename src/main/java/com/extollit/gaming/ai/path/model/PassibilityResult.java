package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

public final class PassibilityResult {
    public final Passibility passibility;
    public final Vec3i pos;

    public PassibilityResult(Passibility passibility, Vec3i pos) {
        this.passibility = passibility;
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PassibilityResult)) return false;

        PassibilityResult that = (PassibilityResult) o;

        if (passibility != that.passibility) return false;
        return pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return passibility.hashCode();
    }

    @Override
    public String toString() {
        return "PassibilityResult{" +
                "passibility=" + passibility +
                ", pos=" + pos +
                '}';
    }
}
