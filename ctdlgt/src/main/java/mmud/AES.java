package mmud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class AES {

    private int round;

    private int n;
    private int nRow;
    private int nCol;

    private int nRound;

    private int[] key;
    private int[] w;

    private byte[] iv;

    private int[][][] state;

    private static final int[] sBox = new int[] {
            //0     1    2      3     4    5     6     7      8    9     A      B    C     D     E     F
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16 };

    private static final int[] rsBox = new int[] {
            0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
            0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
            0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
            0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
            0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
            0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
            0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
            0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
            0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
            0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
            0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
            0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
            0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
            0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
            0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
            0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d };

    private static final int[] rCon = new int[] {
            0x8d, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d, 0x9a,
            0x2f, 0x5e, 0xbc, 0x63, 0xc6, 0x97, 0x35, 0x6a, 0xd4, 0xb3, 0x7d, 0xfa, 0xef, 0xc5, 0x91, 0x39,
            0x72, 0xe4, 0xd3, 0xbd, 0x61, 0xc2, 0x9f, 0x25, 0x4a, 0x94, 0x33, 0x66, 0xcc, 0x83, 0x1d, 0x3a,
            0x74, 0xe8, 0xcb, 0x8d, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8,
            0xab, 0x4d, 0x9a, 0x2f, 0x5e, 0xbc, 0x63, 0xc6, 0x97, 0x35, 0x6a, 0xd4, 0xb3, 0x7d, 0xfa, 0xef,
            0xc5, 0x91, 0x39, 0x72, 0xe4, 0xd3, 0xbd, 0x61, 0xc2, 0x9f, 0x25, 0x4a, 0x94, 0x33, 0x66, 0xcc,
            0x83, 0x1d, 0x3a, 0x74, 0xe8, 0xcb, 0x8d, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b,
            0x36, 0x6c, 0xd8, 0xab, 0x4d, 0x9a, 0x2f, 0x5e, 0xbc, 0x63, 0xc6, 0x97, 0x35, 0x6a, 0xd4, 0xb3,
            0x7d, 0xfa, 0xef, 0xc5, 0x91, 0x39, 0x72, 0xe4, 0xd3, 0xbd, 0x61, 0xc2, 0x9f, 0x25, 0x4a, 0x94,
            0x33, 0x66, 0xcc, 0x83, 0x1d, 0x3a, 0x74, 0xe8, 0xcb, 0x8d, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20,
            0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d, 0x9a, 0x2f, 0x5e, 0xbc, 0x63, 0xc6, 0x97, 0x35,
            0x6a, 0xd4, 0xb3, 0x7d, 0xfa, 0xef, 0xc5, 0x91, 0x39, 0x72, 0xe4, 0xd3, 0xbd, 0x61, 0xc2, 0x9f,
            0x25, 0x4a, 0x94, 0x33, 0x66, 0xcc, 0x83, 0x1d, 0x3a, 0x74, 0xe8, 0xcb, 0x8d, 0x01, 0x02, 0x04,
            0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d, 0x9a, 0x2f, 0x5e, 0xbc, 0x63,
            0xc6, 0x97, 0x35, 0x6a, 0xd4, 0xb3, 0x7d, 0xfa, 0xef, 0xc5, 0x91, 0x39, 0x72, 0xe4, 0xd3, 0xbd,
            0x61, 0xc2, 0x9f, 0x25, 0x4a, 0x94, 0x33, 0x66, 0xcc, 0x83, 0x1d, 0x3a, 0x74, 0xe8, 0xcb, 0x8d };


    private String toHex(int v){
        return  Integer.toHexString(v);
    }

    private void init(byte[] key, byte[] iv){
        this.iv = iv;
        this.key = new int[key.length];

        for (int i = 0; i < key.length; i++) {
            this.key[i] = key[i] &0xFF;
        }

        // 128 bits data
        this.nRow= 4;

        switch (key.length) {
            // 128 bit key
            case 16:
                this.nRound = 10;
                this.nCol = 4;
                break;
            // 192 bit key
            case 24:
                this.nRound = 12;
                this.nCol = 6;
                break;
            // 256 bit key
            case 32:
                this.nRound = 14;
                this.nCol = 8;
                break;
            default:
                throw new IllegalArgumentException("It only supports 128, 192 and 256 bit keys!");
        }
        this.state = new int[2][4][nRow];
        w = expandKey();
    }
    public AES(byte[] key){
        init(key, null);
    }
    public AES(byte[] key, byte[] iv){
        init(key, iv);
    }

    private void printKey(int round){
        int begin = round*4;
        for (int i = begin; i < begin+4; i++) {
            System.out.printf("%s ", toHex(w[i]));
        }
        System.out.println();
    }

    private int[] expandKey() {
        // 43, 51, 59
        w = new int[nRow*(nRound+1)];
        int temp, i = 0;
        // initial w[0], w[1], w[2], w[3] from key[]
        while (i < nCol) {
            w[i] = 0x00000000;
            w[i] |= key[4 * i] << 24;
            w[i] |= key[4 * i + 1] << 16;
            w[i] |= key[4 * i + 2] << 8;
            w[i] |= key[4 * i + 3];
            i++;
        }
        i = nCol;
        while (i < nRow * (nRound + 1)) {
            temp = w[i - 1];
            if (i % nCol == 0) {
                // apply an XOR with a constant round rCon.
                temp = subWord(rotWord(temp)) ^ (rCon[i / nCol] << 24);
            } else if (nCol > 6 && (i % nCol == 4)) {
                temp = subWord(temp);
            } else {
            }
            w[i] = w[i - nCol] ^ temp;
            i++;
        }
//        printKey(10);
        return w;
    }

    // substitute sBox
    private static int subWord(int word) {
        int subWord = 0;
        for (int i = 24; i >= 0; i -= 8) {
            int in = word << i >>> 24;
            subWord |= sBox[in] << (24 - i);
        }
        return subWord;
    }
    private static int rotWord(int word) {
        return (word << 8) | ((word & 0xFF000000) >>> 24);
    }

    private void printState(int[][] state){
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.printf("%s ", toHex(state[i][j]));
            }
            System.out.println();
        }
    }

    private void addRoundKey(int[][] out, int round) {
        for (int c = 0; c < nRow; c++) {
            for (int r = 0; r < 4; r++) {
                // XOR
                out[r][c] = out[r][c] ^ ((w[round * nRow + c] << (r * 8)) >>> 24);
            }
        }
    }
    // Substitute Bytes tra sbox
    private void subBytes(int[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < nRow; j++) {
                state[i][j] = subWord(state[i][j]) & 0xFF;
            }
        }
    }
    private void shiftRows(int[][] state) {
        int temp1, temp2, temp3, i;
        //row 0, ko dich

        // row 1, left shift 1 byte
        temp1 = state[1][0];
        for (i = 0; i < nRow - 1; i++) {
            state[1][i] = state[1][(i + 1) % nRow];
        }
        state[1][nRow - 1] = temp1;

        // row 2, left shift 2 bytes
        temp1 = state[2][0];
        temp2 = state[2][1];
        for (i = 0; i < nRow - 2; i++) {
            state[2][i] = state[2][(i + 2) % nRow];
        }
        state[2][nRow - 2] = temp1;
        state[2][nRow - 1] = temp2;

        // row 3, left shift 3 bytes
        temp1 = state[3][0];
        temp2 = state[3][1];
        temp3 = state[3][2];
        for (i = 0; i < nRow - 3; i++) {
            state[3][i] = state[3][(i + 3) % nRow];
        }
        state[3][nRow - 3] = temp1;
        state[3][nRow - 2] = temp2;
        state[3][nRow - 1] = temp3;

    }
    private static int xtime(int b) {
        // Left shift by 1 no more than 8 bit
        if ((b & 0x80) == 0) { //  & 1000 0000
            return b << 1;
        }
        return (b << 1) ^ 0x11b; //  XOR 1 0001 1011
    }
    private static int mult(int a, int b) {
        int sum = 0;
        while (a != 0) { // a = 11 or 10 or 1
            if ((a & 1) != 0) { // check if the first bit of a is 1
                sum = sum ^ b; // XOR b
            }
            b = xtime(b); // bit shift left 1 mod 0x11b if necessary
            a = a >>> 1;
        }
        return sum;
    }

    // MUL * state
    private static final int[][] MUL = new int[][]{
            {2, 3, 1, 1},
            {1, 2, 3, 1},
            {1, 1, 2, 3},
            {3, 1, 1, 2}
    };
    private void mixColumns(int[][] state) {
        int temp0, temp1, temp2, temp3;
        for (int c = 0; c < nRow; c++) {

            temp0 = mult(0x02, state[0][c]) ^ mult(0x03, state[1][c]) ^ state[2][c] ^ state[3][c];
            temp1 = state[0][c] ^ mult(0x02, state[1][c]) ^ mult(0x03, state[2][c]) ^ state[3][c];
            temp2 = state[0][c] ^ state[1][c] ^ mult(0x02, state[2][c]) ^ mult(0x03, state[3][c]);
            temp3 = mult(0x03, state[0][c]) ^ state[1][c] ^ state[2][c] ^ mult(0x02, state[3][c]);

            state[0][c] = temp0;
            state[1][c] = temp1;
            state[2][c] = temp2;
            state[3][c] = temp3;
        }

    }
    private int[][] cipher(int[][] in, int[][] out) {
        // Copy in to out
        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in.length; j++) {
                out[i][j] = in[i][j];
            }
        }

        int r = 0;

        // Round 0
        addRoundKey(out, r); // Data XOR w[0]-w[3]

        // Round 1 -> Round 9
        for (r = 1; r < nRound; r++) {
            subBytes(out);
            shiftRows(out); //dịch byte
            mixColumns(out); //nhan
            addRoundKey(out, r);
        }

        // Round 10
        subBytes(out);
        shiftRows(out);
        addRoundKey(out, nRound);

        return out;
    }
    public byte[] encrypt(byte[] text) {
        if (text.length != 16) {
            throw new IllegalArgumentException("Only 16-byte blocks can be encrypted");
        }
        byte[] out = new byte[text.length];

        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < 4; j++) {
                state[0][j][i] = text[i * nRow + j] & 0xff;
            }
        }

        cipher(state[0], state[1]);

        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < 4; j++) {
                out[i * nRow + j] = (byte) (state[1][j][i] & 0xff);
            }
        }
        return out;
    }
    private void invShiftRows(int[][] state) {
        int temp1, temp2, temp3, i;

        // row 1; shift right 1
        temp1 = state[1][nRow - 1];
        for (i = nRow - 1; i > 0; i--) {
            state[1][i] = state[1][(i - 1) % nRow];
        }
        state[1][0] = temp1;
        // row 2: shift right 2
        temp1 = state[2][nRow - 1];
        temp2 = state[2][nRow - 2];
        for (i = nRow - 1; i > 1; i--) {
            state[2][i] = state[2][(i - 2) % nRow];
        }
        state[2][1] = temp1;
        state[2][0] = temp2;

        // row 3: shift right 3
        temp1 = state[3][nRow - 3];
        temp2 = state[3][nRow - 2];
        temp3 = state[3][nRow - 1];
        for (i = nRow - 1; i > 2; i--) {
            state[3][i] = state[3][(i - 3) % nRow];
        }
        state[3][0] = temp1;
        state[3][1] = temp2;
        state[3][2] = temp3;

    }
    private static int invSubWord(int word) {
        int subWord = 0;
        for (int i = 24; i >= 0; i -= 8) {
            int in = word << i >>> 24;
            subWord |= rsBox[in] << (24 - i);
        }
        return subWord;
    }
    private void invSubBytes(int[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < nRow; j++) {
                state[i][j] = invSubWord(state[i][j]) & 0xFF;
            }
        }
    }
    private int[][] invMixColumnas(int[][] state) {
        int temp0, temp1, temp2, temp3;
        for (int c = 0; c < nRow; c++) {
            temp0 = mult(0x0e, state[0][c]) ^ mult(0x0b, state[1][c]) ^ mult(0x0d, state[2][c]) ^ mult(0x09, state[3][c]);
            temp1 = mult(0x09, state[0][c]) ^ mult(0x0e, state[1][c]) ^ mult(0x0b, state[2][c]) ^ mult(0x0d, state[3][c]);
            temp2 = mult(0x0d, state[0][c]) ^ mult(0x09, state[1][c]) ^ mult(0x0e, state[2][c]) ^ mult(0x0b, state[3][c]);
            temp3 = mult(0x0b, state[0][c]) ^ mult(0x0d, state[1][c]) ^ mult(0x09, state[2][c]) ^ mult(0x0e, state[3][c]);

            state[0][c] = temp0;
            state[1][c] = temp1;
            state[2][c] = temp2;
            state[3][c] = temp3;
        }
        return state;
    }
    private int[][] decipher(int[][] in, int[][] out) {
        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in.length; j++) {
                out[i][j] = in[i][j];
            }
        }
        int r = nRound;
        addRoundKey(out, r);

        for (r = nRound - 1; r > 0; r--) {
            invShiftRows(out);
            invSubBytes(out);
            addRoundKey(out, r);
            invMixColumnas(out);
        }
        invShiftRows(out);
        invSubBytes(out);
        addRoundKey(out, r);
        return out;

    }
    public byte[] decrypt(byte[] text) {
        if (text.length != 16) {
            throw new IllegalArgumentException("Only 16-byte blocks can be encrypted");
        }
        byte[] out = new byte[text.length];

        for (int i = 0; i < nRow; i++) { // columns
            for (int j = 0; j < 4; j++) { // rows
                state[0][j][i] = text[i * nRow + j] & 0xff;
            }
        }

        decipher(state[0], state[1]);

        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < 4; j++) {
                out[i * nRow + j] = (byte) (state[1][j][i] & 0xff);
            }
        }
        return out;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[Math.min(a.length, b.length)];
        for (int j = 0; j < result.length; j++) {
            int xor = a[j] ^ b[j];
            result[j] = (byte) (0xff & xor);
        }
        return result;
    }

    public byte[] ECB_encrypt(byte[] text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length; i+=16) {
            try {
                out.write(encrypt(Arrays.copyOfRange(text, i, i + 16)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    public byte[] ECB_decrypt(byte[] text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length; i+=16) {
            try {
                out.write(decrypt(Arrays.copyOfRange(text, i, i + 16)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    public byte[] CBC_encrypt(byte[] text) {
        byte[] previousBlock = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length; i+=16) {
            byte[] part = Arrays.copyOfRange(text, i, i + 16);
            try {
                if (previousBlock == null) previousBlock = iv;
                part = xor(previousBlock, part);
                previousBlock = encrypt(part);
                out.write(previousBlock);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    public byte[] CBC_decrypt(byte[] text) {
        byte[] previousBlock = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length; i+=16) {
            byte[] part = Arrays.copyOfRange(text, i, i + 16);
            byte[] tmp = decrypt(part);
            try {
                if (previousBlock == null) previousBlock = iv;
                tmp = xor(previousBlock, tmp);
                previousBlock = part;
                out.write(tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toByteArray();
    }
}
