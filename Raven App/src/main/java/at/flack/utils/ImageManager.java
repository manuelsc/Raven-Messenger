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

package at.flack.utils;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

public class ImageManager {

	public static boolean saveImage(Activity ac, Bitmap imageToSave, String fileName) {
		File direct = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/Raven");
		if (!direct.exists()) {
			File wallpaperDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/Raven");
			wallpaperDirectory.mkdirs();
		}

		File file = new File(new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/Raven"),
				fileName);
		if (file.exists()) {
			return true;
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			if (fileName.endsWith(".jpg"))
				imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
			else if (fileName.endsWith(".png"))
				imageToSave.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		MediaScannerConnection.scanFile(ac, new String[] { file.getAbsolutePath() }, null,
				new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
					}

				});
		return true;
	}

	public static boolean imageAlreadyThere(String name) {
		return new File(new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/Raven"), name)
				.exists();
	}

	public static boolean removeImage(String name) {
		return new File(new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/Raven"), name)
				.delete();
	}

	public static Bitmap getSavedImagePreview(String name) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.outWidth = 190;
		options.outHeight = 140;
		return BitmapFactory.decodeFile(new File(new File(Environment.getExternalStorageDirectory().getPath()
				+ "/Pictures/Raven"), name).getAbsolutePath(), options);
	}

	public static void openImageViewer(Activity a, String name) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Pictures/Raven/"
				+ name), "image/*");
		a.startActivity(intent);
	}

}
