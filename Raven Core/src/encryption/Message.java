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

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import safe.KeyEntity;
import exceptions.MessageDecrypterException;
import exceptions.MessageEncrypterException;

public class Message {
	
	private String message;
	private String random;
	
	public Message(String message){
		this.message =  message;
	}
	
	private String generateRandom(){
		SecureRandom random = new SecureRandom();
	    byte bytes[] = new byte[3];
	    random.nextBytes(bytes);
		try {
			return new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {}
		return "";
	}
	
	private byte [] combine(String message, String random){
		return (random+message).getBytes();
	}
	
	private String uncombine(String decmessage){
		byte[] temp = decmessage.substring(3, decmessage.length()).getBytes();
		if(temp[temp.length-1] == 0){
			for(int i= temp.length-1; i >= 0; i--){
				if(temp[i] != 0){
					byte[] temp2 = new byte[i+1];
					System.arraycopy(temp, 0, temp2, 0, i+1);
					temp = temp2;
					break;
				}
			}
		}
		return new String(temp);
	}
	
	public String encryptedMessage(KeyEntity key) throws MessageEncrypterException{
		this.random = generateRandom();
		try {
			if(key.getVersion() == KeyEntity.BLAKE_AES_NONE_1) // AES
				return new String(Base64.encode(AESCrypt.encrypt(key.getFirstKey(), combine(message, random)), Base64.NO_WRAP), "US-ASCII");
			if(key.getVersion() == KeyEntity.BLAKE_AES_SERPENT_1) // AES & Serpent
				return new String(Base64.encode(AES_SERPENT.encrypt(key.getFirstKey(), key.getSecondKey(), combine(message, random)), Base64.NO_WRAP), "US-ASCII");
			if(key.getVersion() == KeyEntity.BLAKE_AES_THREEFISH_1) // AES & Threefish
				return new String(Base64.encode(AES_THREEFISH.encrypt(key.getFirstKey(), key.getSecondKey(), combine(message, random)), Base64.NO_WRAP), "US-ASCII");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MessageEncrypterException();
		}
		return null;
	}
	
	public String decryptedMessage(KeyEntity key) throws MessageDecrypterException{
		try {
			if(key.getVersion() == KeyEntity.BLAKE_AES_NONE_1) // AES
				return uncombine(new String(AESCrypt.decrypt(key.getFirstKey(), Base64.decode(message.getBytes(), Base64.NO_WRAP)), "UTF-8"));
			if(key.getVersion() == KeyEntity.BLAKE_AES_SERPENT_1) // AES & Serpent
				return uncombine(new String(AES_SERPENT.decrypt(key.getFirstKey(), key.getSecondKey(), Base64.decode(message.getBytes(), Base64.NO_WRAP)), "UTF-8"));
			if(key.getVersion() == KeyEntity.BLAKE_AES_THREEFISH_1) // AES & Threefish
				return uncombine(new String(AES_THREEFISH.decrypt(key.getFirstKey(), key.getSecondKey(), Base64.decode(message.getBytes(), Base64.NO_WRAP)), "UTF-8"));
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new MessageDecrypterException();
		}
		return null;
	}
	
	public String toString(){
		return this.message;
	}
}
