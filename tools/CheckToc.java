import java.io.*;
public class CheckToc {
    public static void main(String[] a) throws Exception {
        String path = "C:\\PROJETOS\\iSiloDocs\\Missal v7-1.pdb";
        RandomAccessFile f = new RandomAccessFile(path,"r");
        byte[] hdr = new byte[78]; f.readFully(hdr);
        int rc = ((hdr[76]&0xFF)<<8)|(hdr[77]&0xFF);
        int[] ro = new int[rc];
        byte[] et = new byte[rc*8]; f.readFully(et);
        for(int i=0;i<rc;i++) ro[i]=(et[i*8]<<24)|((et[i*8+1]&0xFF)<<16)|((et[i*8+2]&0xFF)<<8)|(et[i*8+3]&0xFF);

        // Check records 145-152
        for (int ri=145; ri<=152 && ri<rc; ri++) {
            int sz = ro[ri+1]-ro[ri];
            if (sz<2) continue;
            byte[] d = new byte[Math.min(sz,256)]; f.seek(ro[ri]); f.readFully(d);
            int t = d[0]&0xFF, s = d[1]&0xFF;
            System.out.println("rec["+ri+"] type="+t+" sub="+s+" size="+sz);
            //print first bytes as hex
            for (int i=0;i<Math.min(32,sz);i++) System.out.print(String.format("%02X ",d[i]&0xFF));
            System.out.println();
        }

        // Dump record 149 in full
        System.out.println("\nRECORD 149 FULL ("+ (ro[150]-ro[149]) + " bytes):");
        byte[] r149 = new byte[ro[150]-ro[149]];
        f.seek(ro[149]); f.readFully(r149);
        for (int i=0;i<r149.length;i++) {
            System.out.print(String.format("%02X ",r149[i]&0xFF));
            if ((i+1)%16==0) System.out.println();
        }
        System.out.println();

        // Also check records 260-268
        System.out.println("\nRECORD 260 (sub=1) FULL:");
        byte[] r260 = new byte[ro[261]-ro[260]];
        f.seek(ro[260]); f.readFully(r260);
        for (int i=0;i<r260.length;i++) {
            System.out.print(String.format("%02X ",r260[i]&0xFF));
            if ((i+1)%16==0) System.out.println();
        }

        f.close();
    }
}
