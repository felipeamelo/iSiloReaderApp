package com.dcco.app.iSilo.engine.data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileDataStream extends DataStream {

    private RandomAccessFile file;
    private String path;
    private int openMode;
    private int fileSize;

    public FileDataStream() {
    }

    public int open(String path, int mode) {
        this.path = path;
        this.openMode = mode;
        try {
            String javaMode;
            switch (mode) {
                case 0: javaMode = "r"; break;
                case 1: javaMode = "rw"; break;
                case 2: javaMode = "rw"; break;
                default: return -2147483643;
            }
            File f = new File(path);
            if (mode == 0 && !f.exists()) return -2146959348;
            this.file = new RandomAccessFile(f, javaMode);
            this.fileSize = (int) file.length();
            return 0;
        } catch (IOException e) {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int Close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
            }
            file = null;
        }
        return 0;
    }

    @Override
    public int GetSize(int[] sizeOut) {
        if (file == null) return -2147483643;
        sizeOut[0] = fileSize;
        return 0;
    }

    @Override
    public int Read(byte[] buf, int offset, int length, int[] bytesRead) {
        if (file == null) return -2147483643;
        try {
            int total = 0;
            while (total < length) {
                int count = file.read(buf, offset + total, length - total);
                if (count < 0) break;
                total += count;
            }
            if (bytesRead != null) bytesRead[0] = total;
            if (total != length) return -2147024886;
            return 0;
        } catch (IOException e) {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int Seek(int pos, int mode, int[] resultOut) {
        if (file == null) return -2147483643;
        try {
            switch (mode) {
                case 0:
                    file.seek(pos);
                    break;
                case 1:
                    file.seek(file.getFilePointer() + pos);
                    break;
                case 2:
                    file.seek(file.length() + pos);
                    break;
                default:
                    return -2147483643;
            }
            if (resultOut != null) resultOut[0] = (int) file.getFilePointer();
            return 0;
        } catch (IOException e) {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int Write(byte[] buf, int offset, int length, int[] bytesWritten) {
        if (file == null) return -2147483643;
        try {
            file.write(buf, offset, length);
            if (bytesWritten != null) bytesWritten[0] = length;
            if (file.getFilePointer() > fileSize) {
                fileSize = (int) file.getFilePointer();
            }
            return 0;
        } catch (IOException e) {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int SetSize(int size) {
        if (file == null) return -2147483643;
        try {
            file.setLength(size);
            fileSize = size;
            return 0;
        } catch (IOException e) {
            return Integer.MIN_VALUE;
        }
    }
}
