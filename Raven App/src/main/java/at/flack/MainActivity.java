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

package at.flack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import mail.Email;
import mail.MailControlAndroid;
import mail.MailProfile;
import safe.KeyEntity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NdefRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import api.ChatAPI;
import api.FacebookContact;
import at.flack.GoogleAnalyticsTrack.TrackerName;
import at.flack.activity.DebugActivity;
import at.flack.activity.NewSMSContactActivity;
import at.flack.activity.QuickPrefsActivity;
import at.flack.contacts.SMSItem;
import at.flack.exchange.KeySafe;
import at.flack.exchange.PRNGFixes;
import at.flack.ui.ContactModel;
import at.flack.ui.NavBarAdapter;
import at.flack.ui.NavItemModel;
import at.flack.ui.ProfilePictureCache;
import at.flack.ui.RoundedImageView;
import at.flack.utils.HandshakeProcessor;
import at.flack.utils.NotificationService;
import at.flack.utils.UpdateProcessor;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import encryption.Base64;
import encryption.Message;
import exceptions.MessageDecrypterException;

public class MainActivity extends AppCompatActivity {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mTitle;
	private final String VERSION = "15.04.24.1";

	private static int current_fragment = -1;
	private SharedPreferences sharedPrefs;
	private static Boolean already_updating;
	private TelephonyManager tMgr;
	public static HashMap<String, KeyEntity> tempSafe;
	private static ArrayList<ContactModel> listItems;
	private static ArrayList<ContactModel> fblistItems;
	private static ArrayList<ContactModel> mailItems;
	private static ArrayList<Email> mailcontacts;
	private static ArrayList<FacebookContact> fbcontacts = null;
	private static ArrayList<SMSItem> contacts;

	private Resources res;
	public static ChatAPI fb_api;
	public static MailProfile mailprofile;
	private Bitmap profile_picture;
	private int picture_from;
	private NavBarAdapter adapter;
	public static boolean isOnTop = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		try {
			PRNGFixes.apply(); // fix SecureRandom on <= 4.3
		} catch (SecurityException e) {
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);

		already_updating = Boolean.valueOf(false);
		res = this.getResources();
		mTitle = getTitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		picture_from = Integer.parseInt(sharedPrefs.getString("profile_picture_from", "1"));

		if (sharedPrefs.getString("version", "").equals("")) { // not yet set
			sharedPrefs.edit().putString("version", VERSION).apply();
		} else if (!sharedPrefs.getString("version", "").equals(VERSION)) {
			new UpdateProcessor(this, sharedPrefs).updateMe(sharedPrefs.getString("version", ""), VERSION);
		}

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		adapter = new NavBarAdapter(this, generateNavBar());
		mDrawerList.setAdapter(adapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerList.setItemChecked(1, true);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (tempSafe == null)
			tempSafe = new HashMap<String, KeyEntity>();

		Intent intent = this.getIntent();

		if (savedInstanceState == null && current_fragment == -1) {
			selectItem(1);
		}

		if (intent.getStringExtra("CONTACT_NUMBER") != null) {
			selectItem(1);
			int pos = SMSMainActivity.getInstance().getPositionOfItem(intent.getStringExtra("CONTACT_NUMBER"));
			SMSMainActivity.getInstance().openMessageActivity(this, pos);
		}
		if (intent.getStringExtra("FACEBOOK_NAME") != null) {
			selectItem(2);
			int pos = FacebookMainActivity.getInstance().getPositionOfItem(intent.getStringExtra("FACEBOOK_NAME"));
			FacebookMainActivity.getInstance().openMessageActivity(this, pos);
		}
		if (intent.getStringExtra("MAIL") != null) {

			MailMainActivity.getInstance().onAttach(this);

			selectItem(3);
			emailLogin(1);
			int pos = MailMainActivity.getInstance().getPositionOfItem(intent.getStringExtra("MAIL"));
			MailMainActivity.getInstance().openMessageActivity(this, pos);
		}
		if (intent.getExtras() != null) {

		}

		SMSMainActivity.getInstance(true).onAttach(this);
		FacebookMainActivity.getInstance(true).onAttach(this);
		MailMainActivity.getInstance(true).onAttach(this);
		MailOutActivity.getInstance(true).onAttach(this);

		if (sharedPrefs.getBoolean("notification_fbs", true)) {
			new NotificationService(this).startNotificationService();
		}

		emailLogin(1);

		Tracker t = ((GoogleAnalyticsTrack) this.getApplication()).getTracker(TrackerName.APP_TRACKER);
		t.setScreenName("Main Activity");
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	public void emailLogin(int save, int offset, int limit) {
		new EmailProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, save, offset, limit);
	}

	public void emailLogin(int save) {
		new EmailProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, save, 0, 12);
	}

	public void loadFacebookContacts(int page) {
		new FacebookContactTask().execute(page);
	}

	public boolean facebookLogin(String mail, String password) {
		try {
			if (!new FacebookLoginTask().execute(mail, password).get()) {
				return false;
			}
		} catch (InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			return false;
		}
		redrawFbFragment();
		facebookLogin();
		FacebookMainActivity.forceReinstance();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, FacebookMainActivity.getInstance()).commit();

		return true;
	}

	public void redrawFbFragment() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.detach(FacebookMainActivity.getInstance());
		ft.commit();
	}

	public void redrawMailFragment() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.detach(MailMainActivity.getInstance());
		ft.commit();
	}

	public void drawMailFragment() {
		MailMainActivity.forceReinstance();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, MailMainActivity.getInstance()).commit();
	}

	public void facebookLogin() {
		if (fb_api == null) {
			if (existsCookie()) {
				new FacebookCookieLoginTask().execute(fb_api);
			}

		}
		loadFacebookContacts(0);
		FacebookMainActivity.getInstance().setPage(0);
	}

	public void saveCookie(String cookie) {
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(new File(this.getFilesDir(), "cookie.dat")));
			outputStream.writeObject(cookie);
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

	public boolean existsCookie() {
		return new File(this.getFilesDir(), "cookie.dat").exists();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("frag", current_fragment);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		current_fragment = savedInstanceState.getInt("frag", 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.action_qr: {
			Intent qr = new Intent(MainActivity.this, NewSMSContactActivity.class);
			startActivityForResult(qr, 1);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void cancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			selectItem(position);

		}
	}

	public String getUsername() {
		if (sharedPrefs.getString("username", "").equals("")) {
			if (tMgr != null) {
				String temp = getContactName(this, tMgr.getLine1Number());
				String name = (temp != null) ? temp : tMgr.getLine1Number();
				if (name.equals(tMgr.getLine1Number()) && fb_api != null && fb_api.getName() != null)
					name = fb_api.getName();
				sharedPrefs.edit().putString("username", name).apply();
			}
		}
		return sharedPrefs.getString("username", this.getResources().getString(R.string.username));

	}

	public static String getContactName(Context context, String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() == 0)
			return phoneNumber;
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

	private ArrayList<NavItemModel> generateNavBar() {
		ArrayList<NavItemModel> models = new ArrayList<NavItemModel>();
		models.add(new NavItemModel(RoundedImageView.getCroppedBitmap(getOwnProfilePicture(this), 300), getUsername()));
		models.add(new NavItemModel(R.drawable.ic_nav_message, res.getString(R.string.main_sidebar_sms), "0", true));
		models.add(new NavItemModel(R.drawable.ic_nav_message, res.getString(R.string.main_sidebar_facebook), "0", true));
		models.add(new NavItemModel(R.drawable.ic_nav_message, res.getString(R.string.main_sidebar_mail), "0", true));
		models.add(new NavItemModel(R.drawable.ic_nav_message, res.getString(R.string.main_sidebar_mail_out), "0",
				false));
		models.add(new NavItemModel(res.getString(R.string.main_sidebar_head_general)));
		models.add(new NavItemModel(R.drawable.ic_action_action_settings,
				res.getString(R.string.main_sidebar_settings), "0", false));
		if (sharedPrefs.getBoolean("in_debug_mode", false))
			models.add(new NavItemModel(R.drawable.ic_action_action_settings, res
					.getString(R.string.main_sidebar_debug), "0", false));
		models.add(new NavItemModel(R.drawable.ic_action_image_wb_incandescent, res
				.getString(R.string.main_sidebar_about), "0", false));
		return models;
	}

	private void selectItem(int position) {
		if (position == 0 || getFragmentManager() == null || mDrawerLayout == null || mDrawerList == null)
			return;
		Fragment fragment = null;
		if (position == 7 && !sharedPrefs.getBoolean("in_debug_mode", false))
			position = 8;

		switch (position) {
		case 1: { // SMS Main
			fragment = SMSMainActivity.getInstance();
			current_fragment = 1;
			break;
		}
		case 2: { // Facebook Main
			fragment = FacebookMainActivity.getInstance();
			current_fragment = 2;
			break;
		}
		case 3: { // Mail Main
			fragment = MailMainActivity.getInstance();
			current_fragment = 3;
			break;
		}
		case 4: { // Mail Out
			if (!getSharedPreferences("mail", Context.MODE_PRIVATE).getString("mailaddress", "").equals("")) {
				fragment = MailOutActivity.getInstance();
				current_fragment = 4;
			} else {
				fragment = MailMainActivity.getInstance();
				current_fragment = 3;
			}
			break;
		}
		case 6: { // Settings
			Intent settings = new Intent(MainActivity.this, QuickPrefsActivity.class);
			startActivityForResult(settings, 3);
			fragment = null;
			break;
		}
		case 7: {// Debug
			Intent debug = new Intent(MainActivity.this, DebugActivity.class);
			startActivity(debug);
			break;
		}
		case 8: {// About
			new AlertDialog.Builder(this).setTitle("Version " + VERSION).setMessage(
					"Developed by:\n" + "- Manuel S. Caspari\n" + "- Nicolas Lukaschek \n" + "- Philipp Adam\n\n"
							+ "Graphics by Michael Kroißmayr\n\n" + "Used Librarys:\n" + "- Emojicon by Hieu Rocker\n"
							+ "- Floating Action Button by Oleksandr Melnykov\n"
							+ "- Material Design Library by navasmdc\n" + "- Jsoup by Jonathan Hedley\n"
							+ "- Android Mail by Jon Simon\n" + "- ASync HTTP by James Smith\n"
							+ "- Appcompat & Play Services by Google\n" + "- Bouncycastle / Spongycastle\n\n"
							+ "Contact: contact@ravenapp.org\n" + "GNU GPL | 2014 - 2015").setIcon(
					R.drawable.ic_action_image_wb_incandescent).show();

			break;
		}
		default: {
			return;
		}
		}
		try {
			FragmentManager fragmentManager = getFragmentManager();

			if (fragment != null) {
				fragment.onAttach(this);
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			}
		} catch (Exception e) {
		}

		if (position < 5 && position > 0) {
			mDrawerList.setItemChecked(position, true);
			setWhiteIcon(position);
			setTitle(((NavItemModel) (mDrawerList.getItemAtPosition(position))).getTitle());
		} else {
			mDrawerList.setItemChecked(position, false);
			setAllIconsBlack();
		}

		mDrawerLayout.closeDrawer(mDrawerList);

	}

	public void setAllIconsBlack() {
		((NavItemModel) mDrawerList.getItemAtPosition(1)).setIcon(R.drawable.ic_nav_message);
		((NavItemModel) mDrawerList.getItemAtPosition(2)).setIcon(R.drawable.ic_nav_message);
		((NavItemModel) mDrawerList.getItemAtPosition(3)).setIcon(R.drawable.ic_nav_message);
		((NavItemModel) mDrawerList.getItemAtPosition(4)).setIcon(R.drawable.ic_nav_message);
	}

	public void setWhiteIcon(int position) {
		if (position >= 5 || position <= 0)
			return;
		setAllIconsBlack();
		((NavItemModel) mDrawerList.getItemAtPosition(position)).setIcon(R.drawable.ic_nav_message_light);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	public void whoEncrypts() {
		if (listItems == null)
			return;
		boolean handshake;
		for (int i = 0; i < getListItems().size(); i++) {
			handshake = false;
			if (getListItems().get(i).getLastMessage() == null)
				continue;
			// Pre Shared Exchange
			if ((getListItems().get(i).getLastMessage().toString().length() == 10 || getListItems().get(i)
					.getLastMessage().toString().length() == 9)
					&& getListItems().get(i).getLastMessage().toString().charAt(0) == '%') {
				if (Base64.isPureBase64(getListItems().get(i).getLastMessage().toString().substring(1, 9))) {
					getListItems().get(i).setLastMessage(res.getString(R.string.handshake_message));
					handshake = true;
				}
			}
			// DH Exchange
			if (getListItems().get(i).getLastMessage().toString().length() >= 120
					&& getListItems().get(i).getLastMessage().toString().length() < 125
					&& getListItems().get(i).getLastMessage().toString().charAt(0) == '%') {
				if (Base64.isPureBase64(getListItems().get(i).getLastMessage().toString().substring(1,
						getListItems().get(i).getLastMessage().toString().lastIndexOf("=")))) {
					getListItems().get(i).setLastMessage(res.getString(R.string.handshake_message));
					handshake = true;
				}
			}
			if (contacts.get(i).mAddress != null
					&& KeySafe.getInstance(this).contains(PhoneNumberUtils.formatNumber(contacts.get(i).mAddress))) {
				KeyEntity key = KeySafe.getInstance(this).get(PhoneNumberUtils.formatNumber(contacts.get(i).mAddress));
				if (key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
					getListItems().get(i).setEncrypted(View.VISIBLE);
					if (!handshake) {
						try {
							getListItems().get(i).setLastMessage(
									new Message(getListItems().get(i).getLastMessage()).decryptedMessage(key));
						} catch (MessageDecrypterException e) {
						}
					}
				}
			}

		}
		SMSMainActivity.getInstance().updateContacts(MainActivity.this);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) { // QR Code Activity (return QR code from scan)
			if (resultCode == RESULT_OK) {
				if (data.getByteArrayExtra("QR_RETURNED_Bytes") != null) {
					new HandshakeProcessor(this).processReceivedExchangeInformation(data, this, "");
				}
			}
		}

	}

	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("USASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	public void updateSMSContacts() {
		new UpdateSMSContacts().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onResume() {
		super.onResume();
		isOnTop = true;

		if (getAlreadyUpdating() == false) {
			setAlreadyUpdating(true);
			updateSMSContacts();
		}

		facebookLogin();
		selectItem(current_fragment);
	}

	@Override
	public void onPause() {
		super.onPause();
		isOnTop = false;
	}

	public void setAlreadyUpdating(boolean b) {
		synchronized (already_updating) {
			already_updating = b;
		}
	}

	public boolean getAlreadyUpdating() {
		synchronized (already_updating) {
			return already_updating;
		}
	}

	protected void onRestart() {
		super.onRestart();
	}

	protected void onStart() {
		super.onStart();
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	public String printArray(byte[] ba) {
		String s = "";
		for (byte b : ba)
			s = s + b + ";";
		return s;
	}

	public Bitmap getOwnProfilePicture(Activity activity) {
		int temp = Integer.parseInt(sharedPrefs.getString("profile_picture_from", "1"));
		if (temp != picture_from) {
			picture_from = temp;
			profile_picture = null;
		}
		try {
			return getOwnProfilePicture(activity, picture_from);
		} catch (Exception e) {
			return null;
		}

	}

	public Bitmap getOwnProfilePicture(Activity activity, int get_from) throws Exception {
		if (tMgr == null)
			tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		if (get_from == 1) {
			if (profile_picture == null) {
				profile_picture = fetchThumbnail(activity, tMgr.getLine1Number());
				if (profile_picture != null) {
					ProfilePictureCache.getInstance(this).put("2MyprofilePicture", profile_picture);
				}
				ProfilePictureCache.getInstance(this).save(this);
			}
		} else if (get_from == 2) {
			if (profile_picture == null) {
				profile_picture = ProfilePictureCache.getInstance(this).get("2MyprofilePicture");
				if (profile_picture == null && fb_api != null) {
					profile_picture = FacebookMainActivity.getInstance().getProfilePicture(fb_api.getProfilePicture());
					ProfilePictureCache.getInstance(this).put("2MyprofilePicture", profile_picture);
					ProfilePictureCache.getInstance(this).save(this);
				}
			}
		}

		return profile_picture;
	}

	private static Integer fetchThumbnailId(Activity context, String number) {
		if (number == null || number.length() == 0)
			return null;
		final Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri
				.encode(number));
		final Cursor cursor = context.getContentResolver().query(uri, SMSItem.PHOTO_ID_PROJECTION, null, null,
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

	public static final Bitmap fetchThumbnail(Activity context, String number) {
		Integer id = fetchThumbnailId(context, number);
		if (id == null)
			return null;
		final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
		final Cursor cursor = context.getContentResolver()
				.query(uri, SMSItem.PHOTO_BITMAP_PROJECTION, null, null, null);

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

	class FacebookCookieLoginTask extends AsyncTask<ChatAPI, Void, ArrayList<FacebookContact>> {

		private Exception exception;

		@Override
		protected ArrayList<FacebookContact> doInBackground(ChatAPI... params) {
			ArrayList<FacebookContact> temp = null;
			try {
				fb_api = new ChatAPI(loadCookie(), 0); // 0 for android smileys
			} catch (IOException e) {
				this.exception = e;
				e.printStackTrace();
			}
			return temp;
		}
	}

	class FacebookLoginTask extends AsyncTask<String, Void, Boolean> {

		private Exception exception;

		@Override
		protected Boolean doInBackground(String... params) {
			try {
				fb_api = new ChatAPI(params[0], params[1], 0);
				saveCookie(fb_api.getCookie());
				return true;
			} catch (IOException e) {
				this.exception = e;
				e.printStackTrace();
			}
			return false;
		}

	}

	class FacebookContactTask extends AsyncTask<Integer, Void, ArrayList<FacebookContact>> {

		private Exception exception;

		@Override
		protected ArrayList<FacebookContact> doInBackground(Integer... params) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					FacebookMainActivity.getInstance().showErrorMessage(MainActivity.this, false);
					if (FacebookMainActivity.getInstance().getProgressbar() != null && fbcontacts == null)
						FacebookMainActivity.getInstance().getProgressbar().setVisibility(View.VISIBLE);
				}
			});
			ArrayList<FacebookContact> temp = null;
			if (fb_api == null)
				return temp;
			try {
				if (getFbcontacts() != null && params[0].intValue() != 0)
					getFbcontacts().addAll(fb_api.parseContactList(fb_api.getContactList(params[0].intValue())));
				else {
					setFbcontacts(fb_api.parseContactList(fb_api.getContactList(params[0].intValue())));
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						FacebookMainActivity.getInstance().updateContacts(MainActivity.this);
						FacebookMainActivity.getInstance().updateProfilePictures(MainActivity.this);
					}
				});

			} catch (IOException | StringIndexOutOfBoundsException e) {
				this.exception = e;
				e.printStackTrace();
			}
			return temp;
		}

	}

	class EmailProfileTask extends AsyncTask<Integer, Void, Void> {

		private Exception exception;

		@Override
		protected Void doInBackground(final Integer... params) {
			try {
				SharedPreferences prefs = MainActivity.this.getSharedPreferences("mail", Context.MODE_PRIVATE);
				if (!prefs.getString("mailaddress", "").equals("")) {
					mailprofile = new mail.MailProfile(prefs.getString("mailsmtp", ""), prefs
							.getInt("mailsmtpport", 25), prefs.getString("mailpop3", ""), prefs.getInt("mailpop3port",
							110), prefs.getString("mailimap", ""), prefs.getInt("mailimapport", 143), prefs.getString(
							"mailaddress", ""), prefs.getString("mailpassword", ""), prefs.getString("mailaddress", ""));
					mailprofile.clearReceivedMessages();
					if (prefs.getBoolean("mailuseimap", true))
						MailControlAndroid.receiveEmailIMAPAndroid(mailprofile, "INBOX", params[1], params[2]); // offset,
																												// limit
					else
						MailControlAndroid.receiveEmailPOPAndroid(mailprofile, "INBOX", params[1], params[2]); // offset,
																												// limit
					setMailcontacts(mailprofile.getReceivedMessages());
					if (getMailcontacts().size() == 0)
						throw new Exception("Authentication probably failed");
					if (params[0].intValue() == 0) {
						MainActivity.this.redrawMailFragment();
						MainActivity.this.drawMailFragment();
					}
				} else
					return null;
			} catch (Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						MailMainActivity.getInstance().wrongCredentials(params[0].intValue());
					}
				});

			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					MailMainActivity.getInstance().updateContactList(MainActivity.this);
				}
			});

			return null;
		}

	}

	class UpdateSMSContacts extends AsyncTask<Integer, Void, Void> {

		private Exception exception;

		@Override
		protected Void doInBackground(Integer... params) {
			setContacts(SMSMainActivity.getInstance().readSMSContacts(MainActivity.this));
			setListItems(SMSMainActivity.getInstance().getNames(contacts, MainActivity.this));
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					whoEncrypts();
					setAlreadyUpdating(false);
				}
			});

			return null;
		}

	}

	public static synchronized ArrayList<Email> getMailcontacts() {
		return mailcontacts;
	}

	public static synchronized void setMailcontacts(ArrayList<Email> mailcontacts) {
		MainActivity.mailcontacts = mailcontacts;
	}

	public static synchronized ArrayList<ContactModel> getListItems() {
		return listItems;
	}

	public static synchronized void setListItems(ArrayList<ContactModel> listItems) {
		MainActivity.listItems = listItems;
	}

	public static synchronized ArrayList<ContactModel> getFblistItems() {
		return fblistItems;
	}

	public static synchronized void setFblistItems(ArrayList<ContactModel> fblistItems) {
		MainActivity.fblistItems = fblistItems;
	}

	public static synchronized ArrayList<ContactModel> getMailItems() {
		return mailItems;
	}

	public static synchronized void setMailItems(ArrayList<ContactModel> mailItems) {
		MainActivity.mailItems = mailItems;
	}

	public static synchronized ArrayList<FacebookContact> getFbcontacts() {
		return fbcontacts;
	}

	public static synchronized void setFbcontacts(ArrayList<FacebookContact> fbcontacts) {
		MainActivity.fbcontacts = fbcontacts;
	}

	public static synchronized ArrayList<SMSItem> getContacts() {
		return contacts;
	}

	public static synchronized void setContacts(ArrayList<SMSItem> contacts) {
		MainActivity.contacts = contacts;
	}

}
