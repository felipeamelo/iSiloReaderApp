package com.dcco.app.iSilo.engine.util;

public class HuffmanInflator extends iSiloInflator {

    private static int instCounter = 0;
    private int id;

    private static final byte[] CL_ORDER = {
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    };

    private static final int[] LENGTH_BASE = {
        11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227
    };

    private static final int[] DIST_BASE = {
        5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769,
        1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    };

    private byte[] inBuf;
    private int inOff;
    private int wordsLeft;
    private int bitWord;
    private int bitMask;
    private int errCode;
    private int startWords;

    private static final int MAX_LIT_SYMS = 286;
    private static final int MAX_DIST_SYMS = 30;
    private static final int MAX_CL_SYMS = 19;
    private static final int MAX_TREE_LIT = MAX_LIT_SYMS * 2 - 1;
    private static final int MAX_TREE_DIST = MAX_DIST_SYMS * 2 - 1;
    private static final int MAX_TREE_CL = MAX_CL_SYMS * 2 - 1;

    private byte[] litLeft = new byte[MAX_TREE_LIT];
    private byte[] litAux = new byte[MAX_TREE_LIT];
    private byte[] litRight = new byte[MAX_TREE_LIT];

    private byte[] distLeft = new byte[MAX_TREE_DIST];
    private byte[] distRight = new byte[MAX_TREE_DIST];

    public HuffmanInflator() {
        id = ++instCounter;
    }

    public int getBytesConsumed() {
        return (startWords - wordsLeft) * 4;
    }

    private void initRead(byte[] buf, int off, int words) {
        this.inBuf = buf;
        this.inOff = off;
        this.wordsLeft = words;
        this.startWords = words;
        this.bitWord = 0;
        this.bitMask = 0;
        this.errCode = 0;
    }

    private int readBit() {
        if (bitMask == 0) {
            if (wordsLeft == 0) {
                errCode = 1;
                return 0;
            }
            bitWord = readWordBE(inBuf, inOff);
            inOff += 4;
            wordsLeft--;
            bitMask = 1;
        }
        int b = (bitWord & bitMask) != 0 ? 1 : 0;
        bitMask <<= 1;
        return b;
    }

    private int readBits(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            if (readBit() != 0) v |= (1 << i);
        }
        return v;
    }

    private static int readWordBE(byte[] buf, int off) {
        return (buf[off] << 24) | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8) | (buf[off + 3] & 0xFF);
    }

    private static int getValue(int dir, int node, byte[] left, byte[] right, byte[] aux) {
        byte[] arr = dir == 0 ? left : right;
        if (node < 0 || node >= arr.length) return 0;
        int b = arr[node] & 0xFF;
        if ((b & 0x80) == 0 || aux == null) return b;
        if (node >= aux.length) return b & 0x7F;
        if (dir == 0) return (b & 0x7F) | ((aux[node] & 0xF0) << 3);
        return (b & 0x7F) | ((aux[node] & 0x0F) << 7);
    }

    private static void setValue(int dir, int node, int value, byte[] left, byte[] right, byte[] aux) {
        byte[] arr = dir == 0 ? left : right;
        if (node < 0 || node >= arr.length) return;
        arr[node] = (byte) (value & 0x7F);
        if (value >= 128) {
            arr[node] |= 0x80;
            if (aux != null && node < aux.length) {
                if (dir == 0) {
                    aux[node] = (byte) ((aux[node] & 0x0F) | ((value & 0x780) >> 3));
                } else {
                    aux[node] = (byte) ((aux[node] & 0xF0) | ((value & 0x780) >> 7));
                }
            }
        }
    }

    private boolean buildTree(byte[] left, byte[] aux, byte[] right,
                               byte[] codeLens, int count) {
        int maxNodes = left.length;
        for (int i = 0; i < maxNodes; i++) {
            left[i] = 0;
            right[i] = 0;
            if (aux != null) aux[i] = 0;
        }

        int nextNode = 1;
        int code = 0;

        for (int bitLen = 1; bitLen <= 16; bitLen++) {
            for (int sym = 0; sym < count; sym++) {
                int len = codeLens[sym] & 0xFF;
                if (len != bitLen) continue;

                int stored = count + sym + 1;
                int node = 0;

                int rem = bitLen;
                while (rem > 1) {
                    rem--;
                    int bit = (code >> rem) & 1;
                    int idx = getValue(bit, node, left, right, aux);
                    if (idx == 0) {
                        setValue(bit, node, nextNode, left, right, aux);
                        node = nextNode;
                        nextNode++;
                        if (nextNode > maxNodes) return false;
                    } else {
                        node = idx;
                    }
                }

                int lastBit = code & 1;
                setValue(lastBit, node, stored, left, right, aux);
                code++;
            }
            code <<= 1;
        }
        return true;
    }

    private int decodeSym(byte[] left, byte[] right, byte[] aux, int threshold) {
        int node = 0;
        for (int i = 0; i < 16; i++) {
            int bit = readBit();
            int value = getValue(bit, node, left, right, aux);
            if (value > threshold) return (value - threshold) - 1;
            node = value;
        }
        if (errCode != 0) errCode = 2;
        return threshold;
    }

    private boolean readCodeLens(byte[] clLeft, byte[] clRight, byte[] out, int count) {
        int prev = 0;
        int outPos = 0;
        while (outPos < count) {
            int node = 0;
            int bitCount = 0;
            int value;
            while (true) {
                if (bitCount >= 16) {
                    if (errCode != 0) errCode = 2;
                    value = 0;
                    break;
                }
                int bit = readBit();
                value = getValue(bit, node, clLeft, clRight, null);
                if (value > MAX_CL_SYMS) {
                    value = (value - MAX_CL_SYMS) - 1;
                    break;
                }
                bitCount++;
                node = value;
            }
            switch (value) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                    out[outPos] = (byte) value;
                    prev = value;
                    outPos++;
                    break;
                case 16: {
                    int rep = readBits(2) + 3;
                    for (int i = 0; i < rep && outPos < count; i++) {
                        out[outPos++] = (byte) prev;
                    }
                    break;
                }
                case 17: {
                    int rep = readBits(3) + 3;
                    for (int i = 0; i < rep && outPos < count; i++) {
                        out[outPos++] = 0;
                    }
                    prev = 0;
                    break;
                }
                case 18: {
                    int rep = readBits(7) + 11;
                    for (int i = 0; i < rep && outPos < count; i++) {
                        out[outPos++] = 0;
                    }
                    prev = 0;
                    break;
                }
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public int GetTrees(byte[] buf, int offset, int length, int flags) {
        DebugLog.add("HUF_GETTREES", "[%d] off=%d len=%d", id, offset, length);
        DebugLog.hex("HUF_GETTREES_IN", buf, offset, Math.min(length * 4, 32));
        initRead(buf, offset, length);

        int nLit = readBits(5) + 257;
        if (nLit > 286) { errCode = 3; DebugLog.add("HUF_GETTREES", "[%d] nLit=%d > 286 FAIL", id, nLit); return ERR_UNSUPPORTED; }

        int nDist = readBits(5) + 1;
        if (nDist > 30) { errCode = 3; DebugLog.add("HUF_GETTREES", "[%d] nDist=%d > 30 FAIL", id, nDist); return ERR_UNSUPPORTED; }

        int nCL = readBits(4) + 4;
        if (nCL > 19) { errCode = 3; DebugLog.add("HUF_GETTREES", "[%d] nCL=%d > 19 FAIL", id, nCL); return ERR_UNSUPPORTED; }

        DebugLog.add("HUF_GETTREES", "[%d] nLit=%d nDist=%d nCL=%d consumedBytes=%d",
                id, nLit, nDist, nCL, getBytesConsumed());

        byte[] clCodeLen = new byte[MAX_CL_SYMS];
        for (int i = 0; i < nCL; i++) {
            clCodeLen[CL_ORDER[i]] = (byte) readBits(3);
        }
        if (errCode != 0) {
            DebugLog.add("HUF_GETTREES", "[%d] CL codeLens read FAILED errCode=%d", id, errCode);
            return ERR_UNSUPPORTED;
        }

        byte[] clLeft = new byte[MAX_TREE_CL];
        byte[] clRight = new byte[MAX_TREE_CL];
        if (!buildTree(clLeft, null, clRight, clCodeLen, MAX_CL_SYMS)) {
            errCode = 4;
            DebugLog.add("HUF_GETTREES", "[%d] CL buildTree FAILED", id);
            return ERR_UNSUPPORTED;
        }

        byte[] ll = new byte[286];
        if (!readCodeLens(clLeft, clRight, ll, nLit)) {
            errCode = 5;
            DebugLog.add("HUF_GETTREES", "[%d] lit/len codeLens FAILED", id);
            return ERR_UNSUPPORTED;
        }

        if (!buildTree(litLeft, litAux, litRight, ll, 286)) {
            errCode = 5;
            DebugLog.add("HUF_GETTREES", "[%d] lit/len buildTree FAILED", id);
            return ERR_UNSUPPORTED;
        }

        byte[] dist = new byte[30];
        if (!readCodeLens(clLeft, clRight, dist, nDist)) {
            errCode = 6;
            DebugLog.add("HUF_GETTREES", "[%d] dist codeLens FAILED", id);
            return ERR_UNSUPPORTED;
        }

        if (!buildTree(distLeft, null, distRight, dist, 30)) {
            errCode = 6;
            DebugLog.add("HUF_GETTREES", "[%d] dist buildTree FAILED", id);
            return ERR_UNSUPPORTED;
        }

        DebugLog.add("HUF_GETTREES", "[%d] SUCCESS consumedBytes=%d", id, getBytesConsumed());
        return 0;
    }

    @Override
    public int InflateBlock(byte[] src, int srcOff, int srcWords,
                             byte[] dest, int destOff, int destLen,
                             int[] result) {
        DebugLog.add("HUF_INFLATE", "[%d] srcOff=%d srcWords=%d destLen=%d",
                id, srcOff, srcWords, destLen);
        DebugLog.hex("HUF_INFLATE_IN", src, srcOff, Math.min(srcWords * 4, 32));
        initRead(src, srcOff, srcWords);
        result[0] = 0;
        int outPos = 0;
        int symCount = 0;

        while (errCode == 0) {
            int sym = decodeSym(litLeft, litRight, litAux, 286);
            if (sym < 256) {
                if (outPos >= destLen) {
                    DebugLog.add("HUF_INFLATE", "[%d] output full at %d", id, outPos);
                    result[0] = destLen;
                    return ERR_UNSUPPORTED;
                }
                dest[destOff + outPos] = (byte) sym;
                outPos++;
                symCount++;
            } else if (sym == 256) {
                DebugLog.add("HUF_INFLATE", "[%d] END_BLOCK sym=%d outPos=%d syms=%d",
                        id, sym, outPos, symCount);
                result[0] = outPos;
                return 0;
            } else {
                int length;
                if (sym <= 264) {
                    length = sym - 254;
                } else if (sym == 285) {
                    length = 258;
                } else {
                    int extraBits = (sym - 261) >> 2;
                    int idx = sym - 265;
                    if (idx < 0 || idx >= LENGTH_BASE.length) {
                        result[0] = outPos;
                        return ERR_UNSUPPORTED;
                    }
                    length = LENGTH_BASE[idx] + readBits(extraBits);
                }

                if (outPos + length > destLen) {
                    result[0] = outPos;
                    return ERR_UNSUPPORTED;
                }

                int ds = decodeSym(distLeft, distRight, null, 30);
                if (ds > 29) {
                    result[0] = outPos;
                    return ERR_UNSUPPORTED;
                }

                int distance;
                if (ds <= 3) {
                    distance = ds + 1;
                } else {
                    int extraBits = (ds - 2) >> 1;
                    int idx = ds - 4;
                    if (idx < 0 || idx >= DIST_BASE.length) {
                        result[0] = outPos;
                        return ERR_UNSUPPORTED;
                    }
                    distance = DIST_BASE[idx] + readBits(extraBits);
                }

                if (distance > outPos) {
                    DebugLog.add("HUF_INFLATE", "[%d] dist=%d > outPos=%d FAIL", id, distance, outPos);
                    result[0] = outPos;
                    return ERR_UNSUPPORTED;
                }

                symCount++;
                int srcPos = outPos - distance;
                for (int i = 0; i < length; i++) {
                    dest[destOff + outPos] = dest[destOff + srcPos + i];
                    outPos++;
                }
            }
        }

        DebugLog.add("HUF_INFLATE", "[%d] FINISHED errCode=%d outPos=%d", id, errCode, outPos);
        result[0] = outPos;
        return 0;
    }
}
