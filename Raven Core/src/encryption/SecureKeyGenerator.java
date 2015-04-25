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

import hash.BLAKE512;

import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.ArrayList;

public class SecureKeyGenerator {

	private ArrayList<Byte> random_pool;
	private byte [] key1 = new byte[32];
	private byte [] key2 = new byte[32];
	private BLAKE512 blake = new BLAKE512();
	
	private final String path = "/proc/stat";
	
	public SecureKeyGenerator(){
		random_pool = new ArrayList<Byte>();
		fillWithDefaultRandom();
	}
	
	private byte [] getArray(ArrayList<Byte> ba){
		byte [] erg = new byte [ba.size()];
		for(int i=0; i < ba.size();  i++){
			erg[i] = ba.get(i);
		}
		return erg;
	}
	
	public void doRandom(){
		byte [] temp = blake.digest(getArray(random_pool));
		System.arraycopy(temp, 0, key1, 0, 32);
		System.arraycopy(temp, 32, key2, 0, 32);
	}
	
	public byte [] getAndroidNounce(){
		try {
			String s = "";
			RandomAccessFile reader = new RandomAccessFile(path, "r");
			String load;
			while((load = reader.readLine()) != null){
				s += load;
			}

			return getKey(s.getBytes());
		} catch (Exception e) {
		}
		return null;
	}
	

	private String printArray(byte [] ar){
		String s = "";
		for(byte b : ar)
			s = s + b + "; "; 
		return s;
	}
	

	private void fillWithDefaultRandom(){
		SecureRandom secrandom = new SecureRandom();
		byte [] bytes = new byte[256];
	    secrandom.nextBytes(bytes);
	    addNounce(bytes);
	}
	
	public void addNounce(byte [] ba){
		for(byte b : ba){
			random_pool.add(b);
		}
	}
	
	private byte[] getKey(byte [] b){
		byte [] temp = blake.digest(b);
		byte [] erg = new byte[32];
		for(int i=0; i < erg.length; i++)
			erg[i] = (byte) (temp[i] ^ temp[i+1]);
		return erg;
	}
	
	public byte[] getFirstKey(){
		if(key1 == null) doRandom();
		return key1;
	}
	

	public byte[] getSecondKey(){
		if(key2 == null) doRandom();
		return key2;
	}
	
	public String getHex(byte [] bytes){
		StringBuilder sb = new StringBuilder();
	    for (byte b : bytes) {
	        sb.append(String.format("%02X ", b));
	    }
	    return sb.toString();
	}
	
}
