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

package at.flack.exchange;

import hash.ScryptTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import safe.KeyEntity;
import safe.KeySafeAbstract;
import android.app.Activity;
import android.provider.Settings.Secure;
import exceptions.KeyAlreadyMappedException;
import exchange.ExchangeInformation;

public class KeySafe extends KeySafeAbstract {

	private static KeySafe instance;

	private KeySafe(Object activity, KeyEntity universal_key) {
		super(activity, universal_key);
	}

	public static KeySafe getInstance(Object activity) {
		if (instance == null) {
			try {
				instance = new KeySafe(activity, new KeyEntity(ScryptTool.hashUltraLow(pw((Activity) activity)
						.getBytes(), loadSalt(((Activity) activity))), 0L, (byte) (0xff)));
				instance.load();
			} catch (Exception e) {
				e.printStackTrace();
				saveSalt((Activity) activity, ScryptTool.generateSalt(32));
				try {
					instance = new KeySafe(activity, new KeyEntity(ScryptTool.hashUltraLow(pw((Activity) activity)
							.getBytes(), loadSalt(((Activity) activity))), 0L, (byte) (0xff)));
					instance.load();
				} catch (Exception e1) {
					e1.printStackTrace();
					instance = new KeySafe((Activity) activity, null);
					try {
						instance.load();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}

		}
		return instance;
	}

	private static String pw(Activity ac) {
		return Secure.getString(ac.getContentResolver(), Secure.ANDROID_ID);
	}

	@Override
	public File fileLocation() {
		return new File(((Activity) activity).getFilesDir(), "keysafe.dat");
	}

	public static boolean existsFile(Activity activity) {
		return new File(activity.getFilesDir(), "keysafe.dat").exists();
	}

	public void put(ExchangeInformation ei, boolean force) throws KeyAlreadyMappedException {
		if (map.containsKey(ei.getPhoneNumber()) && !force)
			throw new KeyAlreadyMappedException(ei.getPhoneNumber() + " already used in map.");
		map.put(ei.getPhoneNumber().replaceAll("\\s+", "").replace("\n", "").replace("\r", ""), ei.getKey());
	}

	public static byte[] loadSalt(Activity activity) throws StreamCorruptedException, FileNotFoundException,
			IOException {
		ObjectInputStream inputStream = null;
		byte[] erg = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(activity.getFilesDir(), "salt.dat")));
			erg = (byte[]) inputStream.readObject();
		} catch (ClassNotFoundException e) {
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return erg;
	}

	public static void saveSalt(Activity activity, byte[] salt) {
		saveSalt(activity, salt, "salt.dat");
	}

	public static void saveSalt(Activity activity, byte[] salt, String file) {
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(new File(activity.getFilesDir(), file)));
			outputStream.writeObject(salt);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (outputStream != null)
					outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
