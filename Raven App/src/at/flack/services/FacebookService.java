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

package at.flack.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import api.ChatAPI;
import api.CurrentTyping;
import api.FacebookImageMessage;
import api.FacebookMessage;
import api.FacebookObject;
import api.MarkAsRead;
import api.ReadReceipt;
import at.flack.MainActivity;
import at.flack.utils.FacebookPullProcessor;
import at.flack.utils.FacebookPullService;

public class FacebookService extends Service {

	public static boolean staticBoolean = true;

	public FacebookService() {
	}

	private void notify(FacebookObject facebookObject, long myid) {
		Intent intent = new Intent("at.flack.receiver.FacebookReceiver");
		if (facebookObject instanceof FacebookMessage) {
			FacebookMessage temp = (FacebookMessage) facebookObject;
			intent.putExtra("type", "message");
			intent.putExtra("fb_name", temp.getName());
			intent.putExtra("fb_message", temp.getMessage());
			intent.putExtra("fb_time", temp.getTime());
			intent.putExtra("fb_img", temp.getProfilePicture());
			intent.putExtra("fb_id", temp.getId());
			intent.putExtra("fb_tid", temp.getTid());
			intent.putExtra("my_id", myid);
		}
		if (facebookObject instanceof FacebookImageMessage) {
			FacebookImageMessage temp = (FacebookImageMessage) facebookObject;
			intent.putExtra("type", "img_message");
			intent.putExtra("fb_name", temp.getName());
			intent.putExtra("fb_image", temp.getImage());
			intent.putExtra("fb_preview", temp.getPreview());
			intent.putExtra("fb_time", temp.getTime());
			intent.putExtra("fb_img", temp.getProfilePicture());
			intent.putExtra("fb_id", temp.getId());
			intent.putExtra("fb_tid", temp.getTid());
			intent.putExtra("my_id", myid);
		} else if (facebookObject instanceof ReadReceipt) {
			ReadReceipt temp = (ReadReceipt) facebookObject;
			intent.putExtra("type", "readreceipt");
			intent.putExtra("fb_time", temp.getTime());
			intent.putExtra("fb_reader", temp.getReader());
		} else if (facebookObject instanceof CurrentTyping) {
			CurrentTyping temp = (CurrentTyping) facebookObject;
			intent.putExtra("type", "typ");
			intent.putExtra("my_id", temp.getMyID());
			intent.putExtra("fb_id", temp.getUserID());
			intent.putExtra("fb_status", temp.getStatus());
			intent.putExtra("fb_from_mobile", temp.isFromMobile());
		} else if (facebookObject instanceof MarkAsRead) {
			MarkAsRead temp = (MarkAsRead) facebookObject;
			intent.putExtra("type", "read");
			intent.putExtra("fb_tid", temp.getTid());
			intent.putExtra("fb_markas", temp.isMarkasread());
		}
		sendBroadcast(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("fb_service", "STARTING SERVICE");
		new Thread() {
			public void run() {
				FacebookPullService service;
				try {
					service = new FacebookPullService(
							"Mozilla/5.0 (Linux; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Mobile Safari/537.36 ACHEETAHI/2100050054",
							loadCookie());

					ChatAPI api = MainActivity.fb_api;
					if (MainActivity.fb_api == null) {
						api = new ChatAPI(loadCookie(), 0);
					}

					if (api.getChannel_url() == null)
						api.joinChatChannel();

					service.getPull(api.getChannel_url(), api.getMyID() + "", new FacebookPullProcessor() {
						public void process(ArrayList<FacebookObject> alo) {

						}

						public void process(byte[] b) {
						}
					});

					int count = 0;
					long lastRequest = 0;
					final long myid = api.getMyID();
					FacebookService.staticBoolean = true;
					while (staticBoolean) {
						try {
							lastRequest = System.currentTimeMillis();
							service.getPull(api.getChannel_url(), api.getMyID() + "", new FacebookPullProcessor() {
								public void process(ArrayList<FacebookObject> alo) {
									if (alo != null && alo.size() > 0) {
										for (int i = 0; i < alo.size(); i++) {
											FacebookService.this.notify(alo.get(i), myid);
										}
									}
								}

								public void process(byte[] b) {
								}
							});
							if (System.currentTimeMillis() - lastRequest <= 1500)
								count++;
							else
								count = 0;
							if (count >= 6) {
								Thread.sleep(75000);
								count = 0;
							}
						} catch (Exception e) {
							Thread.sleep(54000);
						}

					}
					Log.d("fb_service", "shutdown");
				} catch (Exception e) {

				}
			}
		}.start();

	}

	public String loadCookie() throws StreamCorruptedException, FileNotFoundException, IOException {
		ObjectInputStream inputStream = null;
		String erg = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(this.getFilesDir(), "cookie.dat")));
			erg = (String) inputStream.readObject();
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
