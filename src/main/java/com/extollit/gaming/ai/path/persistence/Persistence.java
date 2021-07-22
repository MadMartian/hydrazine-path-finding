package com.extollit.gaming.ai.path.persistence;

import com.extollit.gaming.ai.path.HydrazinePathFinder;
import com.extollit.gaming.ai.path.model.IInstanceSpace;

import java.io.*;

public class Persistence {
    private static final String TAG = "HPOD";
    private static final byte VERSION = 4;

    public static void persist(HydrazinePathFinder pathFinder, ObjectOutput out) throws IOException {
        final Persistence.ReaderWriters readerWriters = Persistence.ReaderWriters.forVersion(VERSION);

        out.writeUTF(TAG);
        out.writeByte(VERSION);

        readerWriters.dpe.writePartialObject(pathFinder.subject(), out);
        pathFinder.writeVersioned(VERSION, readerWriters, out);
    }

    public static HydrazinePathFinder restore(ObjectInput in, IInstanceSpace instanceSpace) throws IOException {
        if (!TAG.equals(in.readUTF()))
            throw new IOException("Not a valid " + TAG + " file");
        final byte ver = in.readByte();
        if (ver > VERSION)
            throw new IOException("Unsupported version: " + ver);

        final Persistence.ReaderWriters readerWriters = Persistence.ReaderWriters.forVersion(ver);

        final DummyPathingEntity pathingEntity = readerWriters.dpe.readPartialObject(in);
        final HydrazinePathFinder pathFinder = new HydrazinePathFinder(pathingEntity, instanceSpace);
        pathFinder.readVersioned(ver, readerWriters, in);
        return pathFinder;
    }

    public static void persist(HydrazinePathFinder pathFinder, String fileName) throws IOException {
        final FileOutputStream fileOut = new FileOutputStream(fileName);
        try {
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            try {
                Persistence.persist(pathFinder, out);
            } finally {
                out.close();
            }
        } finally {
            fileOut.close();
        }
    }

    public static HydrazinePathFinder restore(File file, IInstanceSpace instanceSpace) throws IOException {
        final FileInputStream fileIn = new FileInputStream(file);
        try {
            return Persistence.restore(fileIn, instanceSpace);
        } finally {
            fileIn.close();
        }
    }

    public static HydrazinePathFinder restore(InputStream in, IInstanceSpace instanceSpace) throws IOException {
        return restore((ObjectInput) new ObjectInputStream(in), instanceSpace);
    }

    public static final class ReaderWriters {
        public static final ReaderWriters
                legacy = new ReaderWriters(Vec3dReaderWriter.INSTANCEz, MutableVec3dReaderWriter.INSTANCEz, Vec3iReaderWriter.INSTANCEz),
                v4 = new ReaderWriters(NullableVec3dReaderWriter.INSTANCE, NullableMutableVec3dReaderWriter.INSTANCE, NullableVec3iReaderWriter.INSTANCE);

        public final Vec3dReaderWriter v3d;
        public final MutableVec3dReaderWriter mv3d;
        public final Vec3iReaderWriter v3i;
        public final DummyDynamicMovableObject.ReaderWriter ddmo;
        public final DummyPathingEntity.ReaderWriter dpe;

        public ReaderWriters(Vec3dReaderWriter v3d, MutableVec3dReaderWriter mv3d, Vec3iReaderWriter v3i) {
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
}
