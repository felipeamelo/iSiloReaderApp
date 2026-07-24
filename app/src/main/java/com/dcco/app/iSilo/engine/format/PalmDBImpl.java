package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.PalmDB;
import com.dcco.app.iSilo.engine.data.DataStream;
import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class PalmDBImpl extends PalmDB {

    private static final int PALM_HEADER_SIZE = 78;
    private static final int RECORD_ENTRY_SIZE = 8;

    private DataStream stream;
    private int mode;

    private byte[] name;
    private byte[] creator;
    private byte[] type;
    private int flags;
    private int bodySize;

    private int recordCount;
    private int[] recordOffsets;
    private int[] recordAttrs;

    @Override
    public int Open(DataStream stream, int mode) {
        this.stream = stream;
        this.mode = mode;

        int res = stream.Seek(0, 0, null);
        if (ErrorUtil.isError(res)) return res;

        byte[] header = new byte[PALM_HEADER_SIZE];
        int[] bytesRead = new int[1];
        res = stream.Read(header, 0, PALM_HEADER_SIZE, bytesRead);
        if (ErrorUtil.isError(res)) return res;
        if (bytesRead[0] != PALM_HEADER_SIZE) return -2147024886;

        name = new byte[32];
        System.arraycopy(header, 0, name, 0, 32);

        int attr = ((header[32] & 0xFF) << 8) | (header[33] & 0xFF);
        this.flags = attr;

        int version = ((header[34] & 0xFF) << 8) | (header[35] & 0xFF);

        this.type = new byte[4];
        System.arraycopy(header, 60, this.type, 0, 4);

        this.creator = new byte[4];
        System.arraycopy(header, 64, this.creator, 0, 4);

        int uniqueID = ((header[68] & 0xFF) << 24) | ((header[69] & 0xFF) << 16)
                | ((header[70] & 0xFF) << 8) | (header[71] & 0xFF);

        recordCount = ((header[76] & 0xFF) << 8) | (header[77] & 0xFF);

        this.bodySize = 0;

        recordOffsets = new int[recordCount];
        recordAttrs = new int[recordCount];

        byte[] recordList = new byte[recordCount * RECORD_ENTRY_SIZE];
        res = stream.Read(recordList, 0, recordCount * RECORD_ENTRY_SIZE, bytesRead);
        if (ErrorUtil.isError(res)) return res;

        for (int i = 0; i < recordCount; i++) {
            int off = i * RECORD_ENTRY_SIZE;
            recordOffsets[i] = ((recordList[off] & 0xFF) << 24)
                    | ((recordList[off + 1] & 0xFF) << 16)
                    | ((recordList[off + 2] & 0xFF) << 8)
                    | (recordList[off + 3] & 0xFF);
            recordAttrs[i] = recordList[off + 4] & 0xFF;
        }

        return 0;
    }

    @Override
    public int GetInfo(byte[] name, byte[] creator, byte[] type,
                       int[] recordCount, int[] flagsOut, int[] bodySizeOut) {
        if (name != null) System.arraycopy(this.name, 0, name, 0, 32);
        if (creator != null) System.arraycopy(this.creator, 0, creator, 0, 4);
        if (type != null) System.arraycopy(this.type, 0, type, 0, 4);
        if (recordCount != null) recordCount[0] = this.recordCount;
        if (flagsOut != null) flagsOut[0] = this.flags;
        if (bodySizeOut != null) bodySizeOut[0] = this.bodySize;
        return 0;
    }

    @Override
    public int GetRecord(int index, int[] sizeOut, byte[][] dataOut) {
        if (index < 0 || index >= recordCount) return -2147483643;

        DataStream[] recStream = new DataStream[1];
        int res = OpenRecord(index, sizeOut, recStream);
        if (ErrorUtil.isError(res)) return res;

        int size = sizeOut[0];
        byte[] data = new byte[size];
        int[] bytesRead = new int[1];
        res = recStream[0].Read(data, 0, size, bytesRead);
        recStream[0].Close();

        if (ErrorUtil.isError(res)) return res;
        if (bytesRead[0] != size) return -2147024886;

        dataOut[0] = data;
        return 0;
    }

    @Override
    public int OpenRecord(int index, int[] sizeOut, DataStream[] streamOut) {
        if (index < 0 || index >= recordCount) return -2147483643;

        int offset = recordOffsets[index];
        int nextOffset;
        if (index + 1 < recordCount) {
            nextOffset = recordOffsets[index + 1];
        } else {
            int[] bodySizeArr = new int[1];
            stream.GetSize(bodySizeArr);
            nextOffset = bodySizeArr[0];
        }

        int size = nextOffset - offset;

        DataStream recStream = new RecordStream(stream, offset, size);
        sizeOut[0] = size;
        streamOut[0] = recStream;
        return 0;
    }

    @Override
    public int Destroy() {
        if (stream != null) stream.Close();
        stream = null;
        return 0;
    }

    public int getRecordCount() {
        return recordCount;
    }

    private static class RecordStream extends DataStream {
        private final DataStream parent;
        private final int baseOffset;
        private final int size;
        private int pos;

        RecordStream(DataStream parent, int baseOffset, int size) {
            this.parent = parent;
            this.baseOffset = baseOffset;
            this.size = size;
            this.pos = 0;
        }

        @Override
        public int Read(byte[] buf, int offset, int length, int[] bytesRead) {
            if (pos >= size) {
                if (bytesRead != null) bytesRead[0] = 0;
                return 0;
            }
            int avail = size - pos;
            if (length > avail) length = avail;
            int res = parent.readAt(baseOffset + pos, buf, offset, length);
            if (ErrorUtil.isError(res)) return res;
            pos += length;
            if (bytesRead != null) bytesRead[0] = length;
            return 0;
        }

        @Override
        public int Seek(int pos, int mode, int[] resultOut) {
            switch (mode) {
                case 0: this.pos = pos; break;
                case 1: this.pos += pos; break;
                case 2: this.pos = size + pos; break;
                default: return -2147483643;
            }
            if (this.pos < 0) this.pos = 0;
            if (this.pos > size) this.pos = size;
            if (resultOut != null) resultOut[0] = this.pos;
            return 0;
        }

        @Override
        public int GetSize(int[] sizeOut) {
            sizeOut[0] = size;
            return 0;
        }

        @Override
        public int Close() {
            return 0;
        }
    }
}
