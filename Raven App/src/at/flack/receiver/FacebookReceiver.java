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
import api.FacebookImageMessage;
import api.FacebookMessage;
import api.MarkAsRead;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.activity.FbMessageOverview;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ProfilePictureCache;
import at.flack.ui.RoundedImageView;
import encryption.Base64;

public class FacebookReceiver extends BroadcastReceiver {

	private Activity main;
	private SharedPreferences sharedPrefs;
	private boolean notify;
	private boolean vibrate, headsup;
	private int led_color;

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		try {
			if (main instanceof FbMessageOverview && bundle.getString("type").equals("message")) {
				((FbMessageOverview) main).addNewMessage(new FacebookMessage(bundle.getString("fb_name"), bundle
						.getString("fb_message"), bundle.getString("fb_img"), bundle.getLong("fb_id"), bundle
						.getString("fb_tid")));
			} else if (main instanceof FbMessageOverview && bundle.getString("type").equals("readreceipt")) {
				((FbMessageOverview) main).changeReadReceipt(bundle.getLong("fb_time"), bundle.getLong("fb_reader"));
			} else if (main instanceof FbMessageOverview && bundle.getString("type").equals("typ")) {
				((FbMessageOverview) main).changeTypingStatus(bundle.getLong("my_id"), bundle.getLong("fb_id"), bundle
						.getBoolean("fb_from_mobile"), bundle.getInt("fb_status"));
			} else if (main instanceof FbMessageOverview && bundle.getString("type").equals("img_message")) {
				((FbMessageOverview) main).addNewMessage(new FacebookImageMessage(bundle.getString("fb_name"), bundle
						.getString("fb_image"), bundle.getString("fb_preview"), bundle.getString("fb_img"), bundle
						.getLong("fb_id"), bundle.getString("fb_tid")));
			} else if (bundle.getString("type").equals("read")) {
				killNotification(context, new MarkAsRead(bundle.getString("fb_tid"), bundle.getBoolean("fb_markas")));
			} else if (bundle.getString("type").equals("message") || bundle.getString("type").equals("img_message")) {
				sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
				if (!sharedPrefs.getBoolean("notification_fbs", true))
					return;
				notify = sharedPrefs.getBoolean("notifications", true);
				vibrate = sharedPrefs.getBoolean("vibration", true);
				headsup = sharedPrefs.getBoolean("headsup", true);
				led_color = sharedPrefs.getInt("notification_light", -16776961);
				boolean all = sharedPrefs.getBoolean("all_fb", true);

				if (notify == false)
					return;
				if (bundle.getLong("fb_id") == bundle.getLong("my_id")) {
					return;
				}

				NotificationCompat.Builder mBuilder = null;
				boolean use_profile_picture = false;
				Bitmap profile_picture = null;

				String origin_name = bundle.getString("fb_name");

				try {
					profile_picture = RoundedImageView.getCroppedBitmap(Bitmap.createScaledBitmap(ProfilePictureCache
							.getInstance(context).get(origin_name), 200, 200, false), 300);
					use_profile_picture = true;
				} catch (Exception e) {
					use_profile_picture = false;
				}

				Resources res = context.getResources();

				if (bundle.getString("type").equals("img_message")) {
					bundle.putString("fb_message", res.getString(R.string.fb_notification_new_image));
				}

				int lastIndex = bundle.getString("fb_message").lastIndexOf("=");
				if (lastIndex > 0 && Base64.isBase64(bundle.getString("fb_message").substring(0, lastIndex))) {
					mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.raven_notification_icon)
							.setContentTitle(origin_name).setColor(0xFF175ea2).setContentText(
									res.getString(R.string.sms_receiver_new_encrypted_fb)).setAutoCancel(true);
					if (use_profile_picture && profile_picture != null) {
						mBuilder = mBuilder.setLargeIcon(profile_picture);
					} else {
						mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
								.getDrawable(R.drawable.ic_social_person), 200, 200));
					}
				} else { // normal or handshake

					if (bundle.getString("fb_message").charAt(0) == '%'
							&& (bundle.getString("fb_message").length() == 10 || bundle.getString("fb_message")
									.length() == 9)) {
						if (lastIndex > 0 && Base64.isBase64(bundle.getString("fb_message").substring(1, lastIndex))) {
							mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name).setColor(
									0xFF175ea2).setContentText(res.getString(R.string.sms_receiver_handshake_received))
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
					} else if (bundle.getString("fb_message").charAt(0) == '%'
							&& bundle.getString("fb_message").length() >= 120
							&& bundle.getString("fb_message").length() < 125) {
						if (lastIndex > 0 && Base64.isBase64(bundle.getString("fb_message").substring(1, lastIndex))) {
							mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name).setColor(
									0xFF175ea2).setContentText(res.getString(R.string.sms_receiver_handshake_received))
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
								R.drawable.raven_notification_icon).setContentTitle(origin_name).setColor(0xFF175ea2)
								.setContentText(bundle.getString("fb_message")).setAutoCancel(true).setStyle(
										new NotificationCompat.BigTextStyle().bigText(bundle.getString("fb_message")));

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
				resultIntent.putExtra("FACEBOOK_NAME", origin_name);

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
					mNotificationManager.notify(8, mBuilder.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void killNotification(Context ctx, MarkAsRead mas) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
		nMgr.cancel(8);
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
