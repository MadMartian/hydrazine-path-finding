package com.extollit.gaming.ai.path.persistence;

import com.extollit.gaming.ai.path.HydrazinePathFinder;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.persistence.internal.DummyPathingEntity;
import com.extollit.gaming.ai.path.persistence.internal.ReaderWriters;

import java.io.*;

/**
 * Sub-system for persisting the entire object state of a pathing an entity to a stream or to even to disk.
 * This is a troubleshooting aid that allows users to capture a snapshot of path-finding behaviour at any point in
 * time and then transfer that snapshot to another developer for investigation.  The snapshot can then be loaded
 * and deserialized into memory and the developer can inspect the objects as if the behavior occurred on their
 * machine instead of the other user's machine.
 *
 * This API will be essential for other developers consuming / leveraging the Hydrazine Path Engine looking for
 * support where fine-grained technical troubleshooting details are imperative.
 */
public class Persistence {
    private static final String TAG = "HPOD";
    private static final byte VERSION = 4;

    /**
     * Serialize a path-finder and its internal state to a stream.  This takes a snapshot of the path-finding object
     * and it's referenced objects at the time this is called.
     *
     * @param pathFinder Path-finder object whose state shall be frozen to the stream
     * @param out Stream to which to write the state
     * @throws IOException If there was an underlying stream error
     * @see #restore(ObjectInput, IInstanceSpace)
     */
    public static void persist(HydrazinePathFinder pathFinder, ObjectOutput out) throws IOException {
        final ReaderWriters readerWriters = ReaderWriters.forVersion(VERSION);

        out.writeUTF(TAG);
        out.writeByte(VERSION);

        readerWriters.dpe.writePartialObject(pathFinder.subject(), out);
        pathFinder.writeVersioned(VERSION, readerWriters, out);
    }

    /**
     * Deserialize a path-finder state from a stream.  This loads a frozen snapshot into a new path-finder object
     * that can be inspected for the purpose of troubleshooting or integration testing.
     *
     * NOTE: Some reconstructed objects are "dummies" and not real objects where third-party integrations are concerned
     *
     * @param in             Stream from which to load the path-finder state from
     * @param instanceSpace  A reference to an instance space that the newly constructed path-finding state will be
     *                       become bound to
     * @return               The deserialized path-finder state and its internal representation previously serialized
     * @throws IOException  If there was an underlying stream error
     * @see #persist(HydrazinePathFinder, ObjectOutput)
     */
    public static HydrazinePathFinder restore(ObjectInput in, IInstanceSpace instanceSpace) throws IOException {
        if (!TAG.equals(in.readUTF()))
            throw new IOException("Not a valid " + TAG + " file");
        final byte ver = in.readByte();
        if (ver > VERSION)
            throw new IOException("Unsupported version: " + ver);

        final ReaderWriters readerWriters = ReaderWriters.forVersion(ver);

        final DummyPathingEntity pathingEntity = readerWriters.dpe.readPartialObject(in);
        final HydrazinePathFinder pathFinder = new HydrazinePathFinder(pathingEntity, instanceSpace);
        pathFinder.readVersioned(ver, readerWriters, in);
        return pathFinder;
    }

    /**
     * Serialize a path-finder and its internal state directly to a file on disk.  This takes a snapshot of the
     * path-finding object and it's referenced objects at the time this is called.
     *
     * @param pathFinder Path-finder object whose state shall be frozen to the stream
     * @param fileName File and/or path where the snapshot will be saved at
     * @throws IOException If a disk I/O error occurs
     * @see #restore(String, IInstanceSpace)
     */
    public static void persist(HydrazinePathFinder pathFinder, String fileName) throws IOException {
        persist(pathFinder, new File(fileName));
    }

    /**
     * Deserialize a path-finder state directly from a file on disk.  This loads a frozen snapshot into a new path-finder
     * object that can be inspected for the purpose of troubleshooting or integration testing.
     *
     * @param fileName          File and/or path of a file containing a frozen snapshot
     * @param instanceSpace     A reference to an instance space that the newly constructed path-finding state will be
     *                          become bound to
     * @return                  The deserialized path-finder state and its internal representation previously serialized
     * @throws IOException If a disk I/O error occurs
     */
    public static HydrazinePathFinder restore(String fileName, IInstanceSpace instanceSpace) throws IOException {
        return restore(instanceSpace, new File(fileName));
    }

    /**
     * Serialize a path-finder and its internal state directly to a file on disk.  This takes a snapshot of the
     * path-finding object and it's referenced objects at the time this is called.
     *
     * @param pathFinder Path-finder object whose state shall be frozen to the stream
     * @param file File object where the snapshot will be saved at
     * @throws IOException If a disk I/O error occurs
     * @see #restore(String, IInstanceSpace)
     */
    public static void persist(HydrazinePathFinder pathFinder, File file) throws IOException {
        final FileOutputStream fileOut = new FileOutputStream(file);
        try {
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            try {
                Persistence.persist(pathFinder, (ObjectOutput)out);
            } finally {
                out.close();
            }
        } finally {
            fileOut.close();
        }
    }

    /**
     * Deserialize a path-finder state directly from a file on disk.  This loads a frozen snapshot into a new path-finder
     * object that can be inspected for the purpose of troubleshooting or integration testing.
     *
     * @param file              File object pointing to a file containing a frozen snapshot
     * @param instanceSpace     A reference to an instance space that the newly constructed path-finding state will be
     *                          become bound to
     * @return                  The deserialized path-finder state and its internal representation previously serialized
     * @throws IOException If a disk I/O error occurs
     */
    public static HydrazinePathFinder restore(IInstanceSpace instanceSpace, File file) throws IOException {
        final FileInputStream fileIn = new FileInputStream(file);
        try {
            return Persistence.restore(fileIn, instanceSpace);
        } finally {
            fileIn.close();
        }
    }

    /**
     * Serialize a path-finder and its internal state to a stream.  This takes a snapshot of the path-finding object
     * and it's referenced objects at the time this is called.
     *
     * @param pathFinder Path-finder object whose state shall be frozen to the stream
     * @param out Stream to which to write the state
     * @throws IOException If there was an underlying stream error
     * @see #restore(InputStream, IInstanceSpace)
     */
    public static void persist(HydrazinePathFinder pathFinder, OutputStream out) throws IOException {
        persist(pathFinder, (ObjectOutput) new ObjectOutputStream(out));
    }

    /**
     * Deserialize a path-finder state from a stream.  This loads a frozen snapshot into a new path-finder object
     * that can be inspected for the purpose of troubleshooting or integration testing.
     *
     * NOTE: Some reconstructed objects are "dummies" and not real objects where third-party integrations are concerned
     *
     * @param in             Stream from which to load the path-finder state from
     * @param instanceSpace  A reference to an instance space that the newly constructed path-finding state will be
     *                       become bound to
     * @return               The deserialized path-finder state and its internal representation previously serialized
     * @throws IOException  If there was an underlying stream error
     * @see #persist(HydrazinePathFinder, OutputStream)
     */
    public static HydrazinePathFinder restore(InputStream in, IInstanceSpace instanceSpace) throws IOException {
        return restore((ObjectInput) new ObjectInputStream(in), instanceSpace);
    }

}
