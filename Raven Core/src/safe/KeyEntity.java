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

package safe;

import java.io.Serializable;

import tools.ByteHelper;

public class KeyEntity implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private byte [] key;
	private byte [] key2;
	private byte version;
	private long timestamp;
	
	// Format: HASH_ENCRYPTION_ENCRYPTION_VERSION
	public static final byte BLAKE_AES_NONE_1 		= 	0x01;
	public static final byte BLAKE_AES_MARS_1		= 	0x02;
	public static final byte BLAKE_AES_MARS_2 		= 	0x03;
	public static final byte BLAKE_AES_SERPENT_1 	= 	0x04;
	public static final byte BLAKE_AES_THREEFISH_1 	= 	0x05;
	public static final byte ECDH_PRIVATE_KEY 		= 	0x1F; // only use for temporary storage of dh private key 
	
	public static final byte DEFAULT = BLAKE_AES_THREEFISH_1;
	
	public KeyEntity(byte [] key, byte [] key2, long timestamp, byte version){
		this.key = key;
		this.key2 = key2;
		this.version = version;
		this.timestamp = timestamp;
	}

	
	public KeyEntity(byte [] key, long timestamp, byte version){
		this.key = new byte[32];
		this.key2 = new byte[32];
		System.arraycopy(key, 0, this.key, 0, 32);
		System.arraycopy(key, 32, this.key2, 0, 32);
		this.version = version;
		this.timestamp = timestamp;
	}
	
	public long getTimeStamp(){
		return timestamp;
	}
	
	public byte [] getFirstKey(){
		return key;
	}

	public byte [] getSecondKey(){
		return key2;
	}
	
	public byte getVersion(){
		return version;
	}
	
	public void setTimeStamp(long timestamp){
		this.timestamp = timestamp;
	}
	
	public void setFirstKey(byte [] b){
		key = b;
	}

	public void setSecondKey(byte [] b){
		key2 = b;
	}
	
	public void setVersion(byte version){
		this.version = version;
	}
	
	public byte[] getBothKeys(){
		ByteHelper bh = new ByteHelper(key, key2);
		return bh.get();
	}
	
	public String toString(){
		String s = "";
		for(int i = 0; i < key.length; i++)
			s+=key[i]+";";
		if(key2 != null){ 
			s+=" ||| ";
			for(int i = 0; i < key2.length; i++)
				s+=key2[i]+";";
		}
		return s;
	}
	
}
