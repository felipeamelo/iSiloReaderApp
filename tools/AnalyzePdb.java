import java.io.*;

public class AnalyzePdb {
    public static void main(String[] args) throws Exception {
        String path = "C:\\PROJETOS\\iSiloDocs\\Missal v7-1.pdb";
        RandomAccessFile f = new RandomAccessFile(path, "r");
        byte[] hdr = new byte[78]; f.readFully(hdr);
        int rc = (hdr[76]<<8)|(hdr[76+1]&0xFF);
        int[] ro = new int[rc];
        byte[] e = new byte[rc*8]; f.readFully(e);
        for (int i=0;i<rc;i++) ro[i]=(e[i*8]<<24)|((e[i*8+1]&0xFF)<<16)|((e[i*8+2]&0xFF)<<8)|(e[i*8+3]&0xFF);

        byte[] r0 = read(f, ro[0], ro[1]-ro[0]);
        int hs = (r0[0]<<8)|(r0[1]&0xFF);
        int p = hs+2+2; for(int i=0;i<4;i++) p+=2;
        p+=2; int[] k=new int[18],b=new int[18];
        for(int i=0;i<18;i++) {k[i]=u16(r0,p);p+=2;}
        for(int i=0;i<18;i++) {b[i]=u16(r0,p);p+=2;}
        System.out.println("RECORDS="+rc+" k[0]="+k[0]+" B[0]="+b[0]+" k[1]="+k[1]+" B[1]="+b[1]+" k[2]="+k[2]+" B[2]="+b[2]);

        // Analyze records in TOC range (k[1] to k[1]+b[1]-1)
        System.out.println("\nTOC RANGE: records "+k[1]+" to "+(k[1]+b[1]-1));
        for (int ri=k[1]; ri<rc && ri<k[1]+10; ri++) {
            int sz = ro[ri+1]-ro[ri];
            byte[] d = read(f, ro[ri], Math.min(sz,8));
            System.out.println("  rec["+ri+"] sub="+(d[1]&0xFF)+" size="+sz);
        }

        // Analyze record 149 in detail
        System.out.println("\nRECORD 149 DETAIL:");
        int sz149 = ro[150]-ro[149];
        byte[] r149 = read(f, ro[149], Math.min(sz149,512));
        System.out.println("size="+sz149+" type="+(r149[0]&0xFF)+" sub="+(r149[1]&0xFF));
        for (int i=0;i<Math.min(sz149,128);i++) {
            System.out.print(String.format("%02X ",r149[i]&0xFF));
            if ((i+1)%16==0) System.out.println();
        }
        System.out.println();

        // Analyze SUB=1 records format
        System.out.println("\nSUB=1 RECORDS FORMAT:");
        for (int ri=260; ri<=263 && ri<rc; ri++) {
            int sz = ro[ri+1]-ro[ri];
            if (sz<8) continue;
            byte[] d = read(f, ro[ri], Math.min(sz,64));
            int startOff = u16(d,2);
            System.out.println("rec["+ri+"] startOff="+startOff+" size="+sz);
        }

        f.close();
    }

    static byte[] read(RandomAccessFile f, int off, int sz) throws Exception {
        byte[] d = new byte[sz]; f.seek(off); f.readFully(d); return d;
    }
    static int u16(byte[] d, int o) { return ((d[o]&0xFF)<<8)|(d[o+1]&0xFF); }
}
