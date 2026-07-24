package com.dcco.app.iSilo.engine;

import com.dcco.app.iSilo.engine.data.DataStream;

public abstract class PalmDB {

    public PalmDB() {
    }

    public int DeleteRecord(int index) {
        return -2147483643;
    }

    public int Destroy() {
        return -2147483643;
    }

    public int GetInfo(byte[] name, byte[] creator, byte[] type,
                       int[] recordCount, int[] flagsOut, int[] bodySizeOut) {
        return -2147483643;
    }

    public int GetRecord(int index, int[] sizeOut, byte[][] dataOut) {
        return -2147483643;
    }

    public int MoveRecord(int from, int to) {
        return -2147483643;
    }

    public int NewRecord(int[] indexOut, int size, byte[] data, int flags) {
        return -2147483643;
    }

    public int Open(DataStream stream, int mode) {
        return -2147483643;
    }

    public int OpenRecord(int index, int[] sizeOut, DataStream[] streamOut) {
        return -2147483643;
    }

    public int SetInfo(byte[] name, byte[] creator, byte[] type,
                      int flags, int bodySize) {
        return -2147483643;
    }

    public int SizeRecord(int index, int size) {
        return -2147483643;
    }

    public int UnloadRecord(byte[] data) {
        return -2147483643;
    }
}
