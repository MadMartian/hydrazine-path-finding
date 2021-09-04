package com.extollit.gaming.ai.path.model;

public class OcclusionField implements IOcclusionProvider {
    public enum AreaInit {
        north   (0, -1),
        south   (0, +1),
        west    (-1, 0),
        east    (+1, 0),

        northEast   (+1, -1),
        northWest   (-1, -1),
        southEast   (+1, +1),
        southWest   (-1, +1),

        up      (1),
        down    (-1);

        public final byte dx, dy, dz;
        public final short mask = (short)(1 << ordinal());

        AreaInit(int dx, int dz) {
            this.dx = (byte) dx;
            this.dy = 0;
            this.dz = (byte) dz;
        }
        AreaInit(int dy) {
            this.dx = this.dz = 0;
            this.dy = (byte) dy;
        }

        public boolean in(short flags) { return (flags & mask) != 0; }
        public short to(short flags) { return (short)(flags | mask); }

        public static AreaInit given(int dx, int dy, int dz) {
            if (dx == -1 && dz == -1)
                return northWest;
            else
            if (dx == 0 && dz == -1)
                return north;
            else
            if (dx == +1 && dz == -1)
                return northEast;
            else
            if (dx == +1 && dz == 0)
                return east;
            else
            if (dx == +1 && dz == +1)
                return southEast;
            else
            if (dx == 0 && dz == +1)
                return south;
            else
            if (dx == -1 && dz == +1)
                return southWest;
            else
            if (dx == -1 && dz == 0)
                return west;
            else
            if (dx == 0 && dz == 0)
                if (dy == -1)
                    return down;
                else
                if (dy == +1)
                    return up;

            return null;
        }
    }

    private static final byte
            ELEMENT_LENGTH_SHL = 2,
            ELEMENT_LENGTH = 1 << ELEMENT_LENGTH_SHL,
            WORD_LENGTH = 64,
            ELEMENTS_PER_WORD = WORD_LENGTH / ELEMENT_LENGTH,
            WORD_LAST_OFFSET = ELEMENTS_PER_WORD - 1,
            COORDINATE_TO_INDEX_SHR = 4,
            DIMENSION_ORDER = 4;

    public static final int
            DIMENSION_SIZE = 1 << DIMENSION_ORDER;

    static final int
            DIMENSION_MASK = (1 << DIMENSION_ORDER) - 1,
            DIMENSION_EXTENT = DIMENSION_SIZE - 1;

    private static final int
            DIMENSION_SQUARE_SIZE = DIMENSION_SIZE * DIMENSION_SIZE,
            LAST_INDEX = (DIMENSION_SIZE * DIMENSION_SQUARE_SIZE - 1) >> COORDINATE_TO_INDEX_SHR;

    private static final long
            ELEMENT_MASK = (1 << ELEMENT_LENGTH) - 1;

    private static final short
            FULLY_AREA_INIT = 0x3FF;

    private long [] words;
    private byte singleton;
    private short areaInit;

    public OcclusionField() {}

    public boolean areaInitFull() {
        return this.areaInit == FULLY_AREA_INIT;
    }
    public boolean areaInitAt(AreaInit direction) {
        return direction.in(this.areaInit);
    }

    public static boolean fuzzyOpenIn(byte element) {
        return Element.air.in(element) || (Element.earth.in(element) && Logic.fuzzy.in(element));
    }

    public void loadFrom(IColumnarSpace columnarSpace, int cx, int cy, int cz) {
        this.singleton = 0;
        this.words = new long[DIMENSION_SQUARE_SIZE * DIMENSION_SIZE * ELEMENT_LENGTH / WORD_LENGTH];

        boolean compress = true;
        byte lastFlags = this.singleton;

        final int
                x0 = cx << DIMENSION_ORDER,
                y0 = cy << DIMENSION_ORDER, yN = y0 + DIMENSION_SIZE,
                z0 = cz << DIMENSION_ORDER;

        final long[] words = this.words;

        final int yNi = yN - 1;
        for (int y = yNi, i = LAST_INDEX; y >= y0; --y)
            for (int z = DIMENSION_EXTENT; z >= 0; --z)
                for (int x = DIMENSION_SIZE - ELEMENTS_PER_WORD; x >= 0; x -= ELEMENTS_PER_WORD) {
                    long word = 0;
                    for (int b = WORD_LAST_OFFSET; b >= 0; --b) {
                        final int xx = x + b;
                        final IBlockDescription blockDescription = columnarSpace.blockAt(xx, y, z);
                        final byte flags = flagsFor(columnarSpace, x0 + xx, y, z0 + z, blockDescription);
                        compress &= (lastFlags == flags) || (i == LAST_INDEX && b == WORD_LAST_OFFSET);
                        lastFlags = flags;
                        word <<= (1 << ELEMENT_LENGTH_SHL);
                        word |= (long)flags;

                        if (blockDescription.isFenceLike() && y < yNi) {
                            final int indexUp = i + (DIMENSION_SQUARE_SIZE >> COORDINATE_TO_INDEX_SHR);
                            words[indexUp] = modifyWord(words[indexUp], b, flags);
                        }
                    }
                    words[i--] = word;
                }

        if (compress) {
            this.words = null;
            this.singleton = lastFlags;
        } else
            areaInit();
    }

    private boolean fenceOrDoorLike(byte flags) {
        return (Element.earth.in(flags) && Logic.fuzzy.in(flags)) || (Logic.doorway.in(flags));
    }

    private void decompress() {
        long word = singletonWord();
        final long[] words = this.words = new long[DIMENSION_SQUARE_SIZE * DIMENSION_SIZE * ELEMENT_LENGTH / WORD_LENGTH];
        for (int i = 0; i < words.length; ++i)
            words[i] = word;

        this.singleton = 0;
    }

    private long singletonWord() {
        final byte singleton = this.singleton;
        long word = 0;
        for (int b = ELEMENTS_PER_WORD; b > 0; --b) {
            word <<= 1 << ELEMENT_LENGTH_SHL;
            word |= singleton;
        }
        return word;
    }

    private void areaInit() {
        final long[] words = this.words;
        if (words == null)
            return;

        for (int y = 0, index = DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR; y < DIMENSION_SIZE; ++y) {
            for (int z = 1; z < DIMENSION_EXTENT; ++z)
                for (int x = 0; x < DIMENSION_SIZE; x += ELEMENTS_PER_WORD)
                {
                    long
                            word = words[index];
                    final long
                            northWord = words[index - (DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR)],
                            southWord = words[index + (DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR)];

                    for (
                            int b = x == 0 ? 1 : 0,
                            bN = ELEMENTS_PER_WORD - (x + ELEMENTS_PER_WORD >= DIMENSION_SIZE ? 1 : 0);
                            b < bN;
                            ++b
                            ) {

                        final long
                                westWord = words[index - ((b - 1) >> COORDINATE_TO_INDEX_SHR)],
                                eastWord = words[index + ((b + 1) >> COORDINATE_TO_INDEX_SHR)];

                        word = areaWordFor(word, b, northWord, eastWord, southWord, westWord);
                    }
                    words[index++] = word;
                }

            index += ((2 * DIMENSION_SIZE) >> COORDINATE_TO_INDEX_SHR);
        }
    }

    void areaInitNorth(OcclusionField other) {
        areaInitZPlane(other, false);
        this.areaInit = AreaInit.north.to(this.areaInit);
    }
    void areaInitSouth(OcclusionField other) {
        areaInitZPlane(other, true);
        this.areaInit = AreaInit.south.to(this.areaInit);
    }
    void areaInitWest(OcclusionField other) {
        areaInitXPlane(other, false);
        this.areaInit = AreaInit.west.to(this.areaInit);
    }
    void areaInitEast(OcclusionField other) {
        areaInitXPlane(other, true);
        this.areaInit = AreaInit.east.to(this.areaInit);
    }
    void areaInitNorthEast(OcclusionField horizontal, OcclusionField depth) {
        areaInitVerticalEdge(horizontal, depth, true, false);
        this.areaInit = AreaInit.northEast.to(this.areaInit);
    }
    void areaInitSouthEast(OcclusionField horizontal, OcclusionField depth) {
        areaInitVerticalEdge(horizontal, depth, true, true);
        this.areaInit = AreaInit.southEast.to(this.areaInit);
    }
    void areaInitNorthWest(OcclusionField horizontal, OcclusionField depth) {
        areaInitVerticalEdge(horizontal, depth, false, false);
        this.areaInit = AreaInit.northWest.to(this.areaInit);
    }
    void areaInitSouthWest(OcclusionField horizontal, OcclusionField depth) {
        areaInitVerticalEdge(horizontal, depth, false, true);
        this.areaInit = AreaInit.southWest.to(this.areaInit);
    }
    void areaInitUp(IColumnarSpace columnarSpace, int cy, OcclusionField other) {
        resolveTruncatedFencesAndDoors(columnarSpace, cy, other, true);
        this.areaInit = AreaInit.up.to(this.areaInit);
    }
    void areaInitDown(IColumnarSpace columnarSpace, int cy, OcclusionField other) {
        resolveTruncatedFencesAndDoors(columnarSpace, cy, other, false);
        this.areaInit = AreaInit.down.to(this.areaInit);
    }

    private void resolveTruncatedFencesAndDoors(IColumnarSpace columnarSpace, int cy, OcclusionField other, final boolean end) {
        if (other == null)
            return;

        int i = LAST_INDEX - (DIMENSION_SQUARE_SIZE >> COORDINATE_TO_INDEX_SHR) + 1;
        final OcclusionField subject, object;

        if (end)
        {
            subject = this;
            object = other;
            cy++;
        } else {
            subject = other;
            object = this;
        }

        final int y = (cy << DIMENSION_ORDER) - 1;

        final long[] words = subject.words;
        final byte singleton = subject.singleton;
        long word = 0;

        for (int z = 0; z < DIMENSION_SIZE; ++z)
            for (int x = 0; x < DIMENSION_SIZE; x += ELEMENTS_PER_WORD) {
                if (words != null)
                    word = words[i++];
                for (int b = 0; b < ELEMENTS_PER_WORD; ++b) {
                    final int xx = x + b;
                    final byte flags;

                    if (words != null)
                        flags = (byte)(word & ELEMENT_MASK);
                    else
                        flags = singleton;

                    final boolean
                        fenceLike = Element.earth.in(flags) && Logic.fuzzy.in(flags),
                        doorLike = Logic.doorway.in(flags);

                    if (fenceLike || doorLike) {
                        final IBlockDescription block = columnarSpace.blockAt(xx, y, z);
                        if (fenceLike && block.isFenceLike() || doorLike && block.isDoor())
                            object.set(xx, 0, z, flags);
                    }

                    word >>= 1 << ELEMENT_LENGTH_SHL;
                }
            }
    }

    private void areaInitZPlane(OcclusionField neighbor, final boolean end) {
        long[] words = this.words;
        final int
            z0 = end ? DIMENSION_EXTENT : 0,
            disposition = ((z0 / (DIMENSION_EXTENT)) << 1) - 1;
        final long
                neighborWords[] = neighbor.words,
                singletonWord = words == null ? singletonWord() : 0,
                neighborSingletonWord = neighborWords == null ? neighbor.singletonWord() : 0;
        for (int y = 0,
             index = z0 * DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR,
             northIndex = (DIMENSION_SIZE - z0 - 1) * DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR;

             y < DIMENSION_SIZE;

             ++y
        ) {
            for (int x = 0; x < DIMENSION_SIZE; x += ELEMENTS_PER_WORD) {
                long word = words == null ? singletonWord : words[index];
                final long
                        northWord,
                        southWord;

                {
                    final long
                        primary = words == null ? singletonWord : words[index + -disposition * (DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR)],
                        secondary = neighborWords == null ? neighborSingletonWord : neighborWords[northIndex];

                    if (disposition < 0) {
                        northWord = secondary;
                        southWord = primary;
                    } else {
                        northWord = primary;
                        southWord = secondary;
                    }
                }

                for (
                        int b = x == 0 ? 1 : 0,
                        bN = ELEMENTS_PER_WORD - (x + ELEMENTS_PER_WORD >= DIMENSION_SIZE ? 1 : 0);
                        b < bN;
                        ++b
                    ) {

                    final long westWord, eastWord;

                    if (words == null)
                        eastWord = westWord = singletonWord;
                    else {
                        westWord = words[index - ((b - 1) >> COORDINATE_TO_INDEX_SHR)];
                        eastWord = words[index + ((b + 1) >> COORDINATE_TO_INDEX_SHR)];
                    }

                    word = areaWordFor(word, b, northWord, eastWord, southWord, westWord);
                }
                if (words == null && word != singletonWord) {
                    decompress();
                    words = this.words;
                }

                if (words != null)
                    words[index] = word;

                index++;
                northIndex++;
            }

            final int di = ((DIMENSION_EXTENT) * DIMENSION_SIZE) >> COORDINATE_TO_INDEX_SHR;
            index += di;
            northIndex += di;
        }
    }

    private void areaInitXPlane(OcclusionField neighbor, final boolean end) {
        long[] words = this.words;
        final int
            x0 = end ? DIMENSION_EXTENT : 0,
            disposition = ((x0 / (DIMENSION_EXTENT)) << 1) - 1,
            offset = ((disposition + 1) >> 1) * WORD_LAST_OFFSET;

        final long
            neighborWords[] = neighbor.words,
            singletonWord = words == null ? singletonWord() : 0,
                neighborSingletonWord = neighborWords == null ? neighbor.singletonWord() : 0;
        for (int y = 0,
             index = x0 + DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR,
             neighborIndex = (DIMENSION_SIZE - x0 - 1) + DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR;

             y < DIMENSION_SIZE;

             ++y
        ) {
            for (int z = 1; z < DIMENSION_EXTENT; ++z) {
                long word = words == null ? singletonWord : words[index];
                final long
                        westWord,
                        eastWord,
                        northWord,
                        southWord;

                {
                    final long
                            primary = words == null ? singletonWord : words[index],
                            secondary = neighborWords == null ? neighborSingletonWord : neighborWords[neighborIndex];

                    if (disposition < 0) {
                        westWord = secondary;
                        eastWord = primary;
                    } else {
                        westWord = primary;
                        eastWord = secondary;
                    }
                }

                if (words == null)
                    southWord = northWord = singletonWord;
                else {
                    northWord = words[index - (DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR)];
                    southWord = words[index + (DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR)];
                }

                word = areaWordFor(word, offset, northWord, eastWord, southWord, westWord);

                if (words == null && word != singletonWord) {
                    decompress();
                    words = this.words;
                }

                if (words != null)
                    words[index] = word;

                final int di = DIMENSION_SIZE >> COORDINATE_TO_INDEX_SHR;
                index += di;
                neighborIndex += di;
            }

            final int di = (DIMENSION_SIZE * 2) >> COORDINATE_TO_INDEX_SHR;
            index += di;
            neighborIndex += di;
        }
    }

    private void areaInitVerticalEdge(OcclusionField horizNeighbor, OcclusionField depthNeighbor, final boolean horizEnd, final boolean depthEnd) {
        long[] words = this.words;
        final int
                x0 = horizEnd ? DIMENSION_EXTENT : 0,
                z0 = depthEnd ? DIMENSION_EXTENT : 0,
                xd = ((x0 / (DIMENSION_EXTENT)) << 1) - 1,
                zd = ((z0 / (DIMENSION_EXTENT)) << 1) - 1,
                offset = ((xd + 1) >> 1) * WORD_LAST_OFFSET;

        final long
                horizNeighborWords[] = horizNeighbor.words,
                depthNeighborWords[] = depthNeighbor.words,
                singletonWord = words == null ? singletonWord() : 0,
                horizNeighborSingletonWord = horizNeighborWords == null ? horizNeighbor.singletonWord() : 0,
                depthNeighborSingletonWord = depthNeighborWords == null ? depthNeighbor.singletonWord() : 0;

        for (int y = 0,
             index = z0 * DIMENSION_SIZE + x0 >> COORDINATE_TO_INDEX_SHR,
             horizNeighborIndex = z0 * DIMENSION_SIZE + (DIMENSION_SIZE - x0 - 1) >> COORDINATE_TO_INDEX_SHR,
             depthNeighborIndex = (DIMENSION_SIZE - z0 - 1) * DIMENSION_SIZE + x0 >> COORDINATE_TO_INDEX_SHR;

             y < DIMENSION_SIZE;

             ++y
        ) {
            long word = words == null ? singletonWord : words[index];
            final long
                    westWord,
                    eastWord,
                    northWord,
                    southWord;

            {
                final long
                        horizSecondary = horizNeighborWords == null ? horizNeighborSingletonWord : horizNeighborWords[horizNeighborIndex],
                        depthSecondary = depthNeighborWords == null ? depthNeighborSingletonWord : depthNeighborWords[depthNeighborIndex];

                if (xd < 0) {
                    westWord = horizSecondary;
                    eastWord = word;
                } else {
                    westWord = word;
                    eastWord = horizSecondary;
                }

                if (zd < 0) {
                    northWord = depthSecondary;
                    southWord = word;
                } else {
                    northWord = word;
                    southWord = depthSecondary;
                }
            }

            word = areaWordFor(word, offset, northWord, eastWord, southWord, westWord);

            if (words == null && word != singletonWord) {
                decompress();
                words = this.words;
            }

            if (words != null)
                words[index] = word;

            final int di = (DIMENSION_SIZE * DIMENSION_SIZE) >> COORDINATE_TO_INDEX_SHR;
            index += di;
            horizNeighborIndex += di;
            depthNeighborIndex += di;
        }
    }

    private long areaWordFor(long centerWord, int offset, long northWord, long eastWord, long southWord, long westWord) {
        byte
                centerFlags = elementAt(centerWord, offset);

        final byte
                northFlags = elementAt(northWord, offset),
                southFlags = elementAt(southWord, offset),
                westFlags = westFlags(offset, westWord),
                eastFlags = eastFlags(offset, eastWord);

        centerFlags = areaFlagsFor(centerFlags, northFlags, eastFlags, southFlags, westFlags);
        centerWord = modifyWord(centerWord, offset, centerFlags);
        return centerWord;
    }

    private long fenceAndDoorAreaWordFor(IColumnarSpace columnarSpace, int dx, int y, int dz, long centerWord, int offset, long upWord, long downWord, boolean handlingFenceTops) {
        byte
                centerFlags = elementAt(centerWord, offset);
        final byte
                upFlags = elementAt(upWord, offset),
                downFlags = elementAt(downWord, offset);

        centerFlags = fenceAndDoorAreaFlagsFor(columnarSpace, dx, y, dz, centerFlags, upFlags, downFlags, handlingFenceTops);
        centerWord = modifyWord(centerWord, offset, centerFlags);
        return centerWord;
    }

    private byte eastFlags(int offset, long eastWord) {
        return elementAt(eastWord, (offset + 1) % ELEMENTS_PER_WORD);
    }

    private byte westFlags(int offset, long westWord) {
        return elementAt(westWord, (offset + ELEMENTS_PER_WORD - 1) % ELEMENTS_PER_WORD);
    }

    private byte areaFlagsFor(byte centerFlags, byte northFlags, byte eastFlags, byte southFlags, byte westFlags) {
        final Element
            northElem = Element.of(northFlags),
            southElem = Element.of(southFlags),
            westElem = Element.of(westFlags),
            eastElem = Element.of(eastFlags),
            centerElem = Element.of(centerFlags);

        if (Logic.ladder.in(centerFlags) && (northElem == Element.earth || eastElem == Element.earth || southElem == Element.earth || westElem == Element.earth))
            centerFlags = Element.earth.to(centerFlags);
        else if (centerElem == Element.air && Logic.nothing.in(centerFlags)
                && (
                    fuzziable(centerElem, northFlags) ||
                    fuzziable(centerElem, eastFlags) ||
                    fuzziable(centerElem, southFlags) ||
                    fuzziable(centerElem, westFlags)
                )
            )
            centerFlags = Logic.fuzzy.to(centerFlags);

        return centerFlags;
    }

    private byte fenceAndDoorAreaFlagsFor(IColumnarSpace columnarSpace, int dx, int y, int dz, byte centerFlags, byte upFlags, byte downFlags, boolean handlingFenceTops) {
        final boolean
            downFenceOrDoorLike = fenceOrDoorLike(downFlags),
            centerFenceOrDoorLike = fenceOrDoorLike(centerFlags);

        final IBlockDescription
            centerBlock = columnarSpace.blockAt(dx, y, dz),
            downBlock = columnarSpace.blockAt(dx, y - 1, dz);

        if (!centerBlock.isImpeding()) {

            if (downFenceOrDoorLike && !centerFenceOrDoorLike && downBlock.isFenceLike()
                    ||
                !handlingFenceTops && !downFenceOrDoorLike && centerFenceOrDoorLike && !centerBlock.isFenceLike()
                    ||
                downFenceOrDoorLike && centerFenceOrDoorLike &&
                    (
                        Logic.doorway.in(downFlags) && (downBlock.isFenceLike() && downBlock.isDoor() && (Element.doorClosedLike(downFlags) || !(centerBlock.isDoor() && centerBlock.isFenceLike())))
                            ||
                        Logic.doorway.in(centerFlags) && !(downBlock.isFenceLike() && downBlock.isDoor())
                    )
            )
                return downFlags;
        } else if (
            downFenceOrDoorLike && centerFenceOrDoorLike &&
            Logic.doorway.in(downFlags) && Logic.doorway.in(centerFlags) &&
            downBlock.isDoor() && centerBlock.isDoor()
        )
            return downFlags;

        return centerFlags;
    }

    private boolean fuzziable(Element centerElem, byte otherFlags) {
        final Element otherElement = Element.of(otherFlags);
        return centerElem != otherElement && !(otherElement == Element.earth && Logic.fuzzy.in(otherFlags));
    }

    private long modifyWord(long word, int offset, byte flags) {
        final int shl = offset << ELEMENT_LENGTH_SHL;
        return (word & ~(ELEMENT_MASK << shl)) | (((long)flags) << shl);
    }

    @SuppressWarnings("unused")
    public void set(IColumnarSpace columnarSpace, int x, int y, int z, IBlockDescription blockDescription) {
        final int
                dx = x & DIMENSION_MASK,
                dy = y & DIMENSION_MASK,
                dz = z & DIMENSION_MASK;

        final byte flags = flagsFor(columnarSpace, x, y, z, blockDescription);

        if (set(dx, dy, dz, flags))
        {
            final boolean
                    dzb = dz > 0 && dz < DIMENSION_EXTENT,
                    dxb = dx > 0 && dx < DIMENSION_EXTENT,
                    dyb = dy > 0 && dy < DIMENSION_EXTENT;

            if (dzb && dxb && dyb)
                areaComputeAt(dx, dy, dz);
            else
                greaterAreaComputeAt(columnarSpace, x, y, z);

            if (dx > 1 && dzb)
                areaComputeAt(dx - 1, dy, dz);
            else
                greaterAreaComputeAt(columnarSpace, x - 1, y, z);

            if (dx < DIMENSION_EXTENT - 1 && dzb)
                areaComputeAt(dx + 1, dy, dz);
            else
                greaterAreaComputeAt(columnarSpace, x + 1, y, z);

            if (dz > 1 && dxb)
                areaComputeAt(dx, dy, dz - 1);
            else
                greaterAreaComputeAt(columnarSpace, x, y, z - 1);

            if (dz < DIMENSION_EXTENT - 1 && dxb)
                areaComputeAt(dx, dy, dz + 1);
            else
                greaterAreaComputeAt(columnarSpace, x, y, z + 1);

            if (dy > 0 && dy < DIMENSION_EXTENT)
                fencesAndDoorsComputeAt(columnarSpace, dx, y, dz, true);
            else if (y > 0 && y < (DIMENSION_SIZE << 4) - 2)
                greaterFencesAndDoorsComputeAt(columnarSpace, x, y, z, true);

            if (dy > 1)
                fencesAndDoorsComputeAt(columnarSpace, dx, y - 1, dz, false);
            else if (y > 1)
                greaterFencesAndDoorsComputeAt(columnarSpace, x, y - 1, z, false);

            if (dy < DIMENSION_EXTENT - 1)
                fencesAndDoorsComputeAt(columnarSpace, dx, y + 1, dz, false);
            else if (y < (DIMENSION_SIZE << 4) - 2)
                greaterFencesAndDoorsComputeAt(columnarSpace, x, y + 1, z, false);
        }
    }

    private boolean set(int dx, int dy, int dz, byte flags) {
        if (this.words == null && flags != this.singleton)
            decompress();

        if (this.words != null) {
            final int index = index(dx, dy, dz);
            final long word = this.words[index];
            this.words[index] = modifyWord(word, dx % ELEMENTS_PER_WORD, flags);

            return true;
        }

        return false;
    }

    private int index(int dx, int dy, int dz) {
        return (dy * DIMENSION_SQUARE_SIZE + dz * DIMENSION_SIZE + dx) >> COORDINATE_TO_INDEX_SHR;
    }

    private void areaComputeAt(int dx, int dy, int dz) {
        final long[] words = this.words;
        final int
                offset = dx % ELEMENTS_PER_WORD,
                index = index(dx, dy, dz);
        final long
            northWord = words[index(dx, dy, dz - 1)],
            southWord = words[index(dx, dy, dz + 1)],
            westWord = words[index(dx - 1, dy, dz)],
            eastWord = words[index(dx + 1, dy, dz)],
            centerWord = words[index];

        words[index] = areaWordFor(centerWord, offset, northWord, eastWord, southWord, westWord);
    }

    private void fencesAndDoorsComputeAt(IColumnarSpace columnarSpace, int dx, int y, int dz, boolean handlingFenceTops) {
        final long[] words = this.words;
        final int
                dy = y & DIMENSION_MASK,
                offset = dx % ELEMENTS_PER_WORD,
                index = index(dx, dy, dz);
        final long
                downWord = words[index(dx, dy - 1, dz)],
                upWord = words[index(dx, dy + 1, dz)],
                centerWord = words[index];

        words[index] = fenceAndDoorAreaWordFor(columnarSpace, dx, y, dz, centerWord, offset, upWord, downWord, handlingFenceTops);
    }

    private void greaterAreaComputeAt(IColumnarSpace columnarSpace, int x, int y, int z) {
        final int
                dx = x & DIMENSION_MASK,
                dy = y & DIMENSION_MASK,
                dz = z & DIMENSION_MASK,
                cx = x >> DIMENSION_ORDER,
                cy = y >> DIMENSION_ORDER,
                cz = z >> DIMENSION_ORDER;

        final IInstanceSpace instance = columnarSpace.instance();

        final OcclusionField
            center = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, cy, cz);

        if (center == null)
            return;

        final OcclusionField
            north = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, cy, (z - 1) >> DIMENSION_ORDER),
            east = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, (x + 1) >> DIMENSION_ORDER, cy, cz),
            south = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, cy, (z + 1) >> DIMENSION_ORDER),
            west = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, (x - 1) >> DIMENSION_ORDER, cy, cz);

        byte
            centerFlags = center.elementAt(dx, dy, dz),
            northFlags = north == null ? 0 : north.elementAt(dx, dy, (dz - 1) & DIMENSION_MASK),
            southFlags = south == null ? 0 : south.elementAt(dx, dy, (dz + 1) & DIMENSION_MASK),
            westFlags = west == null ? 0 : west.elementAt((dx - 1) & DIMENSION_MASK, dy, dz),
            eastFlags = east == null ? 0 : east.elementAt((dx + 1) & DIMENSION_MASK, dy, dz);

        final byte flags = areaFlagsFor(centerFlags, northFlags, eastFlags, southFlags, westFlags);

        center.set(dx, dy, dz, flags);
    }

    private void greaterFencesAndDoorsComputeAt(IColumnarSpace columnarSpace, int x, int y, int z, boolean handlingFenceTops) {
        final int
                dx = x & DIMENSION_MASK,
                dy = y & DIMENSION_MASK,
                dz = z & DIMENSION_MASK,
                cx = x >> DIMENSION_ORDER,
                cy = y >> DIMENSION_ORDER,
                cz = z >> DIMENSION_ORDER;

        final IInstanceSpace instance = columnarSpace.instance();

        final OcclusionField
                center = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, cy, cz);

        if (center == null)
            return;

        final OcclusionField
                up = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, (y + 1) >> DIMENSION_ORDER, cz),
                down = ColumnarOcclusionFieldList.optOcclusionFieldAt(instance, cx, (y - 1) >> DIMENSION_ORDER, cz);

        byte
                centerFlags = center.elementAt(dx, dy, dz),
                upFlags = up == null ? 0 : up.elementAt(dx, (dy + 1) & DIMENSION_MASK, dz),
                downFlags = down == null ? 0 : down.elementAt(dx, (dy - 1) & DIMENSION_MASK, dz);

        final byte flags = fenceAndDoorAreaFlagsFor(columnarSpace, dx, y, dz, centerFlags, upFlags, downFlags, handlingFenceTops);

        center.set(dx, dy, dz, flags);
    }

    @Override
    public byte elementAt(int x, int y, int z) {
        byte element;
        if (this.words != null) {
            long word = this.words[index(x, y, z)];
            element = elementAt(word, x % ELEMENTS_PER_WORD);
        } else
            element = this.singleton;
        return element;
    }

    private byte elementAt(long word, final int offset) {
        byte element;
        element = (byte) (word >> (offset << ELEMENT_LENGTH_SHL));
        element &= ELEMENT_MASK;
        return element;
    }

    @Override
    public String visualizeAt(int dy) {
        return visualizeAt(this, dy, 0, 0, DIMENSION_SIZE, DIMENSION_SIZE);
    }

    private byte flagsFor(IColumnarSpace columnarSpace, int x, int y, int z, IBlockDescription block) {
        byte flags = 0;

        final IInstanceSpace instance = columnarSpace.instance();
        final boolean doorway = block.isDoor();

        if (doorway)
            flags |= (
                instance.blockObjectAt(x, y, z).isImpeding()
                    ? block.isIntractable() ? Element.fire : Element.earth
                    : Element.air
            ).mask;
        else if (!block.isImpeding())
            if (block.isLiquid())
                if (block.isIncinerating())
                    flags |= Element.fire.mask;
                else
                    flags |= Element.water.mask;
            else if (block.isIncinerating())
                flags |= Element.fire.mask | Logic.fuzzy.mask;
            else
                flags |= Element.air.mask;
        else if (block.isIncinerating())
            flags |= Element.fire.mask;
        else
            flags |= Element.earth.mask;

        if (doorway)
            flags = Logic.doorway.to(flags);
        else if (block.isClimbable())
            flags = Logic.ladder.to(flags);
        else if (Element.earth.in(flags) && !block.isFullyBounded())
            flags = Logic.fuzzy.to(flags);

        return flags;
    }

    static String visualizeAt(IOcclusionProvider provider, int dy, final int x0, final int z0, final int xN, final int zN) {
        final StringBuilder sb = new StringBuilder();

        for (int z = z0; z < zN; ++z) {
            for (int x = x0; x < xN; ++x)
            {
                final char ch;
                final byte flags = provider.elementAt(x, dy, z);
                switch (Element.of(flags)) {
                    case air:
                        ch = Logic.fuzzy.in(flags) ? '░' : ' ';
                        break;
                    case earth:
                        if (Logic.climbable(flags))
                            ch = '#';
                        else if (Logic.fuzzy.in(flags))
                            ch = '▄';
                        else
                            ch = '█';
                        break;
                    case fire:
                        ch = 'X';
                        break;
                    case water:
                        ch = '≋';
                        break;

                    default:
                        ch = '?';
                        break;
                }

                sb.append(ch);
            }
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
