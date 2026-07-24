package com.dcco.app.iSilo.engine.anno;

public abstract class AnnotationStorage {

    public static class Anno {
        public byte[] commentData;
        public byte[] textData;
        public Ent entry = new Ent();
        public int commentLength;
        public int commentOffset;
        public int index;
        public int textLength;
        public int textOffset;
    }

    public static class Attr {
        public byte color;
        public byte flags;
    }

    public static class Ent {
        public byte color;
        public byte flags;
        public int offset;
        public char wLength;
    }

    public AnnotationStorage() {
    }

    public int Add(Anno anno) {
        return -2147483643;
    }

    public int AdjustRange(int offset, int[] sizeOut, Attr attr) {
        return -2147483643;
    }

    public int DeInit() {
        return -2147483643;
    }

    public int Delete(int index) {
        return -2147483643;
    }

    public int Get(int index, int flags, Anno anno) {
        return -2147483643;
    }

    public int GetCount(int[] countOut) {
        return -2147483643;
    }

    public int Modify(int index, Anno anno) {
        return -2147483643;
    }

    public int ReOpenIfReadError() {
        return -2147483643;
    }
}
