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

package hash;

import java.security.SecureRandom;

import org.spongycastle.crypto.generators.SCrypt;

public class ScryptTool {

	public static byte [] hash(byte [] plain, byte [] salt){
		return SCrypt.generate(plain, salt, 16384, 8, 1, 64); // Colin Percival default values
	}
	
	public static byte [] hash(String plain, String salt){
		return SCrypt.generate(plain.getBytes(), salt.getBytes(), 16384, 8, 1, 64); // Colin Percival default values
	}
	
	public static byte [] hashLow(byte [] plain, byte [] salt){
		return SCrypt.generate(plain, salt, 8192, 8, 1, 64); 
	}
	
	public static byte [] hashLow(String plain, String salt){
		return SCrypt.generate(plain.getBytes(), salt.getBytes(), 8192, 8, 1, 64);
	}
	
	public static byte [] hashUltraLow(byte [] plain, byte [] salt){
		return SCrypt.generate(plain, salt, 4096, 8, 1, 64); // 
	}
	
	public static byte [] hashUltraLow(String plain, String salt){
		return SCrypt.generate(plain.getBytes(), salt.getBytes(), 4096, 8, 1, 64); 
	}
	
	public static byte [] generateSalt(int length){
		byte [] erg = new byte[length];
		SecureRandom random = new SecureRandom();
		random.nextBytes(erg);
		return erg;
	}
	
}
