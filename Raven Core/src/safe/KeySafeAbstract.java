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

import hash.BLAKE512;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import encryption.AES_THREEFISH;
import exceptions.KeyAlreadyMappedException;

abstract public class KeySafeAbstract {

	protected KeyEntity universal_key;
	protected final File FILE;

	protected HashMap<String, KeyEntity> map;

	protected BLAKE512 blake = new BLAKE512();

	protected Object activity;
	
	public KeySafeAbstract(Object activity, KeyEntity universal_key){
		this.universal_key = universal_key;
		this.activity = activity;
		FILE = fileLocation();
		map = new HashMap<String, KeyEntity>();
	}
	
	public abstract File fileLocation();

	public boolean existsFile(){
		return FILE.exists();
	}
	
	public void put(String primary, KeyEntity key, boolean force)
			throws KeyAlreadyMappedException {
		if (map.containsKey(primary) && !force)
			throw new KeyAlreadyMappedException(primary
					+ " already used in map.");
		map.put(primary.replaceAll("\\s+","").replace("\n", "").replace("\r", ""), key);
	}

	public void remove(String key){
		map.remove(key.replaceAll("\\s+","").replace("\n", "").replace("\r", ""));
	}
	
	public KeyEntity get(String key) {
		return map.get(key.replaceAll("\\s+","").replace("\n", "").replace("\r", ""));
	}
	
	public boolean contains(String key){
		if(map == null) return false;
		return map.containsKey(key.replaceAll("\\s+","").replace("\n", "").replace("\r", ""));
	}

	public void save(){
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(FILE));
			byte [] encodedobject = AES_THREEFISH.encrypt(universal_key.getFirstKey(), universal_key.getSecondKey(),serialize(map));
			outputStream.writeObject(encodedobject);
		} catch (Exception e) {
			e.printStackTrace();
		}  finally{
			try {
				if(outputStream != null)
					outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void changePassword(KeyEntity key){
		if(map == null)
			map = new HashMap<String, KeyEntity>();
		universal_key = key;
		save();
	}

	@SuppressWarnings("unchecked")
	public void load() {
		if(! FILE.exists()){
			map = new HashMap<String, KeyEntity>();
			return;
		}
		ObjectInputStream inputStream = null;
		try {
			inputStream = new ObjectInputStream(
					new FileInputStream(FILE));
			byte [] encodedobject = AES_THREEFISH.decrypt(universal_key.getFirstKey(), universal_key.getSecondKey(), (byte[]) inputStream.readObject());
			map = (HashMap<String, KeyEntity>) deserialize(encodedobject);
		} catch (Exception e) {
			map = new HashMap<String, KeyEntity>();
		} finally{
			try {
				if(inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public HashMap<String, KeyEntity> getMap() {
		return map;
	}

	public String toString() {
		return map.toString();
	}

	protected static byte[] serialize(Object obj) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(obj);
	    os.close();
	    return out.toByteArray();
	}
	protected static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	}
	

}
