package com.dcco.app.iSilo.engine.util;

public class StringCompare {

    private static final short[] CHAR_CLASS = {
        32, 32, 32, 32, 32, 32, 32, 32, 32, 304, 48, 48, 48, 48, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        272, 576, 320, 576, 576, 576, 576, 576, 320, 320, 576, 576, 320, 576, 576, 320,
        524, 524, 524, 524, 524, 524, 524, 524, 524, 524, 320, 320, 320, 320, 320, 320,
        320, 521, 521, 521, 521, 521, 521, 513, 513, 513, 513, 513, 513, 513, 513, 513,
        513, 513, 513, 513, 513, 513, 513, 513, 513, 513, 513, 320, 320, 320, 576, 576,
        576, 522, 522, 522, 522, 522, 522, 514, 514, 514, 514, 514, 514, 514, 514, 514,
        514, 514, 514, 514, 514, 514, 514, 514, 514, 514, 514, 320, 576, 320, 576, 32
    };

    private static final byte[] REG_CODE_PREFIX = {52, 50, 57, 52, 57, 54, 55, 50, 57, 53};

    public static int compareCaseInsensitive(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        int min = lenA < lenB ? lenA : lenB;
        for (int i = 0; i < min; i++) {
            char ca = Character.toLowerCase(a.charAt(i));
            char cb = Character.toLowerCase(b.charAt(i));
            if (ca < cb) return 2;
            if (ca > cb) return 3;
        }
        if (lenA == lenB) return 0;
        return lenA < lenB ? 2 : 3;
    }

    public static int compareBytesCaseInsensitive(byte[] arr1, int off1, int len1, byte[] arr2, int off2, int len2) {
        int min = len1 < len2 ? len1 : len2;
        int i1 = off1;
        int i2 = off2;
        while (true) {
            int remaining = min - 1;
            if (min == 0) {
                if (len1 == len2) return 0;
                return len1 < len2 ? 2 : 3;
            }
            byte b1 = arr1[i1];
            if (isLowerCase(b1)) b1 = (byte) (b1 + 32);
            byte b2 = arr2[i2];
            if (isLowerCase(b2)) b2 = (byte) (b2 + 32);
            if (b1 < b2) return 2;
            if (b1 > b2) return 3;
            min = remaining;
            i1++;
            i2++;
        }
    }

    public static int parseNumber(byte[] buf, int offset, int length, int[] outValue) {
        int pos = offset;
        int remaining = length;
        while (remaining != 0) {
            if ((CHAR_CLASS[buf[pos] & 0xFF] & 16) == 0) break;
            pos++;
            remaining--;
        }
        int value = 0;
        int i = remaining;
        int p = pos;
        while (i != 0) {
            int b = buf[p] & 0xFF;
            if ((CHAR_CLASS[b] & 4) == 0) break;
            p++;
            i--;
            value = (value * 10) + (b - 48);
        }
        outValue[0] = value;
        int consumed = p - pos;
        if (consumed <= 10 && (consumed != 10 || ByteArrayUtils.memCmp(REG_CODE_PREFIX, 10, buf, pos, 10) != 2)) {
            return 0;
        }
        outValue[0] = -1;
        return -2146762750;
    }

    public static int copyString(byte[] dst, int dstOff, byte[] src, int srcOff) {
        while (true) {
            byte b = src[srcOff];
            dst[dstOff] = b;
            if (b == 0) return 0;
            srcOff++;
            dstOff++;
        }
    }

    public static int findNullTerminator(byte[] buf, int[] outPos) {
        int i = 0;
        while (i < buf.length) {
            if (buf[i] == 0) break;
            i++;
        }
        if (i >= buf.length) outPos[0] = 0;
        else outPos[0] = i;
        return 0;
    }

    public static int binarySearch(byte[][] sortedKeys, int keyCount, byte[] key, int keyOff, int keyLen, int[] outIndex) {
        if (keyCount == 0) return 1;
        int lo = 0;
        int hi = keyCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int cmp = compareBytesCaseInsensitive(key, keyOff, keyLen, sortedKeys[mid], 1, sortedKeys[mid][0]);
            if (cmp == 2) {
                if (mid == 0) return 1;
                hi = mid - 1;
            } else if (cmp == 3) {
                lo = mid + 1;
            } else {
                if (outIndex != null) outIndex[0] = mid;
                return 0;
            }
        }
        return 1;
    }

    public static int compareBytes(byte[] arr1, int off1, byte[] arr2, int off2) {
        while (true) {
            byte b1 = arr1[off1];
            byte b2 = arr2[off2];
            if (b1 == 0) return b2 == 0 ? 0 : 2;
            if (b2 == 0) return 3;
            if (b1 != b2) return (b1 & 0xFF) < (b2 & 0xFF) ? 2 : 3;
            off1++;
            off2++;
        }
    }

    private static boolean isLowerCase(byte b) {
        return (CHAR_CLASS[b & 0xFF] & 1) != 0;
    }
}
