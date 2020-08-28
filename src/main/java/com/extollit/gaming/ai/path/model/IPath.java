package com.extollit.gaming.ai.path.model;

/**
 * Represents a linear set of contiguous points that can be traversed by a pathing entity.  This object also maintains
 * state of the pathing entity's progress along the path including the current path point or whether taxiing is
 * required.  The path can also be shortened, which is useful to prevent pathing into undesirable areas outside as
 * determined by the caller (e.g. left-overs from the witching hour need a little shade and must not path into sunlight.
 *
 * Since paths may dictate to entities to move at oblique angles this can sometimes result in them getting stuck or falling
 * off ledges, for this reason if the engine determines the entity is struggling it will force the entity to taxi for a
 * short while until some progress has been made
 *
 * @see INode
 * @see #taxiing()
 */
public interface IPath extends Iterable<INode> {
    /**
     * Truncate (limit) the path to a new length.  This is useful to prevent pathing into undesirable areas as
     * determined by the caller.
     *
     * @param length new length, must be less than the length of the path
     * @throws ArrayIndexOutOfBoundsException the requested length was greater than or equal to the number of nodes in
     * this path
     */
    void truncateTo(int length);

    /**
     * Removes the previously set length restriction on the path restoring it to full length
     */
    void untruncate();

    /**
     * (Current) length of the path, this also accounts for truncation.
     *
     * @return number of path points in the path from start to finish.
     * @see #truncateTo(int)
     */
    int length();

    /**
     * The cursor of the current (next) path point the pathing entity must move to in order to advance the cursor
     * further.  Used to track pathing progress.
     *
     * @return The index of the path point the pathing entity should move to next.
     */
    int cursor();

    /**
     * The path node in this path at the specified index
     *
     * @param i index of the node in this path to retrieve
     * @return a read-only node descriptor
     */
    INode at(int i);

    /**
     * The current path node in this path at the cursor
     *
     * @return the node at the cursor where the pathing entity should move to next
     * @see #cursor()
     */
    INode current();

    /**
     * The last path node in this path, when the entity moves to this positon the path is done
     *
     * @return the node at the end of this path
     * @see #done()
     */
    INode last();

    /**
     * Whether there is no more progress left on this path and the entity should stop pathing
     *
     * @return true if all points in this path have been traversed, false otherwise
     */
    boolean done();

    /**
     * Whether the entity is currently in taxi mode and moving to directly contiguous points in this path
     *
     * @return true if the entity is taxiing, false otherwise
     */
    boolean taxiing();

    /**
     * Forces the entity to move to directly contiguous path points in lieu of distant oblique angles that can
     * potentially result in entities getting stuck.
     *
     * @param index the index of the path point in this path to taxi up until (inclusive) at which point normal
     *              pathing will resume.
     */
    void taxiUntil(int index);

    /**
     * Determines if the path points in this path are the same as the path points in the other path.  This method
     * accounts for truncation but does not account for progress or any other state.
     *
     * @param other the other path to compare this path to
     * @return true if both paths have the same path points and the same number of path points.
     */
    boolean sameAs(IPath other);
}
