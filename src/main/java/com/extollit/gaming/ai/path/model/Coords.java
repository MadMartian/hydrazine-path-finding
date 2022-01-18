package com.extollit.gaming.ai.path.model;

import java.text.MessageFormat;

/**
 * Represents location in 3-dimensional space with signed integral (whole numbers) components
 */
public final class Coords {
    /**
     * X-component east/west lateral location
     */
    public final int x;

    /**
     * Y-component up/down vertical location
     */
    public final int y;

    /**
     * Z-component north/south longitudinal location
     */
    public final int z;

    /**
     * Constructs a new set of coordinates in 3-dimensional space
     *
     * @param x initializes the {@link #x} component
     * @param y initializes the {@link #y} component
     * @param z initializes the {@link #z} component
     */
    public Coords(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Coords coords = (Coords) o;

        if (x != coords.x) return false;
        if (y != coords.y) return false;
        return z == coords.z;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString()
    {
        return MessageFormat.format("<{0,number,#}, {1,number,#}, {2,number,#}>", x, y, z);
    }
}
