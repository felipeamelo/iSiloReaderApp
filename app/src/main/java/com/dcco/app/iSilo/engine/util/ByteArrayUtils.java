package com.dcco.app.iSilo.engine.util;

import android.os.Environment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

public class ByteArrayUtils {

    private static String sCachedDeviceId;
    private static String sSdcardPath;

    public static int allocArray(int size, byte[][] outArr) {
        try {
            outArr[0] = new byte[size];
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public static int arrayCopy(Object dst, int dstPos, Object src, int srcPos, int length) {
        System.arraycopy(src, srcPos, dst, dstPos, length);
        return 0;
    }

    public static int readInt16BE(byte[] buf, int offset) {
        return ((buf[offset] << 8) & 0xFF00) | (buf[offset + 1] & 0xFF);
    }

    public static int readInt16BE(byte[] buf, int offset, int index) {
        int pos = (index * 2) + offset;
        return (buf[pos + 1] & 0xFF) | ((buf[pos] << 8) & 0xFF00);
    }

    public static int readInt32BE(byte[] buf, int offset) {
        return (buf[offset] << 24) | ((buf[offset + 1] << 16) & 0xFF0000)
                | ((buf[offset + 2] << 8) & 0xFF00) | (buf[offset + 3] & 0xFF);
    }

    public static int readInt32BE(byte[] buf, int offset, int index) {
        int pos = (index * 4) + offset;
        return (buf[pos + 3] & 0xFF) | (buf[pos] << 24)
                | ((buf[pos + 1] << 16) & 0xFF0000) | ((buf[pos + 2] << 8) & 0xFF00);
    }

    public static void writeInt16BE(int value, byte[] buf, int offset) {
        buf[offset] = (byte) (value >> 8);
        buf[offset + 1] = (byte) value;
    }

    public static void writeInt32BE(int value, byte[] buf, int offset) {
        buf[offset] = (byte) (value >> 24);
        buf[offset + 1] = (byte) (value >> 16);
        buf[offset + 2] = (byte) (value >> 8);
        buf[offset + 3] = (byte) value;
    }

    public static int memSet(byte[] buf, byte val, int start, int length) {
        if (start != 0 || length != buf.length) {
            length += start;
        }
        while (start < length) {
            buf[start] = 0;
            start++;
        }
        return 0;
    }

    public static int memCmp(byte[] buf1, int len1, byte[] buf2, int offset2, int len2) {
        int count = (len1 < len2 ? len1 : len2) + 0;
        int i = 0;
        int j = offset2;
        while (i < count) {
            byte b1 = buf1[i];
            byte b2 = buf2[j];
            if (b1 < b2) return 2;
            if (b1 > b2) return 3;
            i++;
            j++;
        }
        if (len1 == len2) return 0;
        return len1 < len2 ? 2 : 3;
    }

    public static String bytesToString(byte[] buf, int offset, int length) {
        StringBuilder sb = new StringBuilder(length);
        while (length > 0) {
            sb.append((char) (buf[offset] & 0xFF));
            length--;
            offset++;
        }
        return sb.toString();
    }

    public static String resolveSdcardPath(String path) {
        if (!path.startsWith("/sdcard/")) return path;
        if (sSdcardPath == null) {
            File extDir = Environment.getExternalStorageDirectory();
            if (extDir == null) return path;
            String p = extDir.getPath();
            sSdcardPath = p;
            if (p == null) return path;
        }
        return sSdcardPath + path.substring(7);
    }

    public static int checkFile(String path, FileInfo info) {
        try {
            File file = new File(resolveSdcardPath(path));
            info.exists = false;
            info.isDirectory = false;
            info.lastModified = 0L;
            info.size = 0L;
            try {
                info.exists = file.exists();
                if (info.exists) {
                    info.isDirectory = file.isDirectory();
                    if (!info.isDirectory) {
                        info.lastModified = file.lastModified();
                        info.size = file.length();
                    }
                }
                return 0;
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int getFileInfo(String path, FileTimestamp ts, byte[] buf, int flags, int[] sizeOut) {
        try {
            File file = new File(resolveSdcardPath(path));
            if (!file.exists()) return -2146959348;
            if (ts != null) {
                try {
                    long lastMod = file.lastModified();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date(lastMod));
                    ts.year = (short) cal.get(Calendar.YEAR);
                    ts.month = (byte) (cal.get(Calendar.MONTH) + 1);
                    ts.day = (byte) cal.get(Calendar.DAY_OF_MONTH);
                    ts.hour = (byte) cal.get(Calendar.HOUR_OF_DAY);
                    ts.minute = (byte) cal.get(Calendar.MINUTE);
                    ts.second = (byte) cal.get(Calendar.SECOND);
                    ts.centisecond = (byte) (cal.get(Calendar.MILLISECOND) / 10);
                } catch (Throwable t) {
                    return errorCode(t);
                }
            }
            if (sizeOut == null) return 0;
            try {
                long len = file.length();
                if (len > Integer.MAX_VALUE) {
                    sizeOut[0] = Integer.MAX_VALUE;
                    return 0;
                }
                sizeOut[0] = (int) len;
                return 0;
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int moveFile(String oldPath, String newPath, boolean overwrite) {
        try {
            File src = new File(resolveSdcardPath(oldPath));
            File dst = new File(resolveSdcardPath(newPath));
            if (!src.exists()) return -2146959348;
            if (dst.exists()) {
                if (!overwrite) return -2146959352;
                dst.delete();
            }
            if (src.renameTo(dst)) return 0;
            try {
                java.io.FileInputStream in = new java.io.FileInputStream(src);
                try {
                    java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
                    try {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
                        out.flush();
                    } finally { out.close(); }
                } finally { in.close(); }
                src.delete();
                return 0;
            } catch (java.io.IOException e) {
                return Integer.MIN_VALUE;
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int renameFile(String oldPath, String newName, boolean overwrite) {
        try {
            resolveSdcardPath(oldPath);
            File oldFile = new File(oldPath);
            int lastSlash = oldPath.lastIndexOf('/');
            if (lastSlash < 0) return -2147483631;
            File newFile = new File(resolveSdcardPath(oldPath.substring(0, lastSlash + 1) + newName));
            if (newName.charAt(0) == '/') {
                int res = moveFile(oldPath, newName, overwrite);
                return !isError(res) ? deleteFile(oldPath) : res;
            }
            try {
                if (oldFile.renameTo(newFile)) return 0;
                try {
                    if (!newFile.exists()) return Integer.MIN_VALUE;
                    if (!overwrite) return -2146959352;
                    if (newFile.isDirectory()) return -2146959347;
                    if (newFile.delete()) {
                        return oldFile.renameTo(newFile) ? 0 : Integer.MIN_VALUE;
                    }
                    return Integer.MIN_VALUE;
                } catch (Throwable t) {
                    return errorCode(t);
                }
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int getFileSize(String path, int[] sizeOut) {
        try {
            File file = new File(resolveSdcardPath(path));
            try {
                if (!file.exists()) return -2146959348;
                try {
                    if (file.isDirectory()) return -2146959345;
                    try {
                        long len = file.length();
                        if (len > Integer.MAX_VALUE) {
                            sizeOut[0] = Integer.MAX_VALUE;
                            return 0;
                        }
                        sizeOut[0] = (int) len;
                        return 0;
                    } catch (Throwable t) {
                        return errorCode(t);
                    }
                } catch (Throwable t) {
                    return errorCode(t);
                }
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int listDirectory(String path, Enumeration[] outEnum) {
        if (path == null || path.length() == 0) path = "/";
        try {
            File file = new File(resolveSdcardPath(path));
            try {
                if (!file.exists()) return -2146959348;
                try {
                    if (!file.isDirectory()) return -2146959348;
                    String[] list = file.list();
                    if (list == null) return -2146959348;
                    outEnum[0] = new DirectoryListing(list);
                    return 0;
                } catch (Throwable t) {
                    return errorCode(t);
                }
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int errorCode(Throwable t) {
        if (t instanceof Error) {
            if (t instanceof VirtualMachineError && t instanceof OutOfMemoryError) {
                return -2147483646;
            }
        } else if (t instanceof Exception && t instanceof IOException && t instanceof FileNotFoundException) {
            return -2146959348;
        }
        return Integer.MIN_VALUE;
    }

    public static int deleteFile(String path) {
        boolean exists;
        boolean isDir;
        try {
            File file = new File(resolveSdcardPath(path));
            try {
                exists = file.exists();
            } catch (Throwable t) {
                exists = false;
            }
            if (!exists) return Integer.MIN_VALUE;
            try {
                isDir = file.isDirectory();
            } catch (Throwable t) {
                isDir = false;
            }
            if (isDir) return Integer.MIN_VALUE;
            try {
                return file.delete() ? 0 : Integer.MIN_VALUE;
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static int makeDirectory(String path) {
        try {
            File file = new File(resolveSdcardPath(path));
            try {
                if (!file.exists()) {
                    return file.mkdirs() ? 0 : -2146959350;
                }
                try {
                    return file.isDirectory() ? 0 : Integer.MIN_VALUE;
                } catch (Throwable t) {
                    return errorCode(t);
                }
            } catch (Throwable t) {
                return errorCode(t);
            }
        } catch (Throwable t) {
            return errorCode(t);
        }
    }

    public static String getDeviceId() {
        String id = readDeviceId();
        return (id == null || id.length() == 0) ? "" : id;
    }

    private static String readDeviceId() {
        return sCachedDeviceId;
    }

    public static class FileInfo {
        public boolean exists;
        public boolean isDirectory;
        public long lastModified;
        public long size;
    }

    public static class FileTimestamp {
        public short year;
        public byte month;
        public byte day;
        public byte hour;
        public byte minute;
        public byte second;
        public byte centisecond;
    }

    public static boolean isError(int code) {
        return code < 0;
    }
}
