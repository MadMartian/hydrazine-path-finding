package com.extollit.gaming.ai.path.model;

/**
 * Passibility of blocks is stored in nibbles by an occlusion field.  The lower two bits of this nibble indicates
 * the material and basic passibility of the block.  This is determined from the flags on {@link IBlockDescription}.
 *
 * @see IBlockDescription
 * @see Logic
 */
public enum Element {
    /**
     * Represents a passible block having no collision bounds.
     * A block description that is not impeding, a liquid or incinerating produces an 'air' element.
     *
     * @see IBlockDescription#isImpeding()
     * @see IBlockDescription#isLiquid()
     * @see IBlockDescription#isIncinerating()
     */
    air,

    /**
     * Represents an impassible block having at least some collision bounds.
     * A block description that is impeding produces an 'earth' element with the following additional conditions:
     * - If the block is a door, {@link IBlockObject#isImpeding()} is called to determine dynamic collision bounds
     *   from the specific block's state inside the instance.  If the door is open then the element 'air' results
     *   instead.
     * - Climable blocks with no collision bounds (i.e. vines and ladders) also produce an 'earth' element.  This is
     *   a special provision due to bit restrictions.  This is acceptable because something that has collision bounds
     *   cannot also be a climbable ladder or vine due to how these work in the Notchian implementation.
     *
     * @see Logic#climbable(byte)
     * @see IBlockObject#isImpeding()
     * @see IBlockDescription#isDoor()
     * @see #air
     */
    earth,

    /**
     * Represents a fluid block having no collision bounds but can potentially drown an entity or requires swimming.
     * This is used only for non-incinerating fluids such as water, quicksand or mud.  Lava or magma is expressed
     * differently.
     *
     * A block description that is liquid and not incinerating produces a 'water' element.
     *
     * @see IBlockDescription#isLiquid()
     * @see IBlockDescription#isIncinerating()
     */
    water,

    /**
     * Represents a block that can burn entities that pass through it (if they are not fire resistant).  This is also
     * used to represent lava or magma blocks with the additional {@link Logic#fuzzy} flag.
     *
     * A block description that is incinerating produces a 'fire' element.
     *
     * @see IBlockDescription#isIncinerating()
     * @see Logic#fuzzy
     */
    fire;

    private static final Element[] VALUES = values();

    public static final int MASK = 4 - 1;

    public final byte mask = (byte)ordinal();

    /**
     * Helper function for determining whether this element is represented by the specified nibble.  The nibble may
     * contain {@link Logic} flags in the high two bits, these will be ignored.
     *
     * @param flags A four-bit nibble containing a lower two bit element representation
     *
     * @return true if the lower two bits map to this element
     */
    public boolean in(byte flags) {
        return (flags & MASK) == this.mask;
    }

    /**
     * Helper function for determining the element represented by the specified nibble.  The nibble may contain
     * {@link Logic} flags in the high two bits, these will be ignored.
     *
     * @param flags A four-bit nibble containing a lower two bit element representation
     *
     * @return the element represented by the nibble
     */
    public static Element of(byte flags) {
        return VALUES[flags & MASK];
    }

    /**
     * Helper function for setting the lower two bits of the specified nibble to represent this element.  The nibble
     * may contain {@link Logic} flags in the high two bits, these bits will not be affected.
     *
     * @param flags A four-bit nibble that will be modified to represent this element
     * @return the passed-in flags modified to represent this element
     */
    public byte to(byte flags) {
        return (byte)((flags & ~MASK) | this.mask);
    }

    static boolean doorClosedLike(byte flags) { return (flags & (earth.mask | fire.mask)) != 0; }
}
