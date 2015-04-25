/*
 Copyright 2015 Philipp Adam, Manuel Caspari, Nicolas Lukaschek
 contact@ravenapp.org

 This file is part of Raven.

 Raven is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Raven is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Raven. If not, see <http://www.gnu.org/licenses/>.

*/

package encryption;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.ThreefishEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

public class ThreefishBouncyCastle {
 
    private final static BlockCipher ThreefishCipher = new ThreefishEngine(256);
 
    private static PaddedBufferedBlockCipher pbbc;
    private static KeyParameter key;
  
 
    public static byte[] encrypt(byte[] key, byte[] input) throws DataLengthException, InvalidCipherTextException {
    	pbbc = new PaddedBufferedBlockCipher(ThreefishCipher, new PKCS7Padding());
    	ThreefishBouncyCastle.key = new KeyParameter(key);
        return processing(input, true);
    }
 
    public static byte[] decrypt(byte[] key, byte[] input) throws DataLengthException, InvalidCipherTextException {
    	pbbc = new PaddedBufferedBlockCipher(ThreefishCipher, new PKCS7Padding());
    	ThreefishBouncyCastle.key = new KeyParameter(key);
        return processing(input, false);
    }
 
    private static byte[] processing(byte[] input, boolean encrypt)
            throws DataLengthException, InvalidCipherTextException {
 
        pbbc.init(encrypt, key);
 
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(
            input, 0, input.length, output, 0);
 
        pbbc.doFinal(output, bytesWrittenOut);
 
        return output;
 
    }
 
}