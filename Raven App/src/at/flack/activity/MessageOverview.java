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

package at.flack.activity;

import hash.BLAKE512;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import safe.KeyEntity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import at.flack.GoogleAnalyticsTrack;
import at.flack.GoogleAnalyticsTrack.TrackerName;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.exchange.KeySafe;
import at.flack.receiver.SmsReceiver;
import at.flack.sms.SMSTool;
import at.flack.ui.MessageAdapter;
import at.flack.ui.MessageModel;
import at.flack.ui.ProfilePictureCache;
import at.flack.utils.DHandshakeProcessor;
import at.flack.utils.HandshakeProcessor;
import at.flack.utils.ImageDownloader;
import at.flack.utils.ImageManager;
import at.flack.utils.ImageUrlChecker;

import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import encryption.Base64;
import encryption.Message;
import exceptions.KeyAlreadyMappedException;
import exceptions.MessageDecrypterException;
import exchange.ECDHExchange;

public class MessageOverview extends NFCActionBarActivity implements EmojiconGridFragment.OnEmojiconClickedListener,
		EmojiconsFragment.OnEmojiconBackspaceClickedListener {

	private String contactName, phoneNumber;
	private Bitmap profilePicture, ownProfilePicture;
	private long id;
	private ListView sms_inbox_list;
	private EditText message_text;
	private ImageView emoji;
	private SMSTool smstool;
	private KeyEntity key;
	private ArrayAdapter<MessageModel> arrayAdapter;
	private SmsReceiver smsReceiver;
	private LinearLayout.LayoutParams params;
	private DisplayMetrics metrics;

	private static String multi = "";
	private static boolean multi_found = false;
	private boolean emojiActive;
	private ImageUrlChecker iuc;
	private Resources res;
	private boolean encrypt_messages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_message_overview);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		Intent intent = getIntent();
		res = this.getResources();
		contactName = intent.getStringExtra("CONTACT_NAME");
		phoneNumber = intent.getStringExtra("CONTACT_NUMBER");
		key = (KeyEntity) intent.getSerializableExtra("CONTACT_KEY");
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;

		byte[] bytes = intent.getByteArrayExtra("profilePicture");
		if (bytes != null)
			profilePicture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		id = intent.getLongExtra("CONTACT_ID", 0);
		ProfilePictureCache cache = null;
		try {
			cache = ProfilePictureCache.getInstance(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (cache != null)
			ownProfilePicture = cache.get("2MyprofilePicture");

		this.setTitle(contactName);

		sms_inbox_list = (ListView) this.findViewById(R.id.sms_listview);
		message_text = (EditText) this.findViewById(R.id.message);
		final ImageView send = (ImageView) this.findViewById(R.id.sendMessage);
		emoji = (ImageView) this.findViewById(R.id.emoji);
		final View fragment = MessageOverview.this.findViewById(R.id.emojicons);
		fragment.setVisibility(View.INVISIBLE);
		params = (LinearLayout.LayoutParams) fragment.getLayoutParams();
		metrics = getBaseContext().getResources().getDisplayMetrics();

		smstool = new SMSTool(this);
		send.setEnabled(false);

		message_text.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				send.setEnabled(message_text.getText().toString().length() > 0);
				if (message_text.getText().toString().length() > 0 && !encrypt_messages)
					send.setBackgroundResource(R.drawable.ic_action_send2);
				else if (message_text.getText().toString().length() > 0 && encrypt_messages)
					send.setBackgroundResource(R.drawable.ic_action_send3);
				else
					send.setBackgroundResource(R.drawable.ic_action_send);
			}
		});
		sms_inbox_list.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		message_text.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (emojiActive) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
						emoji.callOnClick();
					else
						emoji.performClick();
				}
			}
		});

		emoji.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				emojiActive = !emojiActive;
				if (fragment.getVisibility() == View.VISIBLE) {
					params.height = 0;
					fragment.setLayoutParams(params);
					fragment.setVisibility(View.INVISIBLE);
				} else {
					params.height = (int) ((metrics.density * 220) + 0.5f);
					fragment.setLayoutParams(params);
					fragment.setVisibility(View.VISIBLE);
				}
			}
		});

		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = message_text.getText().toString();
				if (arrayAdapter == null)
					arrayAdapter = new MessageAdapter(MessageOverview.this, new ArrayList<MessageModel>());
				if (message.length() > 0) {
					if (key != null && encrypt_messages) {
						smstool.sendEncryptedSMS(phoneNumber, message, key);
						arrayAdapter.add(new MessageModel("me", ownProfilePicture, message, getDate(System
								.currentTimeMillis()), false, 0));
					} else {
						smstool.sendSMS(phoneNumber, message);
						arrayAdapter.add(new MessageModel("me", ownProfilePicture, message, getDate(System
								.currentTimeMillis()), false, 1));
						message_text.setText("");
					}
					message_text.setText("");
					sms_inbox_list.setAdapter(arrayAdapter);
					sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
				} else {
					Toast.makeText(MessageOverview.this, "Message too short", Toast.LENGTH_SHORT).show();
				}
			}
		});

		sms_inbox_list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if ((MessageModel) arg0.getItemAtPosition(arg2) != null
						&& ((MessageModel) arg0.getItemAtPosition(arg2)).isImage()) {
					try {
						String name = ((MessageModel) arg0.getItemAtPosition(arg2)).getMessage();
						name = imgParse(name);
						if (!ImageManager.imageAlreadyThere(name)) {
							Bitmap a = new ImageDownloader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
									((MessageModel) arg0.getItemAtPosition(arg2)).getMessage()).get();

							if (ImageManager.saveImage(MessageOverview.this, a, name))
								ImageManager.openImageViewer(MessageOverview.this, name);
							else {
								Toast.makeText(MessageOverview.this, "Error while loading picture", Toast.LENGTH_SHORT)
										.show();
								ImageManager.removeImage(name);
							}
						} else {
							ImageManager.openImageViewer(MessageOverview.this, name);
						}
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
			}
		});

		runNFC();
		Tracker t = ((GoogleAnalyticsTrack) this.getApplication()).getTracker(TrackerName.APP_TRACKER);
		t.setScreenName("SMS Overview");
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("contactName", contactName);
		savedInstanceState.putString("phoneNumber", phoneNumber);
		savedInstanceState.putSerializable("key", key);
		savedInstanceState.putLong("id", id);
		savedInstanceState.putBoolean("encrypt_messages", encrypt_messages);
		super.onSaveInstanceState(savedInstanceState);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		contactName = savedInstanceState.getString("contactName");
		phoneNumber = savedInstanceState.getString("phoneNumber");
		id = savedInstanceState.getLong("id");
		key = (KeyEntity) savedInstanceState.getSerializable("key");
		encrypt_messages = savedInstanceState.getBoolean("encrypt_messages");
	}

	private void markMessageRead(Context context, String number, String body) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT
				|| !Telephony.Sms.getDefaultSmsPackage(context).equals(context.getPackageName()))
			return; // if not default SMS App -> cant mark as read on KitKat+
		Uri uri = Uri.parse("content://sms/inbox");
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		try {

			while (cursor.moveToNext()) {
				if ((cursor.getString(cursor.getColumnIndex("address")).equals(number))
						&& (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
					if (cursor.getString(cursor.getColumnIndex("body")).startsWith(body)) {
						String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
						ContentValues values = new ContentValues();
						values.put("read", true);
						context.getContentResolver().update(Uri.parse("content://sms/inbox"), values,
								"_id=" + SmsMessageId, null);
						return;
					}
				}
			}
		} catch (Exception e) {
			Log.e("Mark Read", "Error in Read: " + e.toString());
		}
	}

	public String imgParse(String name) {
		String erg = "";
		try {
			erg = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".") + 4);
		} catch (StringIndexOutOfBoundsException e) {
		}
		return erg.toLowerCase(Locale.getDefault());
	}

	@Override
	public void onEmojiconClicked(Emojicon emojicon) {
		EmojiconsFragment.input(message_text, emojicon);
	}

	@Override
	public void onEmojiconBackspaceClicked(View v) {
		EmojiconsFragment.backspace(message_text);
	}

	public void addNewMessage(String message) {
		if (iuc == null)
			iuc = new ImageUrlChecker();
		if (arrayAdapter == null)
			return;
		int type;
		String tempm = message;
		if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
			try {
				message = new Message(message).decryptedMessage(key);
				type = 0; // encrypted
			} catch (MessageDecrypterException e) {
				message = tempm;
				if (Base64.isBase64(message))
					type = 2; // cant encrypt
				else
					type = 1;
			}
		} else {
			type = 1; // not encrypted
		}
		MessageModel model;
		if (iuc.isImageUrl(message)) {
			String n2 = imgParse(message);
			if (ImageManager.imageAlreadyThere(n2))
				model = new MessageModel(contactName, profilePicture, message, ImageManager.getSavedImagePreview(n2),
						getDate(System.currentTimeMillis()), true, type);
			else
				model = new MessageModel(contactName, profilePicture, message, null,
						getDate(System.currentTimeMillis()), true, type);
		} else
			model = new MessageModel(contactName, profilePicture, message, getDate(System.currentTimeMillis()), true,
					type);

		ArrayList<MessageModel> ta = new ArrayList<MessageModel>();
		ta.add(model);
		searchForHandshake(ta);
		if (ta.size() >= 1)
			arrayAdapter.add(ta.get(0));
		sms_inbox_list.setAdapter(arrayAdapter);
		sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
		new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				cancelNotification(MessageOverview.this, 7);
			}
		}.start();
		markMessageRead(this, phoneNumber, message);
	}

	public void cancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	public void fillListWithMessages(int max) throws Exception {
		Uri uri = Uri.parse("content://sms/");
		ContentResolver contentResolver = getContentResolver();
		Cursor cursor = contentResolver.query(uri, new String[] { "thread_id", "date", "body", "type" }, "thread_id = "
				+ id, null, null);

		if (cursor.getCount() == 0)
			return;
		ArrayList<MessageModel> erg = new ArrayList<MessageModel>();
		if (max <= 0)
			max = cursor.getCount();
		cursor.moveToPosition(max - 1);

		multi = "";
		multi_found = false;
		iuc = new ImageUrlChecker();
		MessageModel te;
		if (key != null) {
			do {
				te = decryptMessages(cursor.getString(cursor.getColumnIndex("body")), cursor.getLong(cursor
						.getColumnIndex("date")), cursor.getInt(cursor.getColumnIndex("type")));
				if (te != null)
					erg.add(te);
			} while ((cursor.moveToPrevious()));
		} else {
			do {
				te = addMessage(cursor.getString(cursor.getColumnIndex("body")), cursor.getLong(cursor
						.getColumnIndex("date")), cursor.getInt(cursor.getColumnIndex("type")));
				if (te != null)
					erg.add(te);
			} while ((cursor.moveToPrevious()));
		}
		searchForHandshake(erg);
		if (sms_inbox_list.getFooterViewsCount() == 0) {
			TextView padding = new TextView(this);
			padding.setHeight(15);
			sms_inbox_list.addFooterView(padding);
			sms_inbox_list.addHeaderView(padding);
		}
		arrayAdapter = new MessageAdapter(this, erg);

		sms_inbox_list.setAdapter(arrayAdapter);
		sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
	}

	private MessageModel decryptMessages(String mess, long date, long type) throws Exception {
		if (mess == null)
			return null;
		Message mes = new Message(mess.replace("\n", "").replace("\r", ""));
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		String name = isNotMe ? contactName : "me";
		if (Base64.isBase64(mes.toString())) {
			try {
				if (mes.toString().charAt(mes.toString().length() - 1) == '$') {
					multi += mes.toString().substring(0, mes.toString().length() - 1);
					multi_found = true;
					return null;
				} else {
					if (multi_found == true) {
						multi += mes.toString().substring(0, mes.toString().length());
						MessageModel temp = new MessageModel(name, picture, new Message(multi).decryptedMessage(key),
								getDate(date), isNotMe, key.getTimeStamp() < date ? 0 : 2);
						multi_found = false;
						multi = "";
						return temp;
					} else {
						String msg_dec = mes.decryptedMessage(key);
						if (iuc == null)
							iuc = new ImageUrlChecker();
						if (iuc.isImageUrl(msg_dec)) {
							String n2 = imgParse(msg_dec);
							if (ImageManager.imageAlreadyThere(n2))
								return new MessageModel(name, picture, msg_dec, ImageManager.getSavedImagePreview(n2),
										getDate(date), isNotMe, key.getTimeStamp() < date ? 0 : 2);
							else
								return new MessageModel(name, picture, msg_dec, null, getDate(date), isNotMe, key
										.getTimeStamp() < date ? 0 : 2);
						} else
							return new MessageModel(name, picture, msg_dec, getDate(date), isNotMe,
									key.getTimeStamp() < date ? 0 : 2);
					}
				}
			} catch (Exception e) {
				if (multi_found == true) {
					multi_found = false;
					multi = "";
				}
				return new MessageModel(name, picture, mes.toString(), getDate(date), isNotMe, 2);
			}
		}
		return addMessage(mess, date, type);
	}

	protected void reloadAfterKeyExchange() {
		if (KeySafe.getInstance(this).contains(phoneNumber)) {
			key = KeySafe.getInstance(this).get(phoneNumber);
		}
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;
		this.invalidateOptionsMenu();
	}

	private MessageModel addMessage(String mes, long date, long type) throws Exception {
		if (mes == null)
			return null;
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		String name = isNotMe ? contactName : "me";
		if (iuc == null)
			iuc = new ImageUrlChecker();
		if (iuc.isImageUrl(mes.toString())) {
			String n2 = imgParse(mes.toString());
			if (ImageManager.imageAlreadyThere(n2))
				return new MessageModel(name, picture, mes.toString(), ImageManager.getSavedImagePreview(n2),
						getDate(date), isNotMe, 1);
			else
				return new MessageModel(name, picture, mes.toString(), null, getDate(date), isNotMe, 1);
		}
		return new MessageModel(name, picture, mes.toString(), getDate(date), isNotMe, 1);
	}

	public void searchForHandshake(ArrayList<MessageModel> erg) {
		MessageModel mm;
		int look_depth = erg.size() - 1;
		if (look_depth < 0)
			look_depth = 0;
		boolean showing_key_exchanged_notification = false;
		for (int i = erg.size() - 1; i >= 0; i--) {
			mm = erg.get(i);
			if (mm.getMessage() == null)
				continue;
			// Pre Shared
			if (mm.getMessage().length() > 0 && mm.getMessage().charAt(0) == '%'
					&& (mm.getMessage().length() == 10 || mm.getMessage().length() == 9)) {
				if (Base64.isPureBase64(mm.getMessage().substring(1, 9))) {
					String str_dec = new String(Base64.decode(mm.getMessage().substring(1, 9), Base64.NO_WRAP));
					if (MainActivity.tempSafe.containsKey(str_dec) && i >= look_depth) {
						Intent returnInten = new Intent();
						returnInten.putExtra("ADD_NEW_KEY_CONFIRMATIONCODE", str_dec);
						returnInten.putExtra("ADD_NEW_KEY_TELEPHONNUMBER", phoneNumber);
						HandshakeProcessor hsp = new HandshakeProcessor(this);
						hsp.processReceivedRandomConfirmationCode(returnInten);
						reloadAfterKeyExchange();

					} else {
						erg.remove(i);
						erg.add(i, new MessageModel(res.getString(R.string.message_handshake_completed), true));
						break;
					}
				}
			}

			// Diffie Hellman
			if (mm.getMessage().length() >= 120
					&& mm.getMessage().length() <= 125
					&& mm.getMessage().charAt(0) == '%'
					&& !MessageOverview.this.getSharedPreferences(phoneNumber, Context.MODE_PRIVATE).getString(
							"key_pub", "").equals(mm.getMessage())) {
				Log.d("dhexchange", "stage 1");
				if (Base64.isPureBase64(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=")))) {
					Log.d("dhexchange", "stage 2");
					if ((key == null || key.getVersion() == KeyEntity.ECDH_PRIVATE_KEY) && mm.isNotMe()
							&& i >= look_depth) {
						new DHandshakeProcessor(this, phoneNumber) {
							@Override
							public void sendHandshake(String handshaketext) {
								smstool.sendSMS(phoneNumber, handshaketext);
							}
						}.processDHExchange(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=") + 1));
						reloadAfterKeyExchange();
						MessageOverview.this.getSharedPreferences(phoneNumber, Context.MODE_PRIVATE).edit().putString(
								"key_pub", mm.getMessage()).apply();
					}

				}
			} else if (MessageOverview.this.getSharedPreferences(phoneNumber, Context.MODE_PRIVATE).getString(
					"key_pub", "").equals(mm.getMessage())) {
				break;
			}
		}
		for (int i = erg.size() - 1; i >= 0; i--) {
			mm = erg.get(i);
			if (mm == null || mm.getMessage() == null)
				continue;
			if (mm.getMessage().length() >= 120 && mm.getMessage().length() <= 125 && mm.getMessage().charAt(0) == '%') {
				if (Base64.isPureBase64(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=")))) {
					if (showing_key_exchanged_notification)
						erg.remove(i);
					if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY
							&& !showing_key_exchanged_notification) {
						showing_key_exchanged_notification = true;
						erg.remove(i);
						erg.add(i, new MessageModel(res.getString(R.string.message_handshake_completed), true));
					} else {
						if (!showing_key_exchanged_notification && i <= look_depth) {
							showing_key_exchanged_notification = true;
							erg.remove(i);
							erg.add(i, new MessageModel(res.getString(R.string.message_handshake_initiated), true));
						}
					}
				}
			}
		}
		markMessageRead(this, phoneNumber, erg.get(erg.size() - 1).getMessage());
	}

	public boolean deleteSms(String smsId) {
		boolean isSmsDeleted = false;
		try {
			this.getContentResolver().delete(Uri.parse("content://sms/" + smsId), null, null);
			isSmsDeleted = true;
		} catch (Exception ex) {
			isSmsDeleted = false;
		}
		return isSmsDeleted;
	}

	public String getDate(long date) {
		long cur = System.currentTimeMillis();
		StringBuilder erg;
		if ((long) (cur / 86400000) == (long) (date / 86400000))
			erg = new StringBuilder(res.getString(R.string.message_overview_date_today) + " ")
					.append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
		else if (date >= cur - 518400000)
			erg = new StringBuilder(new SimpleDateFormat("EE HH:mm", Locale.getDefault()).format(date));
		else
			erg = new StringBuilder(new SimpleDateFormat("dd. MMMM HH:mm", Locale.getDefault()).format(date));
		return erg.toString();
	}

	public String printText(String s) {
		String erg = "";
		for (int i = 0; i < s.length(); i++) {
			erg += (byte) (s.charAt(i)) + ";";
		}
		return erg;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SharedPreferences prefs = getSharedPreferences("app", 0);
		boolean isDark = "Dark".equals(prefs.getString("theme", "Dark"));

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_overview, menu);
		MenuItem item_encrypted = menu.findItem(R.id.action_message_encrypted);
		MenuItem mail = menu.findItem(R.id.action_mail_reply);
		MenuItem item_unencrypted = menu.findItem(R.id.action_qr);
		MenuItem key_info = menu.findItem(R.id.action_key_information);
		if (key == null) {
			item_encrypted.setVisible(false);
			key_info.setVisible(false);
		} else if (key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
			item_unencrypted.setVisible(false);
			key_info.setVisible(true);
			if (encrypt_messages)
				item_encrypted.setIcon(R.drawable.ic_action_message_encrypted_light);
			else
				item_encrypted.setIcon(R.drawable.ic_not_encrypted_white);
		} else {
			item_encrypted.setVisible(false);
			key_info.setVisible(false);
		}
		mail.setVisible(false);
		menu.findItem(R.id.action_message_encrypted).setIcon(
				isDark ? R.drawable.ic_action_message_encrypted_light : R.drawable.ic_action_message_encrypted);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_message_encrypted: {
			encrypt_messages = !encrypt_messages;
			if (encrypt_messages) {
				item.setIcon(R.drawable.ic_action_message_encrypted_light);
			} else {
				item.setIcon(R.drawable.ic_not_encrypted_white);
			}
			message_text.setText(message_text.getText());
			return true;
		}
		case R.id.action_key_information: {
			Intent keyinfo = new Intent(MessageOverview.this, KeyInformationActivity.class);
			keyinfo.putExtra("hashed_key", new BLAKE512().digest(key.getBothKeys()));
			keyinfo.putExtra("creationdate", key.getTimeStamp());
			keyinfo.putExtra("algo", key.getVersion());
			keyinfo.putExtra("primary", phoneNumber);
			startActivityForResult(keyinfo, 2);

			return true;
		}
		case R.id.action_qr: {
			Intent qr = new Intent(MessageOverview.this, QRActivity.class);
			qr.putExtra("forsms", phoneNumber);
			qr.putExtra("otherID", phoneNumber);
			qr.putExtra("myID", ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
			qr.putExtra("type", NFCActionBarActivity.HANDSHAKE_TYPE.SMS_HANDSHAKE);
			startActivityForResult(qr, 1);

			return true;
		}
		case R.id.action_phone_number: {
			try {
				Intent callIntent = new Intent(Intent.ACTION_DIAL);
				callIntent.setData(Uri.parse("tel:" + phoneNumber));
				startActivity(callIntent);
			} catch (ActivityNotFoundException e) {
				Log.e("call", "Call failed", e);
			}
			return true;
		}

		}
		return super.onOptionsItemSelected(item);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) { // QR Code Activity (return QR code from scan)
			if (resultCode == RESULT_OK) {
				if (data.getByteArrayExtra("QR_RETURNED_Bytes") != null) {
					new HandshakeProcessor(this).processReceivedExchangeInformation(data, this,
							((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
					reloadAfterKeyExchange();
				}
				if (data.getBooleanExtra("request_auto_exchange", false) == true) {
					if (KeySafe.getInstance(this).contains(phoneNumber)) {
						Dialog dialog = new Dialog(this, res.getString(R.string.popup_keyalreadymapped_title), res
								.getString(R.string.popup_dh_key_already_mapped));
						dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								finishDHHandshake();
							}
						});
						dialog.setOnCancelButtonClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								return;
							}
						});
						dialog.show();

					} else {
						finishDHHandshake();
					}
				}
				if (data.getBooleanExtra("reload", false) == true) {
					reloadAfterKeyExchange();
				}
			}
		} else if (requestCode == 2) { // After KeyInformationActivity
			if (resultCode == RESULT_OK) {
				if (data.getBooleanExtra("reload", false)) {
					reloadAfterKeyExchange();
					MessageOverview.this.getSharedPreferences(phoneNumber, Context.MODE_PRIVATE).edit().putLong(
							"key_delted", System.currentTimeMillis()).apply();
					finish();
				}
			}
		}
	}

	private void finishDHHandshake() {
		try {
			ECDHExchange dh = new ECDHExchange();
			try {
				smstool.sendSMS(phoneNumber, "%" + dh.getEncodedPublicKey());
				KeySafe.getInstance(this)
						.put(phoneNumber,
								new KeyEntity(dh.getPrivateKey(), null, System.currentTimeMillis(),
										KeyEntity.ECDH_PRIVATE_KEY), false);
				KeySafe.getInstance(this).save();
			} catch (KeyAlreadyMappedException e) {
				Toast.makeText(this, "ERROR: Can't save ECDH Key", Toast.LENGTH_LONG).show();
			}
		} catch (InvalidKeyException | IllegalStateException | InvalidAlgorithmParameterException
				| NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
			Toast.makeText(this, "ERROR: ECDH Exception", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	public static String getDate(long milliSeconds, String dateFormat) {
		DateFormat formatter = new SimpleDateFormat(dateFormat);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds);
		return formatter.format(calendar.getTime());
	}

	public void onResume() {
		super.onResume();
		smsReceiver = new SmsReceiver();
		smsReceiver.setMainActivityHandler(this);
		android.content.IntentFilter filter = new android.content.IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");
		this.registerReceiver(smsReceiver, filter);
		try {
			fillListWithMessages(0);
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}
		reloadAfterKeyExchange();

	}

	@Override
	public void onBackPressed() {
		if (emojiActive && emoji != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
				emoji.callOnClick();
			else
				emoji.performClick();
		} else
			finish();
	}

	public void onPause() {
		super.onPause();
		if (smsReceiver != null)
			this.unregisterReceiver(smsReceiver);
	}

	@Override
	public HANDSHAKE_TYPE initNFCActivity() {
		setMyID(((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
		setOtherID(phoneNumber);
		return NFCActionBarActivity.HANDSHAKE_TYPE.SMS_HANDSHAKE;
	}

}
