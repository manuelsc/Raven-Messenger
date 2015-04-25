/*
 Copyright (c) <2011> <Marc Greim>

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 
 */

package hash;

/**
 *
 * @author Marc Greim
 */
public class BLAKE512 extends java.security.MessageDigest {
    
    public BLAKE512(){
        super("BLAKE-512");
    }
    
    private static final int ROUNDS = 16;
    
    private static final int perm[][] = 
    {
        {0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15},
        {14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3},
        {11,  8 ,12 , 0  ,5 , 2 ,15 ,13, 10 ,14 , 3 , 6 , 7 , 1 , 9 , 4},
        {7,  9 , 3 , 1 ,13 ,12 ,11 ,14 , 2 , 6 , 5 ,10 , 4 , 0 ,15 , 8},
        {9 , 0 , 5 , 7 , 2 , 4 ,10, 15 ,14 , 1 ,11 ,12 , 6 , 8 , 3 ,13},
        {2, 12 , 6 ,10 , 0 ,11 , 8 , 3 , 4, 13 , 7 , 5 ,15, 14,  1 , 9},
        {12,  5 , 1, 15, 14, 13 , 4 ,10 , 0 , 7 , 6 , 3 , 9 , 2 , 8 ,11},
        {13, 11 , 7 ,14, 12,  1 , 3 , 9 , 5 , 0, 15,  4 , 8 , 6 , 2 ,10},
        {6 ,15 ,14  ,9, 11 , 3 , 0 , 8, 12 , 2 ,13 , 7 , 1 , 4, 10 , 5},
        {10 , 2 , 8 , 4 , 7 , 6 , 1 , 5 ,15, 11 , 9 ,14  ,3, 12, 13 , 0},
        {0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15},
        {14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3},
        {11,  8 ,12 , 0  ,5 , 2 ,15 ,13, 10 ,14 , 3 , 6 , 7 , 1 , 9 , 4},
        {7,  9 , 3 , 1 ,13 ,12 ,11 ,14 , 2 , 6 , 5 ,10 , 4 , 0 ,15 , 8},
        {9 , 0 , 5 , 7 , 2 , 4 ,10, 15 ,14 , 1 ,11 ,12 , 6 , 8 , 3 ,13},
        {2, 12 , 6 ,10 , 0 ,11 , 8 , 3 , 4, 13 , 7 , 5 ,15, 14,  1 , 9}
    };
    
    private static final long initialvalue[] = {
        0x6A09E667F3BCC908L,
        0xBB67AE8584CAA73BL,
        0x3C6EF372FE94F82BL,
        0xA54FF53A5F1D36F1L,
        0x510E527FADE682D1L,
        0x9B05688C2B3E6C1FL,
        0x1F83D9ABFB41BD6BL,
        0x5BE0CD19137E2179L
    };
    
    private static final long constant[] = {
        0x243F6A8885A308D3L,
        0x13198A2E03707344L,
        0xA4093822299F31D0L,
        0x082EFA98EC4E6C89L,
        0x452821E638D01377L, 
        0xBE5466CF34E90C6CL,
        0xC0AC29B7C97C50DDL, 
        0x3F84D5B5B5470917L,
        0x9216D5D98979FB1BL, 
        0xD1310BA698DFB5ACL,
        0x2FFD72DBD01ADFB7L, 
        0xB8E1AFED6A267E96L,
        0xBA7C9045F12C7F99L, 
        0x24A19947B3916CF7L,
        0x0801F2E2858EFC16L, 
        0x636920D871574E69L
    };
    
    private static final long nullsalt[] = 
    {
        0,
        0,
        0,
        0
    };
    
    //byte to long conversion
    static long getLong(byte[] b, int off) {
        return  ((b[off + 7] & 0xFFL)      ) +
                ((b[off + 6] & 0xFFL) <<  8) +
                ((b[off + 5] & 0xFFL) << 16) +
                ((b[off + 4] & 0xFFL) << 24) +
                ((b[off + 3] & 0xFFL) << 32) +
                ((b[off + 2] & 0xFFL) << 40) +
                ((b[off + 1] & 0xFFL) << 48) +
                (((long) b[off])      << 56);
    }
    //long to byte conversion
    static void putLong(byte[] b, int off, long val) {
        b[off + 7] = (byte) (val       );
        b[off + 6] = (byte) (val >>>  8);
        b[off + 5] = (byte) (val >>> 16);
        b[off + 4] = (byte) (val >>> 24);
        b[off + 3] = (byte) (val >>> 32);
        b[off + 2] = (byte) (val >>> 40);
        b[off + 1] = (byte) (val >>> 48);
        b[off    ] = (byte) (val >>> 56);
    }

    
    // internal buffer for one block of data (1024 bit)
    private final byte[] buffer = new byte[128];
    private int bufferpos = 0;
    
    // internal buffer for one converted block of data(16 * long)
    private final long[] m = new long[16];
    
    // internal buffer for hash state (16 * long)
    private final long[] v = new long[16];
    
    /** internal buffer for total bit-length of the message
     * IMPORTANT NOTICE:    DUE TO THE HIGH UNLIKELINESS OF A MESSAGE THAT EXCEEDS 2^63 bits ( = 1.153 exabytes)
     *                      ONLY THE LAST 63 BITs OF THE SPECIFIED 128 BITs ARE IN USE 
    */
    private final long[] l = {0,0};
    
    // internal buffer for salt (4 * long)
    private final long[] s = new long[4];
    
    // buffer for hash
    private final long[] h = java.util.Arrays.copyOf(initialvalue, 8);
    
    
    private void Round(int r,long[] v,long[] m){
        // G 0
        
        int i_pc0 = perm[r][0];
        int i_pc1 = perm[r][1];

        v[0] = v[0] + v[4] + ( m[i_pc0] ^ constant[i_pc1]);

        v[12] = java.lang.Long.rotateRight((v[12] ^ v[0]),32);

        v[8] = v[8] + v[12];

        v[4] = java.lang.Long.rotateRight((v[4] ^ v[8]),25);

        v[0] = v[0] + v[4] + ( m[i_pc1] ^ constant[i_pc0]);

        v[12] = java.lang.Long.rotateRight((v[12] ^ v[0]),16);

        v[8] = v[8] + v[12];

        v[4] = java.lang.Long.rotateRight((v[4] ^ v[8]),11);

        
        // G 1

        i_pc0 = perm[r][2];
        i_pc1 = perm[r][3];

        v[1] = v[1] + v[5] + ( m[i_pc0] ^ constant[i_pc1]);

        v[13] = java.lang.Long.rotateRight((v[13] ^ v[1]),32);

        v[9] = v[9] + v[13];

        v[5] = java.lang.Long.rotateRight((v[5] ^ v[9]),25);

        v[1] = v[1] + v[5] + ( m[i_pc1] ^ constant[i_pc0]);

        v[13] = java.lang.Long.rotateRight((v[13] ^ v[1]),16);

        v[9] = v[9] + v[13];

        v[5] = java.lang.Long.rotateRight((v[5] ^ v[9]),11);
            
        
        // G 2
        
        i_pc0 = perm[r][4];
        i_pc1 = perm[r][5];

        v[2] = v[2] + v[6] + ( m[i_pc0] ^ constant[i_pc1]);

        v[14] = java.lang.Long.rotateRight((v[14] ^ v[2]),32);

        v[10] = v[10] + v[14];

        v[6] = java.lang.Long.rotateRight((v[6] ^ v[10]),25);

        v[2] = v[2] + v[6] + ( m[i_pc1] ^ constant[i_pc0]);

        v[14] = java.lang.Long.rotateRight((v[14] ^ v[2]),16);

        v[10] = v[10] + v[14];

        v[6] = java.lang.Long.rotateRight((v[6] ^ v[10]),11);

        
        // G 3

        i_pc0 = perm[r][6];
        i_pc1 = perm[r][7];

        v[3] = v[3] + v[7] + ( m[i_pc0] ^ constant[i_pc1]);

        v[15] = java.lang.Long.rotateRight((v[15] ^ v[3]),32);

        v[11] = v[11] + v[15];

        v[7] = java.lang.Long.rotateRight((v[7] ^ v[11]),25);

        v[3] = v[3] + v[7] + ( m[i_pc1] ^ constant[i_pc0]);

        v[15] = java.lang.Long.rotateRight((v[15] ^ v[3]),16);

        v[11] = v[11] + v[15];

        v[7] = java.lang.Long.rotateRight((v[7] ^ v[11]),11);

            
        // G 4

        i_pc0 = perm[r][8];
        i_pc1 = perm[r][9];

        v[0] = v[0] + v[5] + ( m[i_pc0] ^ constant[i_pc1]);

        v[15] = java.lang.Long.rotateRight((v[15] ^ v[0]),32);

        v[10] = v[10] + v[15];

        v[5] = java.lang.Long.rotateRight((v[5] ^ v[10]),25);

        v[0] = v[0] + v[5] + ( m[i_pc1] ^ constant[i_pc0]);

        v[15] = java.lang.Long.rotateRight((v[15] ^ v[0]),16);

        v[10] = v[10] + v[15];

        v[5] = java.lang.Long.rotateRight((v[5] ^ v[10]),11);
        
            
        // G 5

        i_pc0 = perm[r][10];
        i_pc1 = perm[r][11];

        v[1] = v[1] + v[6] + ( m[i_pc0] ^ constant[i_pc1]);

        v[12] = java.lang.Long.rotateRight((v[12] ^ v[1]),32);

        v[11] = v[11] + v[12];

        v[6] = java.lang.Long.rotateRight((v[6] ^ v[11]),25);

        v[1] = v[1] + v[6] + ( m[i_pc1] ^ constant[i_pc0]);

        v[12] = java.lang.Long.rotateRight((v[12] ^ v[1]),16);

        v[11] = v[11] + v[12];

        v[6] = java.lang.Long.rotateRight((v[6] ^ v[11]),11);

            
        // G 6

        i_pc0 = perm[r][12];
        i_pc1 = perm[r][13];

        v[2] = v[2] + v[7] + ( m[i_pc0] ^ constant[i_pc1]);

        v[13] = java.lang.Long.rotateRight((v[13] ^ v[2]),32);

        v[8] = v[8] + v[13];

        v[7] = java.lang.Long.rotateRight((v[7] ^ v[8]),25);

        v[2] = v[2] + v[7] + ( m[i_pc1] ^ constant[i_pc0]);

        v[13] = java.lang.Long.rotateRight((v[13] ^ v[2]),16);

        v[8] = v[8] + v[13];

        v[7] = java.lang.Long.rotateRight((v[7] ^ v[8]),11);

            
        // G 7
        
        i_pc0 = perm[r][14];
        i_pc1 = perm[r][15];

        v[3] = v[3] + v[4] + ( m[i_pc0] ^ constant[i_pc1]);

        v[14] = java.lang.Long.rotateRight((v[14] ^ v[3]),32);

        v[9] = v[9] + v[14];

        v[4] = java.lang.Long.rotateRight((v[4] ^ v[9]),25);

        v[3] = v[3] + v[4] + ( m[i_pc1] ^ constant[i_pc0]);

        v[14] = java.lang.Long.rotateRight((v[14] ^ v[3]),16);

        v[9] = v[9] + v[14];

        v[4] = java.lang.Long.rotateRight((v[4] ^ v[9]),11);
        
    }
    private void Finalize (long[] v,long[] h,long[] s){
        h[0] = h[0] ^ s[0] ^ v[0] ^ v[8];
        h[1] = h[1] ^ s[1] ^ v[1] ^ v[9];
        h[2] = h[2] ^ s[2] ^ v[2] ^ v[10];
        h[3] = h[3] ^ s[3] ^ v[3] ^ v[11];
        h[4] = h[4] ^ s[0] ^ v[4] ^ v[12];
        h[5] = h[5] ^ s[1] ^ v[5] ^ v[13];
        h[6] = h[6] ^ s[2] ^ v[6] ^ v[14];
        h[7] = h[7] ^ s[3] ^ v[7] ^ v[15];
    }
    private void Initialize (long[] v,long[] h,long[] s,long t0,long t1){
        
        java.lang.System.arraycopy(h, 0, v, 0, 8);
        
        v[8]    =   s[0] ^ constant[0];
        v[9]    =   s[1] ^ constant[1];
        v[10]   =   s[2] ^ constant[2];
        v[11]   =   s[3] ^ constant[3];
        
        v[12]   =   t0 ^ constant[4];
        v[13]   =   t0 ^ constant[5];
        v[14]   =   t1 ^ constant[6];
        v[15]   =   t1 ^ constant[7];
        
    }
    private void Calculate(long[] v,long[] h,long[] m,long[] s,long[] l){
        Initialize(v,h,s,l[0],l[1]); 
        for (int i = 0 ; i<ROUNDS;i++){
            Round(i,v,m);
        }
        Finalize(v,h,s);
    }
    private void hashBlock(){
        //convert byte[] to long[] for performance
        for (int i = 0; i<16;i++){
            m[i] = getLong(buffer,(i*8));
        }
        Calculate(v,h,m,s,l);
    }
    private void hashBlock(byte[] buf,int off){
        //convert byte[] to long[] for performance
        for (int i = 0; i<16;i++){
            m[i] = getLong(buf,(i*8)+off);
        }
        Calculate(v,h,m,s,l);
    }
    
    @Override
    protected void engineUpdate(byte input) {
        buffer[bufferpos] = input;
        bufferpos++;
        if (bufferpos == 128){
            l[0] += 1024;
            hashBlock();
            bufferpos = 0;
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        // fill internal buffer
        if (bufferpos > 0){
            if (len >= 128-bufferpos){
                java.lang.System.arraycopy(input, offset, buffer, bufferpos, 128-bufferpos);
                l[0] += 1024;
                len -= 128-bufferpos;
                offset += 128-bufferpos;
                bufferpos = 0;
                hashBlock();
            } else {
                java.lang.System.arraycopy(input, offset, buffer, bufferpos, len);
                bufferpos += len;
                return;
            }
        }
        // calculate hash from input for higher performance
        while (len >= 128){
                l[0] += 1024;
                hashBlock(input,offset);
                offset += 128;
                len -= 128;
        }
        // buffer remaining data
        if (len>0){
            java.lang.System.arraycopy(input, offset, buffer, 0, len);
            bufferpos = len;
        }
    }

    @Override
    protected byte[] engineDigest() {
        byte[] retur = new byte[64];
        if (retur != null){
            if (bufferpos >111){
                // if the datalength exceeds 111 bytes 2 blocks are needed for padding
                //Block 1
                java.util.Arrays.fill(m, 0);                                                                //reset buffer
                
                for (int i = 0;i<bufferpos ;i++){                                                           //slower conversion implementation but compatible with uneven bytecount
                    m[i>>3] = m[i>>3] + ((buffer[i]&0xFFL)<<((7-(i&7))*8));
                }
                m[bufferpos>>3] = m[bufferpos>>3] | ((Byte.MIN_VALUE)&0xFFL)<<((7-(bufferpos&7))*8);        //append bit 1 as specified
                l[0] = l[0]+(bufferpos *8);                                                                 // increase bit counter
                Calculate(v,h,m,s,l);                                                                       //hash
                //Block 2
                java.util.Arrays.fill(m, 0);                                                                //reset buffer
                m[15] = l[0];                                                                               //append bit length
                m[13] = 1;                                                                                  // set bit 1 before the 128 bit length value
                l[0] = 0;                                                                                   // set length to 0 because this block contains no message data
                Calculate(v,h,m,s,l);                                                                       //hash
            } else {
                // if the datalength doesn't exceed 111 byte then the padding can be done within the current block
                java.util.Arrays.fill(m, 0);                                                                //reset buffer
                for (int i = 0;i<bufferpos ;i++){                                                           //slower conversion implementation but compatible with uneven bytecount
                    m[i>>3] = m[i>>3] + ((buffer[i]&0xFFL)<<((7-(i&7))*8));
                }
                m[bufferpos>>3] = m[bufferpos>>3] | ((Byte.MIN_VALUE)&0xFFL)<<((7-(bufferpos&7))*8);        //append bit 1 as specified
                m[13] = m[13] + 1;                                                                          // set bit 1 before the 128 bit length value
                l[0] = l[0] + (bufferpos*8);                                                                // increase bit counter
                m[15] = l[0];                                                                               //append bit length
                if (bufferpos == 0)                                                                         // set length to 0 if this block contains no message data
                    l[0] = 0;
                Calculate(v,h,m,s,l);                                                                       //hash
            }
//            System.out.println();                                                                           //debug
            for (int i = 0;i<8 ;i++){
                putLong(retur,i*8,h[i]);
//                java.lang.System.out.println(Long.toHexString(h[i]));                                       //debug
            }
        }
        engineReset();                                                                                      // reset engine
        return retur;
    }

    @Override
    protected void engineReset() { 
        java.util.Arrays.fill(l, 0);                                //reset bit-counter
        java.lang.System.arraycopy(initialvalue, 0, h, 0, 8);       //reset hash to initial value
        java.lang.System.arraycopy(nullsalt,0,s,0,4);               //reset salt
        bufferpos = 0;                                              //reset buffer position
    }
    
}
