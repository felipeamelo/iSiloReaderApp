import java.io.*;
public class AnalyzePdb {
    public static void main(String[] a) throws Exception {
        String path = "C:\\PROJETOS\\iSiloDocs\\Missal v7-1.pdb";
        RandomAccessFile f = new RandomAccessFile(path,"r");
        byte[] hdr = new byte[78]; f.readFully(hdr);
        int rc = ((hdr[76]&0xFF)<<8)|(hdr[77]&0xFF);
        int[] ro = new int[rc];
        byte[] et = new byte[rc*8]; f.readFully(et);
        for(int i=0;i<rc;i++) ro[i]=g32(et,i*8);

        byte[] r0 = new byte[ro[1]-ro[0]];
        f.seek(ro[0]); f.readFully(r0);
        System.out.println("Record 0 size="+r0.length);
        for(int i=128;i<r0.length;i++) {
            System.out.print(String.format("%02X ",r0[i]&0xFF));
            if((i-127)%16==0) System.out.println();
        }
        System.out.println();

        // Parse at offset 128
        int gc = u16(r0,128);
        System.out.println("groupCount="+gc+" at 128");
        System.out.println("A[0]="+u16(r0,130)+" A[1]="+u16(r0,132)+" A[2]="+u16(r0,134)+" A[3]="+u16(r0,136));
        int kc = u16(r0,138);
        System.out.println("kCount="+kc+" at 138");
        System.out.print("k[]: ");
        for(int i=0;i<kc&&i<18;i++) System.out.print(u16(r0,140+i*2)+" ");
        System.out.println();
        System.out.print("B[]: ");
        for(int i=0;i<kc&&i<18;i++) System.out.print(u16(r0,140+kc*2+i*2)+" ");
        System.out.println();

        f.close();
    }
    static int u16(byte[] d, int o) { return ((d[o]&0xFF)<<8)|(d[o+1]&0xFF); }
    static int g32(byte[] d, int o) { return (d[o]<<24)|((d[o+1]&0xFF)<<16)|((d[o+2]&0xFF)<<8)|(d[o+3]&0xFF); }
}
