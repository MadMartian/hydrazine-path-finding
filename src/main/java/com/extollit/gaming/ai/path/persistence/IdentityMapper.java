package com.extollit.gaming.ai.path.persistence;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.MessageFormat;
import java.util.*;

public class IdentityMapper<T, RW extends PartialObjectReader<T> & PartialObjectWriter<T>> {
    private final Map<T, Short> reverseMap = new IdentityHashMap<>();
    private final List<T> map = new ArrayList<>();
    private final RW readerWriter;

    public IdentityMapper(RW readerWriter) {
        this.readerWriter = readerWriter;
    }

    public void initialize(LinkableWriter<T, T> writer, Collection<T> iterable, ObjectOutput out) throws IOException {
        final List<T> map = this.map;
        if (!map.isEmpty())
            throw new IllegalStateException("Already initialized");

        final RW readerWriter = this.readerWriter;
        final ReferenceRecorder<T> recorder = new ReferenceRecorder<T>();
        final Map<T, Short> reverseMap = this.reverseMap;

        Collection<T> source = iterable;
        do {
            for (T object : source) {
                register(object);
                writer.writeLinkages(object, recorder);
            }
            source = recorder.popRecorded(reverseMap.keySet());
        } while (!source.isEmpty());

        if (map.size() > Short.MAX_VALUE)
            throw new IOException("Too many objects");

        out.writeShort(map.size());
        for (T object : map)
            readerWriter.writePartialObject(object, out);

        writeLinksInternal(writer, map, out);
    }

    public Iterable<T> readAll(ObjectInput in) throws IOException {
        clear();

        short count = in.readShort();
        final RW readerWriter = this.readerWriter;
        while (count-- > 0) {
            T object = readerWriter.readPartialObject(in);
            register(object);
        }

        return new ArrayList<>(this.map);
    }

    public <A, W extends PartialObjectWriter<A> & LinkableWriter<A, T>> void writeWith(W writer, Iterable<A> source, ObjectOutput out) throws IOException {
        final List<A> list = new LinkedList<>();
        for (A elem : source)
            list.add(elem);

        out.writeInt(list.size());
        final ReferenceWriter refOut = new ReferenceWriter(out);
        for (A object : list) {
            writer.writePartialObject(object, out);
            writer.writeLinkages(object, refOut);
        }

        writeLinksInternal(writer, list, out);
    }

    private <A, W extends LinkableWriter<A, T>> void writeLinksInternal(W writer, Iterable<A> source, ObjectOutput out) throws IOException {
        final ReferenceWriter refOut = new ReferenceWriter(out);
        for (A object : source)
            writer.writeLinkages(object, refOut);
    }

    public <A, W extends LinkableWriter<A, T>> void writeLinks(W writer, A object, ObjectOutput out) throws IOException {
        writer.writeLinkages(object, new ReferenceWriter(out));
    }

    public <A, R extends PartialObjectReader<A> & LinkableReader<A, T>> List<A> readWith(R reader, ObjectInput in) throws IOException {
        int count = in.readInt();
        List<A> results = new ArrayList<>(count);
        final ReferenceReader refIn = new ReferenceReader(in);
        while (count-- > 0) {
            final A object = reader.readPartialObject(in);
            results.add(object);
            reader.readLinkages(object, refIn);
        }

        readLinks(reader, results, in);

        return results;
    }

    public <A, R extends LinkableReader<A, T>> void readLinks(R reader, A object, ObjectInput in) throws IOException {
        reader.readLinkages(object, new ReferenceReader(in));
    }

    public <A, R extends LinkableReader<A, T>> void readLinks(R reader, Iterable<A> results, ObjectInput in) throws IOException {
        final ReferenceReader refIn = new ReferenceReader(in);
        for (A object : results)
            reader.readLinkages(object, refIn);
    }

    private void register(T object) throws IOException {
        final List<T> map = this.map;
        if (map.size() >= Short.MAX_VALUE)
            throw new IOException("Too many objects");

        final Map<T, Short> reverseMap = this.reverseMap;
        if (!reverseMap.containsKey(object)) {
            reverseMap.put(object, (short) map.size());
            map.add(object);
        }
    }

    public void clear() {
        reverseMap.clear();
        map.clear();
    }

    private static final class ReferenceRecorder<T> implements ReferableObjectOutput<T> {
        private final Set<T> recorded = new HashSet<>();

        public final Set<T> popRecorded(Collection<T> pop) {
            final Set<T> recorded = this.recorded;
            recorded.removeAll(pop);
            return Collections.unmodifiableSet(recorded);
        }

        @Override
        public void writeRef(T object) throws IOException {
            this.recorded.add(object);
        }

        @Override
        public void writeObject(Object obj) throws IOException {}

        @Override
        public void write(int b) throws IOException {}

        @Override
        public void write(byte[] b) throws IOException {}

        @Override
        public void write(byte[] b, int off, int len) throws IOException {}

        @Override
        public void writeBoolean(boolean v) throws IOException {}

        @Override
        public void writeByte(int v) throws IOException {}

        @Override
        public void writeShort(int v) throws IOException {}

        @Override
        public void writeChar(int v) throws IOException {}

        @Override
        public void writeInt(int v) throws IOException {}

        @Override
        public void writeLong(long v) throws IOException {}

        @Override
        public void writeFloat(float v) throws IOException {}

        @Override
        public void writeDouble(double v) throws IOException {}

        @Override
        public void writeBytes(String s) throws IOException {}

        @Override
        public void writeChars(String s) throws IOException {}

        @Override
        public void writeUTF(String s) throws IOException {}

        @Override
        public void flush() throws IOException {}

        @Override
        public void close() throws IOException {}
    }

    private final class ReferenceWriter implements ReferableObjectOutput<T> {
        private final ObjectOutput delegate;

        public ReferenceWriter(ObjectOutput delegate) {
            this.delegate = delegate;
        }

        @Override
        public void writeObject(Object obj) throws IOException {
            delegate.writeObject(obj);
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public void writeBoolean(boolean v) throws IOException {
            delegate.writeBoolean(v);
        }

        @Override
        public void writeByte(int v) throws IOException {
            delegate.writeByte(v);
        }

        @Override
        public void writeShort(int v) throws IOException {
            delegate.writeShort(v);
        }

        @Override
        public void writeChar(int v) throws IOException {
            delegate.writeChar(v);
        }

        @Override
        public void writeInt(int v) throws IOException {
            delegate.writeInt(v);
        }

        @Override
        public void writeLong(long v) throws IOException {
            delegate.writeLong(v);
        }

        @Override
        public void writeFloat(float v) throws IOException {
            delegate.writeFloat(v);
        }

        @Override
        public void writeDouble(double v) throws IOException {
            delegate.writeDouble(v);
        }

        @Override
        public void writeBytes(String s) throws IOException {
            delegate.writeBytes(s);
        }

        @Override
        public void writeChars(String s) throws IOException {
            delegate.writeChars(s);
        }

        @Override
        public void writeUTF(String s) throws IOException {
            delegate.writeUTF(s);
        }

        @Override
        public void writeRef(T object) throws IOException {
            final Short id = reverseMap.get(object);
            if (id == null)
                throw new IOException(MessageFormat.format("Missing instance for object {0}", object));
            else
                delegate.writeShort(id);
        }
    }

    private final class ReferenceReader implements ReferableObjectInput<T> {
        private final ObjectInput delegate;

        private ReferenceReader(ObjectInput delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object readObject() throws ClassNotFoundException, IOException {
            return delegate.readObject();
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return delegate.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            delegate.readFully(b);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            delegate.readFully(b, off, len);
        }

        @Override
        public int skipBytes(int n) throws IOException {
            return delegate.skipBytes(n);
        }

        @Override
        public boolean readBoolean() throws IOException {
            return delegate.readBoolean();
        }

        @Override
        public byte readByte() throws IOException {
            return delegate.readByte();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return delegate.readUnsignedByte();
        }

        @Override
        public short readShort() throws IOException {
            return delegate.readShort();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return delegate.readUnsignedShort();
        }

        @Override
        public char readChar() throws IOException {
            return delegate.readChar();
        }

        @Override
        public int readInt() throws IOException {
            return delegate.readInt();
        }

        @Override
        public long readLong() throws IOException {
            return delegate.readLong();
        }

        @Override
        public float readFloat() throws IOException {
            return delegate.readFloat();
        }

        @Override
        public double readDouble() throws IOException {
            return delegate.readDouble();
        }

        @Override
        public String readLine() throws IOException {
            return delegate.readLine();
        }

        @Override
        public String readUTF() throws IOException {
            return delegate.readUTF();
        }

        @Override
        public T readRef() throws IOException {
            final short id = delegate.readShort();
            if (id >= map.size() || id < 0)
                throw new IOException("Invalid object reference received in stream: " + id);

            return map.get(id);
        }
    }
}
