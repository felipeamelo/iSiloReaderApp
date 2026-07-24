package com.dcco.app.iSilo.engine.util;

import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class iSiloDecompress {

    public static final int MAX_BLOCK_SIZE = 0x1000;

    public static int calcBlockSize(byte[] data, int offset, int length, int[] sizeOut) {
        int size = 0;
        int pos = offset;
        int remaining = length;

        while (remaining > 0) {
            int token = data[pos] & 0xFF;
            pos++;
            remaining--;

            if ((token & 0x80) != 0) {
                if ((token & 0x40) != 0) {
                    size += 2;
                } else {
                    if (remaining <= 0) break;
                    int next = data[pos] & 0xFF;
                    pos++;
                    remaining--;
                    size += 3 + (next & 7);
                }
            } else {
                if (token >= 9) {
                    size += 1;
                } else {
                    int count = token;
                    if (remaining < count) count = remaining;
                    size += count;
                    pos += count;
                    remaining -= count;
                }
            }
        }
        sizeOut[0] = size;
        return 0;
    }

    public static int decompressBlock(byte[] input, int inOffset, int inLength,
                                       byte[] output, int outOffset, int maxOutSize,
                                       int[] actualOutSize) {
        int ip = inOffset;
        int remaining = inLength;
        int op = 0;

        while (remaining > 0) {
            int token = input[ip] & 0xFF;
            ip++;
            remaining--;

            if ((token & 0x80) != 0) {
                if ((token & 0x40) != 0) {
                    if (op + 2 > maxOutSize) break;
                    output[outOffset + op] = 0x20;
                    op++;
                    output[outOffset + op] = (byte) (token & 0x7F);
                    op++;
                } else {
                    if (remaining <= 0) break;
                    int next = input[ip] & 0xFF;
                    ip++;
                    remaining--;

                    int offset = (32 * (token & 0x3F)) | (next >> 3);
                    int length = (next & 7) + 3;

                    if (offset > op) offset = op;

                    int copyLen = length;
                    if (copyLen > maxOutSize - op) copyLen = maxOutSize - op;

                    for (int i = 0; i < copyLen; i++) {
                        output[outOffset + op] = output[outOffset + op - offset];
                        op++;
                    }
                }
            } else {
                if (token >= 9) {
                    if (op >= maxOutSize) break;
                    output[outOffset + op] = (byte) token;
                    op++;
                } else if (token >= 1) {
                    int count = token;
                    if (remaining < count) count = remaining;
                    if (op + count > maxOutSize) count = maxOutSize - op;
                    System.arraycopy(input, ip, output, outOffset + op, count);
                    ip += count;
                    remaining -= count;
                    op += count;
                } else {
                    if (op >= maxOutSize) break;
                    output[outOffset + op] = 0;
                    op++;
                }
            }
        }

        actualOutSize[0] = op;
        return 0;
    }
}
