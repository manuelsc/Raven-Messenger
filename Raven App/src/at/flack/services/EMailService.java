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

import java.util.ArrayList;
import java.util.Date;

import mail.Email;
import mail.MailControlAndroid;
import mail.MailProfile;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;

public class EMailService extends Service {

	private static Context context;

	public EMailService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		new WorkerThread().start();

	}

	public static class WorkerThread extends Thread {

		public static boolean staticBoolean = true;
		private MailProfile mailprofile;
		private ArrayList<Email> mailcontacts;
		private static long intervall = 1000 * 60 * 45;
		private SharedPreferences prefs;

		public void run() {
			staticBoolean = true;
			while (staticBoolean) {
				try {
					new EmailProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0, 0, 1).get();
				} catch (Exception e1) {
					try {
						Thread.sleep(intervall);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(intervall);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		class EmailProfileTask extends AsyncTask<Integer, Void, Void> {

			private Exception exception;

			@Override
			protected Void doInBackground(final Integer... params) {
				try {
					long lastcheck = System.currentTimeMillis() - intervall;
					if (prefs == null && context != null)
						prefs = EMailService.context.getSharedPreferences("mail", Context.MODE_PRIVATE);

					intervall = Integer.parseInt(prefs.getString("mail_pull_intervall", "2700000"));
					if (mailprofile == null) {
						if (!prefs.getString("mailaddress", "").equals("")) {
							mailprofile = new mail.MailProfile(prefs.getString("mailsmtp", ""), prefs.getInt(
									"mailsmtpport", 25), prefs.getString("mailpop3", ""), prefs.getInt("mailpop3port",
									110), prefs.getString("mailimap", ""), prefs.getInt("mailimapport", 143), prefs
									.getString("mailaddress", ""), prefs.getString("mailpassword", ""), prefs
									.getString("mailaddress", ""));
						} else
							return null;
					}

					mailprofile.clearReceivedMessages();
					if (prefs.getBoolean("mailuseimap", true))
						MailControlAndroid.receiveEmailIMAPAndroid(mailprofile, "INBOX", params[1], params[2]);
					else
						MailControlAndroid.receiveEmailPOPAndroid(mailprofile, "INBOX", params[1], params[2]); // offset,
																												// limit
					mailcontacts = mailprofile.getReceivedMessages();
					if (mailcontacts.size() == 0)
						throw new Exception("Authentication probably failed");

					for (Email mail : mailcontacts) {
						if (mail.getDate().after(new Date(lastcheck)) && context != null) {
							notify(mail);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

			private void notify(Email mailObject) {
				Intent intent = new Intent("at.flack.receiver.EMailReceiver");

				intent.putExtra("type", "mail");
				intent.putExtra("senderName", mailObject.getSenderName());
				intent.putExtra("senderMail", mailObject.getSender());
				intent.putExtra("subject", mailObject.getSubject());

				EMailService.context.sendBroadcast(intent);
			}
		}
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
