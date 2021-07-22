package com.extollit.gaming.ai.path.model;

/**
 * Description of a block independent of instance, this is the most basic type that is used primarily for populating
 * the occlusion fields.
 */
public interface IBlockDescription {
    /**
     * Whether or not this is a fence, wall, or fence gate.  Blocks like this have irregular maximum bounds
     * y-component of 1.5.
     *
     * This signals to the engine that entities should not attempt to path over these blocks.
     *
     * @return true if this describes a block that is a fence, wall or fence gate .
     */
    boolean isFenceLike();

    /**
     * Whether this block is used for climbing (typically by players, but now by mobs who can climb too!)
     *
     * If this block is climbable (i.e. vines or ladder) then the engine looks for adjacent solid blocks to support
     * climbing (most often necessary for vines since free-hanging vines cannot be climbed).  The engine looks for
     * the next adjacent block at the top of the continuous vertical string of ladders of vines.
     *
     * @return true if this is either a ladder or a vine block.
     */
    boolean isClimbable();

    /**
     * Whether the block is a door that can be manually opened or broken-down by zombies.
     *
     * In the Notchian implementation villagers and zombies interact with doors in different ways.  Zombies always try
     * to path through doors that are considered breakable (wooden doors).  Villagers will always go through doors
     * whether closed or not (if closed they open the door).  Under certain circumstances (usually when the villager is
     * threatened) villagers treat open doors as not passable.  This combined with other AI logic has the effect of
     * keeping villagers indoors during times of threat (zombie invasions) even when the doors to their houses are
     * open while still allowing them to wander inside their homes.
     *
     * @return true if this is a wooden door, wooden trap door or wooden fence gate.
     */
    boolean isDoor();

    /**
     * Whether an NPC may influence the passibility of this block or if the block is "intractable" and cannot be
     * influenced by any AI.
     *
     * This applies typically to doors, some doors an NPC may open while other types of doors an NPC may not open.
     * This flag is global and applies to all types of pathing subjects for a particular block description.
     *
     * @return true if NPCs should not attempt to path through this block if it is impeding and it is flagged as a
     * door.
     */
    boolean isIntractable();

    /**
     * Does this block impede movement in at least one direction?
     *
     * This is used to determine if a block impacts passibility at all and has at least some collision bounds that
     * prevent any movement.
     *
     * Examples of such blocks include:
     * - Solid stone and ore
     * - Slab or plank
     * - Wall
     * - Anvil
     * - Yes, even lily-pads
     *
     * Examples of blocks that do NOT impede:
     * - Air
     * - Grass
     * - Water
     * - Fire
     * - Lava
     * - Quicksand
     * - Flowers
     * - Vines
     * - Ladders
     *
     * @return true if this block has at least some collision bounds.
     */
    boolean isImpeding();

    /**
     * This is a special sort of block that "impedes" with full 1x1x1 collision bounds effectively filling the entire
     * volume of the block.
     *
     * This flag is used to signal to the engine whether additional and potentially CPU-intensive calcuations are
     * necessary to determine how this block affects path-finding.  If the block is fully bounded then no additional
     * comprehensive calculations are necessary.
     *
     * Examples of such blocks include:
     * - Stone
     * - Ore
     * - Lapis compressed block
     * - Command cube
     *
     * Examples of blocks that are NOT fully bounded, but do impede:
     * - Anvil
     * - Wall
     * - Lily-pad
     *
     * @return true if the block has min/max bounds of &lt; 0, 0, 0 &gt; to &lt; 1, 1, 1 &gt;.
     */
    boolean isFullyBounded();

    /**
     * Whether this represents a liquid block, either flowing or static, harmful or benign.  Blocks like this will
     * signal to pathing entities that they need to swim through it.
     *
     * Swimming is conducted by the Notchian implementation (and by Hydrazine path-finding engine) by constantly telling
     * the mob to jump as if holding the space-bar steady and attempting to walk on-top of the fluid.  This is to
     * counteract gravity from pulling the entity downward and drowning it.
     *
     * @return true if this is a liquid such as water, lava, mud or quicksand.
     */
    boolean isLiquid();

    /**
     * Does this block burn entities that come into contact with it?
     *
     * Blocks with this flag signal that only entities with fire-resistance (either with the potion effect running or
     * natural fire resistance as with the Lava Monsters mod) can path through this.
     *
     * @return true if this is lava or fire or something similar that burns due to high heat.
     */
    boolean isIncinerating();
}
