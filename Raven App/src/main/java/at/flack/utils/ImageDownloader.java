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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import api.FacebookObject;

public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

	private Exception exception;
	private byte[] data = null;
	private Bitmap erg = null;

	@Override
	protected Bitmap doInBackground(String... params) {

		if (params.length == 1)
			return downloadImage(params[0]);
		if (params.length >= 2 && params[1].equals("attachment")) {
			data = null;
			FacebookPullService service;
			try {
				service = new FacebookPullService(
						"Mozilla/5.0 (Linux; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Mobile Safari/537.36 ACHEETAHI/2100050054",
						params[2]);
				service.downloadImage(params[0], new FacebookPullProcessor() {

					@Override
					public void process(byte[] b) {
						data = b;
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inPreferredConfig = Bitmap.Config.ARGB_8888;
						erg = BitmapFactory.decodeByteArray(data, 0, data.length, options);
					}

					public void process(ArrayList<FacebookObject> alo) {
					}

				});
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (data != null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				return BitmapFactory.decodeByteArray(data, 0, data.length, options);
			}
		}
		return erg;
	}

	public static Bitmap downloadImage(String url) {
		URL link = null;
		Bitmap bmp = null;
		try {
			link = new URL(url);
		} catch (MalformedURLException e1) {
		}
		if (link == null)
			return null;
		InputStream in = null;
		try {
			in = new BufferedInputStream(link.openStream());
			bmp = BitmapFactory.decodeStream(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bmp;
	}

}