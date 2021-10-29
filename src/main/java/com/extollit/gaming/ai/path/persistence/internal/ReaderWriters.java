package com.extollit.gaming.ai.path.persistence.internal;

/**
 * Internal API, do not use this directly
 * @see com.extollit.gaming.ai.path.persistence.Persistence
 */
public final class ReaderWriters {
    private static final ReaderWriters
            legacy = new ReaderWriters(Vec3dReaderWriter.INSTANCEz, MutableVec3dReaderWriter.INSTANCEz, Vec3iReaderWriter.INSTANCEz),
            v4 = new ReaderWriters(NullableVec3dReaderWriter.INSTANCE, NullableMutableVec3dReaderWriter.INSTANCE, NullableVec3iReaderWriter.INSTANCE);

    public final Vec3dReaderWriter v3d;
    public final MutableVec3dReaderWriter mv3d;
    public final Vec3iReaderWriter v3i;
    public final DummyDynamicMovableObject.ReaderWriter ddmo;
    public final DummyPathingEntity.ReaderWriter dpe;

    private ReaderWriters(Vec3dReaderWriter v3d, MutableVec3dReaderWriter mv3d, Vec3iReaderWriter v3i) {
        this.v3d = v3d;
        this.mv3d = mv3d;
        this.v3i = v3i;
        this.ddmo = new DummyDynamicMovableObject.ReaderWriter(v3d);
        this.dpe = new DummyPathingEntity.ReaderWriter(mv3d, v3d);
    }

    public static ReaderWriters forVersion(byte version) {
        if (version < 4)
            return legacy;
        else
            return v4;
    }
}
