package com.extollit.gaming.ai.path.model;

public class FlagSampler {
    private final IOcclusionProvider op;
    private int volatileCount;

    public FlagSampler(IOcclusionProvider op) {
        this.op = op;
    }

    public byte flagsAt(int x, int y, int z) {
        byte flags = this.op.elementAt(x, y, z);
        if (volatileIn(flags))
            this.volatileCount++;
        return flags;
    }

    private boolean volatileIn(byte flags) {
        return Logic.doorway.in(flags);
    }

    public int volatility() { return this.volatileCount; }
}
