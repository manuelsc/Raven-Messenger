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

import safe.KeyEntity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import at.flack.GoogleAnalyticsTrack;
import at.flack.GoogleAnalyticsTrack.TrackerName;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.exchange.KeySafe;
import at.flack.ui.MailAdapter;
import at.flack.ui.MailModel;
import at.flack.utils.DHandshakeProcessor;
import at.flack.utils.HandshakeProcessor;
import at.flack.utils.ImageUrlChecker;

import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import encryption.Base64;
import encryption.Message;
import exceptions.KeyAlreadyMappedException;
import exchange.ECDHExchange;

public class MailOverview extends NFCActionBarActivity {

	private String mailAddress, mailText, mailTitle, myMail, mailDate, decryptedMailText, contactName;
	private Bitmap profilePicture;
	private ListView mail_list;
	private boolean allowKeyExchange;
	private KeyEntity key;
	private ArrayAdapter<MailModel> arrayAdapter;

	private ImageUrlChecker iuc;
	private Resources res;
	private boolean encrypt_messages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_mail_overview);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		Intent intent = getIntent();
		res = this.getResources();
		mailAddress = intent.getStringExtra("CONTACT_MAIL");
		contactName = intent.getStringExtra("CONTACT_NAME");
		myMail = intent.getStringExtra("MY_MAIL");
		key = (KeyEntity) intent.getSerializableExtra("CONTACT_KEY");
		allowKeyExchange = intent.getBooleanExtra("ALLOW_KEY_EXCHANGE", true);
		encrypt_messages = key != null;

		profilePicture = intent.getParcelableExtra("profilePicture");
		mailText = intent.getStringExtra("MAIL_TEXT");
		mailTitle = intent.getStringExtra("MAIL_TITLE");
		mailDate = intent.getStringExtra("MAIL_DATE");

		if (contactName.equals(""))
			this.setTitle(mailAddress);
		else
			this.setTitle(contactName);

		mail_list = (ListView) this.findViewById(R.id.mail_listview);

		runNFC();
		Tracker t = ((GoogleAnalyticsTrack) this.getApplication()).getTracker(TrackerName.APP_TRACKER);
		t.setScreenName("Mail Overview");
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	public void fillListWithMessages() throws Exception {
		ArrayList<MailModel> erg = new ArrayList<MailModel>();

		decryptMessage(erg, mailAddress, myMail, mailDate, profilePicture, mailText, mailTitle, contactName);

		arrayAdapter = new MailAdapter(this, erg);

		mail_list.setAdapter(arrayAdapter);
		mail_list.setSelection(arrayAdapter.getCount() - 1);
	}

	private void decryptMessage(ArrayList<MailModel> erg, String address, String myAddress, String mailDate,
			Bitmap profilePicture, String mailText, String mailTitle, String contactName) {
		int temp = mailText.lastIndexOf("=") + 1;
		if (temp <= 0)
			temp = mailText.length();
		String saa = mailText.substring(0, temp);
		Message m = new Message(saa);
		if (Base64.isBase64(saa) && key != null) {
			try {
				String msg_dec = m.decryptedMessage(key);
				erg.add(new MailModel(mailTitle));
				erg.add(new MailModel(!contactName.equals("") ? contactName : mailAddress, myMail, mailDate,
						profilePicture, 0));
				erg.add(new MailModel(msg_dec, msg_dec.indexOf("<html") >= 0 || msg_dec.indexOf("<body") >= 0
						|| msg_dec.indexOf("<div") >= 0));
				decryptedMailText = msg_dec;
			} catch (Exception e) {
				e.printStackTrace();
				erg.add(new MailModel(mailTitle));
				erg.add(new MailModel(!contactName.equals("") ? contactName : mailAddress, myMail, mailDate,
						profilePicture, 2));
				erg.add(new MailModel(mailText, mailText.indexOf("<html") >= 0 || mailText.indexOf("<body") >= 0
						|| mailText.indexOf("<div") >= 0));
				decryptedMailText = mailText;
			}
		} else {
			erg.add(new MailModel(mailTitle));
			erg.add(new MailModel(!contactName.equals("") ? contactName : mailAddress, myMail, mailDate,
					profilePicture, 1));
			erg.add(new MailModel(mailText, mailText.indexOf("<html") >= 0 || mailText.indexOf("<body") >= 0
					|| mailText.indexOf("<div") >= 0));
			searchForHandshake(erg.get(erg.size() - 1));
			decryptedMailText = mailText;
		}
	}

	public void searchForHandshake(MailModel mm) {
		if (mm.getMessage().length() > 0 && mm.getMessage().charAt(0) == '%'
				&& (mm.getMessage().length() == 10 || mm.getMessage().length() == 9)) {
			if (Base64.isPureBase64(mm.getMessage().substring(1, 9))) {
				String str_dec = new String(Base64.decode(mm.getMessage().substring(1, 9), Base64.NO_WRAP));
				if (MainActivity.tempSafe.containsKey(str_dec)) {
					Intent returnInten = new Intent();
					returnInten.putExtra("ADD_NEW_KEY_CONFIRMATIONCODE", str_dec);
					returnInten.putExtra("ADD_NEW_KEY_TELEPHONNUMBER", mailAddress);
					HandshakeProcessor hsp = new HandshakeProcessor(this);
					hsp.processReceivedRandomConfirmationCode(returnInten);
					reloadAfterKeyExchange();
				}
			}
		}

		// Diffie Hellman Exchange
		if (mm.getMessage().length() >= 120
				&& mm.getMessage().length() <= 125
				&& mm.getMessage().charAt(0) == '%'
				&& !MailOverview.this.getSharedPreferences(mailAddress, Context.MODE_PRIVATE).getString("key_pub", "")
						.equals(mm.getMessage())) {
			Log.d("dhexchange", "stage 1");
			if (Base64.isPureBase64(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=")))) {
				Log.d("dhexchange", "stage 2");
				if ((key == null || key.getVersion() == KeyEntity.ECDH_PRIVATE_KEY)) {
					new DHandshakeProcessor(this, mailAddress) {
						@Override
						public void sendHandshake(String handshaketext) {
							new NewMailActivity().sendMail(mailAddress, MailOverview.this.getResources().getString(
									R.string.mail_activity_handshake_mail_subject), handshaketext);
						}
					}.processDHExchange(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=") + 1));
					MailOverview.this.getSharedPreferences(mailAddress, Context.MODE_PRIVATE).edit().putString(
							"key_pub", mm.getMessage()).apply();
					reloadAfterKeyExchange();
				}

			}
		}

	}

	protected void reloadAfterKeyExchange() {
		key = KeySafe.getInstance(this).get(mailAddress);
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;
		this.invalidateOptionsMenu();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SharedPreferences prefs = getSharedPreferences("app", 0);
		boolean isDark = "Dark".equals(prefs.getString("theme", "Dark"));

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_overview, menu);
		MenuItem item_encrypted = menu.findItem(R.id.action_message_encrypted);
		MenuItem phone = menu.findItem(R.id.action_phone_number);

		MenuItem item_unencrypted = menu.findItem(R.id.action_qr);
		MenuItem key_info = menu.findItem(R.id.action_key_information);
		if (key == null) {
			if (!allowKeyExchange)
				item_unencrypted.setVisible(false);
			item_encrypted.setVisible(false);
			key_info.setVisible(false);
		} else {
			item_unencrypted.setVisible(false);
			key_info.setVisible(true);
		}
		phone.setVisible(false);
		menu.findItem(R.id.action_message_encrypted).setIcon(
				isDark ? R.drawable.ic_action_message_encrypted_light : R.drawable.ic_action_message_encrypted);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		case R.id.action_message_encrypted: {
			encrypt_messages = !encrypt_messages;
			if (encrypt_messages) {
				item.setIcon(R.drawable.ic_action_message_encrypted_light);

			} else {
				item.setIcon(R.drawable.ic_not_encrypted_white);
			}
			return true;
		}
		case R.id.action_key_information: {
			Intent keyinfo = new Intent(MailOverview.this, KeyInformationActivity.class);
			keyinfo.putExtra("hashed_key", new BLAKE512().digest(key.getBothKeys()));
			keyinfo.putExtra("creationdate", key.getTimeStamp());
			keyinfo.putExtra("algo", key.getVersion());
			keyinfo.putExtra("primary", mailAddress);
			startActivityForResult(keyinfo, 2);

			return true;
		}
		case R.id.action_qr: {
			Intent qr = new Intent(MailOverview.this, QRActivity.class);
			qr.putExtra("formail", mailAddress);
			qr.putExtra("fbuser", myMail);
			qr.putExtra("otherID", mailAddress);
			qr.putExtra("myID", myMail);
			qr.putExtra("type", NFCActionBarActivity.HANDSHAKE_TYPE.MAIL_HANDSHAKE);
			startActivityForResult(qr, 1);

			return true;
		}
		case R.id.action_mail_reply: {
			try {
				Intent newMail = new Intent(this, NewMailActivity.class);
				newMail.putExtra("MAIL", mailAddress);
				newMail.putExtra("SUBJECT", mailTitle.startsWith("Re:") ? mailTitle : "Re: " + mailTitle);
				newMail.putExtra("BODY", decryptedMailText);
				newMail.putExtra("KEY", key);
				startActivity(newMail);
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
					new HandshakeProcessor(this).processReceivedExchangeInformation(data, this, myMail);
					reloadAfterKeyExchange();
				}
				if (data.getBooleanExtra("request_auto_exchange", false) == true) {
					if (KeySafe.getInstance(this).contains(mailAddress)) {
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
					MailOverview.this.getSharedPreferences(mailAddress, Context.MODE_PRIVATE).edit().putLong(
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
				new NewMailActivity().sendMail(mailAddress, this.getResources().getString(
						R.string.mail_activity_handshake_mail_subject), "%" + dh.getEncodedPublicKey());
				KeySafe.getInstance(this)
						.put(mailAddress,
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

		try {
			fillListWithMessages();
		} catch (Exception e) {
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	public void onPause() {
		super.onPause();
	}

	@Override
	public HANDSHAKE_TYPE initNFCActivity() {
		setMyID(myMail);
		setOtherID(mailAddress);
		return NFCActionBarActivity.HANDSHAKE_TYPE.MAIL_HANDSHAKE;
	}

}
