package com.extollit.gaming.ai.path.model;

/**
 * A one-dimensional store for occlusion fields aligned along the y-axis distributed according to chunk coordinates.
 * Define an object of this class as a final field member of your concrete implementation of {@link IColumnarSpace} then
 * implement {@link IColumnarSpace#occlusionFields()} to return this object.
 *
 * Additionally, some members of this type must be called upon certain server events.  Methods that the implementor should
 * be aware of are documented whereas methods below that remain undocumented are for internal use.
 *
 * @see IColumnarSpace
 * @see OcclusionField
 */
public class ColumnarOcclusionFieldList {
    /**
     * Containing columnar space that owns this object.  This object will typically have a final field member that points
     * to this object and returns it via {@link IColumnarSpace#occlusionFields()}
     */
    public final IColumnarSpace container;

    private OcclusionField [] fields;

    /**
     * Construct a new object bound to the specified columnar space container, this is what {@link #container} will be
     * bound to in a one-to-one relationship.
     *
     * @param container columnar space that owns this object
     */
    public ColumnarOcclusionFieldList(IColumnarSpace container) {
        this.container = container;
    }

    /**
     * Completely erases all data in this object, this must be called by the implementor prior to loading a chunk or
     * unloading a chunk to prevent stale state creep.  This effectively forces lazy-reinitialization of the occlusion
     * field cache.
     */
    @SuppressWarnings("unused")
    public void reset() {
        this.fields = null;
    }

    /**
     * Notifies the occlusion field cache that a block in the containing columnar space has changed (i.e has been added,
     * removed, changed type, or has had its meta-data changed).  The implementor must call this method whenever this
     * change occurs in the server, it causes the associated occlusion field data to update accordingly.
     *
     * This method's coordinate parameters are exceptional because they are absolute (relative to the instance) rather
     * than relative (to the parent columnar space)
     *
     * @param x absolute (relative to the instance space, not the columnar space) x coordinate of the block that changed
     * @param y absolute (relative to the instance space, not the columnar space) y coordinate of the block that changed
     * @param z absolute (relative to the instance space, not the columnar space) z coordinate of the block that changed
     * @param description description of the new block replacing what existed previously
     * @param metaData meta-data for the new block replacing what existed previously
     */
    @SuppressWarnings("unused")
    public void onBlockChanged(int x, int y, int z, IBlockDescription description, int metaData) {
        final OcclusionField[] fields = this.fields;
        if (fields == null)
            return;

        final OcclusionField field = fields[y >> 4 & 0xF];

        if (field == null)
            return;

        field.set(this.container, x, y, z, description);
    }

    public final OcclusionField occlusionFieldAt(int cx, int cy, int cz) {
        OcclusionField[] fields = this.fields;
        if (fields == null)
            fields = this.fields = new OcclusionField[OcclusionField.DIMENSION_SIZE];

        OcclusionField result = fields[cy];
        if (result == null) {
            final OcclusionField occlusionField = createOcclusionField(cx, cy, cz);
            return fields[cy] = occlusionField;
        } else
            return result;
    }

    public final OcclusionField optOcclusionFieldAt(int cy) {
        return this.fields == null ? null : this.fields[cy];
    }

    public static OcclusionField optOcclusionFieldAt(IInstanceSpace instance, int cx, int cy, int cz) {
        final IColumnarSpace columnarSpace = instance.columnarSpaceAt(cx, cz);

        if (columnarSpace == null)
            return null;

        return columnarSpace.occlusionFields().optOcclusionFieldAt(cy);
    }

    protected OcclusionField createOcclusionField(int cx, int cy, int cz) {
        final OcclusionField occlusionField = new OcclusionField();
        occlusionField.loadFrom(this.container, cx, cy, cz);
        return occlusionField;
    }
}
