package com.extollit.gaming.ai.path.persistence;

import com.extollit.gaming.ai.path.HydrazinePathFinder;
import com.extollit.gaming.ai.path.model.IInstanceSpace;

import java.io.*;

public class Persistence {
    private static final String TAG = "HPOD";
    private static final byte VERSION = 2;

    public static void persist(HydrazinePathFinder pathFinder, ObjectOutput out) throws IOException {
        out.writeUTF(TAG);
        out.writeByte(VERSION);

        DummyPathingEntity.ReaderWriter.INSTANCE.writePartialObject(pathFinder.subject(), out);
        pathFinder.writeVersioned(VERSION, out);
    }

    public static HydrazinePathFinder restore(ObjectInput in, IInstanceSpace instanceSpace) throws IOException {
        if (!TAG.equals(in.readUTF()))
            throw new IOException("Not a valid " + TAG + " file");
        final byte ver = in.readByte();
        if (ver > VERSION)
            throw new IOException("Unsupported version: " + ver);

        final DummyPathingEntity pathingEntity = DummyPathingEntity.ReaderWriter.INSTANCE.readPartialObject(in);
        final HydrazinePathFinder pathFinder = new HydrazinePathFinder(pathingEntity, instanceSpace);
        pathFinder.readVersioned(ver, in);
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
}
