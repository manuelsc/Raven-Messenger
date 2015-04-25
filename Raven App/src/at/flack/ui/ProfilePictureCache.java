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

package at.flack.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ProfilePictureCache {
	HashMap<String, byte[]> cache;
	private static ProfilePictureCache instance;

	public static ProfilePictureCache getInstance(Context context) {
		if (instance == null) {
			instance = new ProfilePictureCache();
			try {
				instance.load(context);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	private ProfilePictureCache() {
		cache = new HashMap<String, byte[]>();
	}

	public Bitmap get(String s) {
		if (s == null || cache.get(s) == null)
			return null;
		return BitmapFactory.decodeByteArray(cache.get(s), 0, cache.get(s).length);
	}

	public boolean put(String s, Bitmap b) {
		if (b == null)
			return false;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		b.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();

		cache.put(s, byteArray);
		return true;
	}

	public boolean putNoForce(String s, Bitmap b) {
		if (b == null || cache.containsKey(s))
			return false;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		b.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();

		cache.put(s, byteArray);
		return true;
	}

	public boolean contains(String s) {
		if (s == null)
			return false;
		return cache.containsKey(s);
	}

	public void save(Context activity) {
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(new File(activity.getFilesDir(),
					"profile_picture_cache.dat")));
			outputStream.writeObject(cache);
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

	@SuppressWarnings("unchecked")
	public void load(Context activity) throws StreamCorruptedException, FileNotFoundException, IOException {
		if (!new File(activity.getFilesDir(), "profile_picture_cache.dat").exists())
			return;
		if (cache.size() > 0)
			return;
		ObjectInputStream inputStream = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(activity.getFilesDir(),
					"profile_picture_cache.dat")));
			cache = (HashMap<String, byte[]>) inputStream.readObject();
		} catch (ClassNotFoundException e) {
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
