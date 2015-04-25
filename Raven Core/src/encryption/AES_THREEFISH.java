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

import meta.Core;

public class AES_THREEFISH {

	
	public static byte [] encrypt(byte[] key_aes, byte [] key_mars, byte [] clean) throws Exception {
		if(Core.PROVIDER.equals(Core.BOUNCY_CASTLE)){
			byte [] threefish = ThreefishBouncyCastle.encrypt(key_mars, clean);
			return AESBouncyCastle.encryptNoPadding(key_aes, threefish);
		} else {
			byte [] threefish = ThreefishSpongeyCastle.encrypt(key_mars, clean);
			return AESSpongeyCastle.encryptNoPadding(key_aes, threefish);
		}
	}
	
	public static byte [] decrypt(byte[] key_aes, byte [] key_mars, byte [] encrypted) throws Exception {
		if(Core.PROVIDER.equals(Core.BOUNCY_CASTLE)){
			byte [] aes = AESBouncyCastle.decryptNoPadding(key_aes, encrypted);
			return ThreefishBouncyCastle.decrypt(key_mars, aes);
		} else {
			byte [] aes = AESSpongeyCastle.decryptNoPadding(key_aes, encrypted);
			return ThreefishSpongeyCastle.decrypt(key_mars, aes);
		}

	}
	
	
}
