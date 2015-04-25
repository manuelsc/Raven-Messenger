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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsMessage;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.activity.MessageOverview;
import at.flack.ui.ContactAdapter;
import at.flack.ui.RoundedImageView;
import encryption.Base64;

public class SmsReceiver extends BroadcastReceiver {

	private SharedPreferences sharedPrefs;
	private boolean notify;
	private boolean vibrate, headsup;
	private int led_color;
	private MessageOverview main = null;

	private static final String[] PHOTO_ID_PROJECTION = new String[] { ContactsContract.Contacts.PHOTO_ID };

	private static final String[] PHOTO_BITMAP_PROJECTION = new String[] { ContactsContract.CommonDataKinds.Photo.PHOTO };

	public void setMainActivityHandler(MessageOverview main) {
		this.main = main;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		notify = sharedPrefs.getBoolean("notifications", true);
		vibrate = sharedPrefs.getBoolean("vibration", true);
		headsup = sharedPrefs.getBoolean("headsup", true);
		led_color = sharedPrefs.getInt("notification_light", -16776961);
		boolean all = sharedPrefs.getBoolean("all_sms", true);

		if (notify == false)
			return;

		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;
		String str = "";
		if (bundle != null) {
			Object[] pdus = (Object[]) bundle.get("pdus");
			msgs = new SmsMessage[pdus.length];
			for (int i = 0; i < msgs.length; i++) {
				msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				str += msgs[i].getMessageBody().toString();
				str += "\n";
			}

			NotificationCompat.Builder mBuilder;

			if (main != null) {
				main.addNewMessage(str);
				return;
			}

			boolean use_profile_picture = false;
			Bitmap profile_picture = null;

			String origin_name = null;
			try {
				origin_name = getContactName(context, msgs[0].getDisplayOriginatingAddress());
				if (origin_name == null)
					origin_name = msgs[0].getDisplayOriginatingAddress();
			} catch (Exception e) {
			}
			if (origin_name == null)
				origin_name = "Unknown";
			try {
				profile_picture = RoundedImageView.getCroppedBitmap(Bitmap.createScaledBitmap(fetchThumbnail(context,
						msgs[0].getDisplayOriginatingAddress()), 200, 200, false), 300);
				use_profile_picture = true;
			} catch (Exception e) {
				use_profile_picture = false;
			}

			Resources res = context.getResources();
			int positionOfBase64End = str.lastIndexOf("=");
			if (positionOfBase64End > 0 && Base64.isBase64(str.substring(0, positionOfBase64End))) {
				mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.raven_notification_icon)
						.setContentTitle(origin_name).setColor(0xFFB71C1C).setContentText(
								res.getString(R.string.sms_receiver_new_encrypted)).setAutoCancel(true);
				if (use_profile_picture && profile_picture != null) {
					mBuilder = mBuilder.setLargeIcon(profile_picture);
				} else {
					mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources().getDrawable(
							R.drawable.ic_social_person), 200, 200));
				}

			} else if (str.toString().charAt(0) == '%'
					&& (str.toString().length() == 10 || str.toString().length() == 9)) {
				int lastIndex = str.toString().lastIndexOf("=");
				if (lastIndex > 0 && Base64.isBase64(str.toString().substring(1, lastIndex))) {
					mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name)
							.setColor(0xFFB71C1C).setContentText(
									res.getString(R.string.sms_receiver_handshake_received)).setSmallIcon(
									R.drawable.raven_notification_icon).setAutoCancel(true);
					if (use_profile_picture && profile_picture != null) {
						mBuilder = mBuilder.setLargeIcon(profile_picture);
					} else {
						mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
								.getDrawable(R.drawable.ic_social_person), 200, 200));
					}
				} else {
					return;
				}
			} else if (str.toString().charAt(0) == '%' && str.toString().length() >= 120
					&& str.toString().length() < 125) { // DH Handshake
				int lastIndex = str.toString().lastIndexOf("=");
				if (lastIndex > 0 && Base64.isBase64(str.toString().substring(1, lastIndex))) {
					mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name)
							.setColor(0xFFB71C1C).setContentText(
									res.getString(R.string.sms_receiver_handshake_received)).setSmallIcon(
									R.drawable.raven_notification_icon).setAutoCancel(true);
					if (use_profile_picture && profile_picture != null) {
						mBuilder = mBuilder.setLargeIcon(profile_picture);
					} else {
						mBuilder = mBuilder.setLargeIcon(convertToBitmap(origin_name, context.getResources()
								.getDrawable(R.drawable.ic_social_person), 200, 200));
					}
				} else {
					return;
				}
			} else { // unencrypted messages
				if (all) {
					mBuilder = new NotificationCompat.Builder(context).setContentTitle(origin_name)
							.setColor(0xFFB71C1C).setContentText(str).setAutoCancel(true).setStyle(
									new NotificationCompat.BigTextStyle().bigText(str)).setSmallIcon(
									R.drawable.raven_notification_icon);
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

			Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			mBuilder.setSound(alarmSound);
			mBuilder.setLights(led_color, 750, 4000);
			if (vibrate) {
				mBuilder.setVibrate(new long[] { 0, 100, 200, 300 });
			}

			Intent resultIntent = new Intent(context, MainActivity.class);
			resultIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			resultIntent.putExtra("CONTACT_NUMBER", msgs[0].getOriginatingAddress());

			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			if (Build.VERSION.SDK_INT >= 16 && headsup)
				mBuilder.setPriority(Notification.PRIORITY_HIGH);
			if (Build.VERSION.SDK_INT >= 21)
				mBuilder.setCategory(Notification.CATEGORY_MESSAGE);

			final NotificationManager mNotificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);

			if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") && !MainActivity.isOnTop)
				mNotificationManager.notify(7, mBuilder.build());

			// Save SMS if default app
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
					&& Telephony.Sms.getDefaultSmsPackage(context).equals(context.getPackageName())) {
				ContentValues values = new ContentValues();
				values.put("address", msgs[0].getDisplayOriginatingAddress());
				values.put("body", str.replace("\n", "").replace("\r", ""));

				if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
					context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);

			}
		}

	}

	public static String getContactName(Context context, String phoneNumber) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor cursor = cr.query(uri, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor == null) {
			return null;
		}
		String contactName = null;
		if (cursor.moveToFirst()) {
			contactName = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		return contactName;
	}

	private Integer fetchThumbnailId(Context context, String address) {

		final Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri
				.encode(address));
		final Cursor cursor = context.getContentResolver().query(uri, PHOTO_ID_PROJECTION, null, null,
				ContactsContract.Contacts.DISPLAY_NAME + " ASC");

		try {
			Integer thumbnailId = null;
			if (cursor.moveToFirst()) {
				thumbnailId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
			}
			return thumbnailId;
		} finally {
			cursor.close();
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

	public final Bitmap fetchThumbnail(Context context, String address) {
		Integer id = fetchThumbnailId(context, address);
		if (id == null)
			return null;
		final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
		final Cursor cursor = context.getContentResolver().query(uri, PHOTO_BITMAP_PROJECTION, null, null, null);

		try {
			Bitmap thumbnail = null;
			if (cursor.moveToFirst()) {
				final byte[] thumbnailBytes = cursor.getBlob(0);
				if (thumbnailBytes != null) {
					thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
				}
			}
			return thumbnail;
		} finally {
			cursor.close();
		}
	}

}
