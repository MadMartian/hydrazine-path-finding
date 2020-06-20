package com.extollit.gaming.ai.path.model;

/**
 * Provides simplified low-latency information about a block at a location in an instance.  This is not meant to be
 * implemented by API consumers.
 *
 * @see AreaOcclusionProvider
 * @see OcclusionField
 */
public interface IOcclusionProvider {
    /**
     * Returns a nibble that describes the passibility of the block at the specified coordinates in the instance.
     *
     * The low two bits of the nibble are used to identify the {@link Element} of the block and the high two bits of the
     * nibble are used to identify the {@link Logic} of the block.  There are utility methods on those types for
     * conveniently extracting that information from a nibble.
     *
     * @param x absolute (relative to the instance) x-coordinate
     * @param y absolute (relative to the instance) y-coordinate
     * @param z absolute (relative to the instance) z-coordinate
     * @return a nibble containing the {@link Element} and {@link Logic} information about a block in the instance at
     *          the aforementioned coordinates.
     * @see Element
     * @see Logic
     */
    byte elementAt(int x, int y, int z);

    /**
     * Provides a visualization of an x/z plane of the occlusion field at the specified y coordinate using ASCII art.
     *
     * This method was used for troubleshooting and is not used by the engine itself.
     *
     * @param y absolute y-coordinate (relative to the instance) to render an x/z plane snapshot of the field
     * @return a string that conveniently provides a visualization of the field, especially if used as a watch expression
     *         during a debugging session.
     */
    String visualizeAt(int y);
}
