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

package exchange;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import safe.KeyEntity;
import tools.ByteHelper;
import encryption.Base64;
import exceptions.WrongByteFormatException;

public class ExchangeInformation {

	public static final byte SMS = 0x01;
	public static final byte FACEBOOK = 0x02;
	public static final byte MAIL = 0x03;
	
	
	private String myID;
	private String otherPersonID; 
	private byte version = 0x01; 
	private byte type; 
	private KeyEntity key;
	private byte[] randomConfirmation = new byte[4];
	private long timestamp;
	private ByteHelper bh;


	public ExchangeInformation(byte [] ergdec) throws WrongByteFormatException{
		decode(ergdec);
	}
	
	
	public ExchangeInformation(String myid, String id, byte [] key, byte [] key2, byte type, byte version){
		randomConfirmation = getRandomConfirm();
		encode(myid, id, key, key2, type, version);
		this.key = new KeyEntity(key, key2, timestamp, type);
		this.myID = id;
		this.otherPersonID = myid;
		this.type = type;
	}
	
	private byte [] getRandomConfirm(){
		SecureRandom random = new SecureRandom();
	    byte erg[] = new byte[4];
	    random.nextBytes(erg);
		return erg;
	}
	
	private void decode(byte [] ergdec) throws WrongByteFormatException{
		byte [] erg;
		try{
			erg = Base64.decode(ergdec, Base64.NO_WRAP);
		}catch(Exception e){return;}
		byte version_ = erg[0];
		version = version_;
		bh = new ByteHelper(erg);
		int headerof = Long.SIZE/8 +3;
		if(erg.length - erg[1] - 79 <= 0) 
			throw new WrongByteFormatException();
		otherPersonID = new String(bh.get(erg[1]+headerof, erg.length - erg[1] - headerof - 68));

		myID = new String(bh.get(headerof, erg[1]));
		type = bh.get(headerof-1, 1)[0];
		timestamp = bytesToLong(bh.get(2, Long.SIZE/8));
		key = new KeyEntity(bh.get(erg.length - 68, 32), bh.get(erg.length - 36, 32), timestamp, type);
		randomConfirmation = bh.get(erg.length-4, 4);

	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	private void encode(String myid, String id, byte [] key, byte [] key2, byte type, byte version){
		timestamp = System.currentTimeMillis();
		bh = new ByteHelper(512, new byte[]{version}, new byte []{(byte) id.length()}, longToBytes(timestamp), new byte[]{type}, id.getBytes(), myid.getBytes(), key, key2, randomConfirmation);
	}
	
	public byte [] getEncdodedBytes(){
		return Base64.encodeToString(bh.get(), Base64.NO_WRAP).getBytes();
	}
	
	private byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/8);
	    buffer.putLong(x);
	    return buffer.array();
	}

	private long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/8);
	    buffer.put(bytes);
	    buffer.flip();
	    return buffer.getLong();
	}
	
	public byte getVersion(){
		return version;
	}
	
	public String getMyID(){
		return otherPersonID;
	}
	
	public String getID(){
		return myID;
	}
	
	public String getPhoneNumber(){
		return getID();
	}
	
	public KeyEntity getKey(){
		return key;
	}
	
	public byte[] getRandomConfirmation(){
		return randomConfirmation;
	}
	


}
