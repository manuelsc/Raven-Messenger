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

package at.flack.receiver;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ProfilePictureCache;
import at.flack.ui.RoundedImageView;
import encryption.Base64;

public class EMailReceiver extends BroadcastReceiver {

	private Activity main;
	private SharedPreferences sharedPrefs;
	private boolean notify;
	private boolean vibrate, headsup;
	private int led_color;

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		try {
			if (bundle.getString("type").equals("mail")) {
				sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
				if (!sharedPrefs.getBoolean("notification_mail", true))
					return;
				notify = sharedPrefs.getBoolean("notifications", true);
				vibrate = sharedPrefs.getBoolean("vibration", true);
				headsup = sharedPrefs.getBoolean("headsup", true);
				led_color = sharedPrefs.getInt("notification_light", -16776961);
				boolean all = sharedPrefs.getBoolean("all_mail", true);

				if (notify == false)
					return;

				NotificationCompat.Builder mBuilder = null;
				boolean use_profile_picture = false;
				Bitmap profile_picture = null;

				String origin_name = bundle.getString("senderName").equals("") ? bundle.getString("senderMail")
						: bundle.getString("senderName");

				try {
					profile_picture = RoundedImageView.getCroppedBitmap(Bitmap.createScaledBitmap(ProfilePictureCache
							.getInstance(context).get(origin_name), 200, 200, false), 300);
					use_profile_picture = true;
				} catch (Exception e) {
					use_profile_picture = false;
				}

				Resources res = context.getResources();

				int lastIndex = bundle.getString("subject").lastIndexOf("=");
				if (lastIndex > 0 && Base64.isBase64(bundle.getString("subject").substring(0, lastIndex))) {
					mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.raven_notification_icon)
							.setContentTitle(origin_name).setColor(0xFFFF5722).setContentText(
									res.getString(R.string.sms_receiver_new_encrypted_mail)).setAutoCancel(true);
					if (use_profile_picture && profile_picture != null) {
						mBuilder = mBuilder.setLargeIcon(profile_picture);
					} else {
						mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
								.getDrawable(R.drawable.ic_social_person), 200, 200));
					}
				} else {

					if (bundle.getString("subject").charAt(0) == '%'
							&& (bundle.getString("subject").length() == 10 || bundle.getString("subject").length() == 9)) {
						if (lastIndex > 0 && Base64.isBase64(bundle.getString("subject").substring(1, lastIndex))) {
							mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name).setColor(
									0xFFFF5722).setContentText(res.getString(R.string.sms_receiver_handshake_received))
									.setSmallIcon(R.drawable.notification_icon).setAutoCancel(true);
							if (use_profile_picture && profile_picture != null) {
								mBuilder = mBuilder.setLargeIcon(profile_picture);
							} else {
								mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
										.getDrawable(R.drawable.ic_social_person), 200, 200));
							}
						} else {
							return;
						}
					} else if (bundle.getString("subject").charAt(0) == '%'
							&& bundle.getString("subject").length() >= 120
							&& bundle.getString("subject").length() < 125) {
						if (lastIndex > 0 && Base64.isBase64(bundle.getString("subject").substring(1, lastIndex))) {
							mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name).setColor(
									0xFFFF5722).setContentText(res.getString(R.string.sms_receiver_handshake_received))
									.setSmallIcon(R.drawable.notification_icon).setAutoCancel(true);
							if (use_profile_picture && profile_picture != null) {
								mBuilder = mBuilder.setLargeIcon(profile_picture);
							} else {
								mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
										.getDrawable(R.drawable.ic_social_person), 200, 200));
							}
						} else {
							return;
						}
					} else if (all) { // normal message
						mBuilder = new NotificationCompat.Builder(context).setSmallIcon(
								R.drawable.raven_notification_icon).setContentTitle(origin_name).setColor(0xFFFF5722)
								.setContentText(bundle.getString("subject")).setAutoCancel(true).setStyle(
										new NotificationCompat.BigTextStyle().bigText(bundle.getString("subject")));

						if (use_profile_picture && profile_picture != null) {
							mBuilder = mBuilder.setLargeIcon(profile_picture);
						} else {
							mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
									.getDrawable(R.drawable.ic_social_person), 200, 200));
						}
					} else {
						return;
					}
				}
				// }
				Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				mBuilder.setSound(alarmSound);
				mBuilder.setLights(led_color, 750, 4000);
				if (vibrate) {
					mBuilder.setVibrate(new long[] { 0, 100, 200, 300 });
				}

				Intent resultIntent = new Intent(context, MainActivity.class);
				resultIntent.putExtra("MAIL", bundle.getString("senderMail"));

				TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
				stackBuilder.addParentStack(MainActivity.class);
				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);
				mBuilder.setContentIntent(resultPendingIntent);
				if (Build.VERSION.SDK_INT >= 16 && headsup)
					mBuilder.setPriority(Notification.PRIORITY_HIGH);
				if (Build.VERSION.SDK_INT >= 21)
					mBuilder.setCategory(Notification.CATEGORY_MESSAGE);

				final NotificationManager mNotificationManager = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);

				if (!MainActivity.isOnTop)
					mNotificationManager.notify(9, mBuilder.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Bitmap convertToBitmap(String name, Drawable drawable, int widthPixels, int heightPixels) {
		Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mutableBitmap);

		drawable.setBounds(0, 0, widthPixels, heightPixels);
		canvas.drawColor(ContactAdapter.colors[Math.abs(ContactAdapter.betterHashCode(name))
				% ContactAdapter.colors.length]);
		drawable.draw(canvas);

		return RoundedImageView.getCroppedBitmap(mutableBitmap, 300);
	}

	public void setMainActivityHandler(Activity main) {
		this.main = main;
	}

}
