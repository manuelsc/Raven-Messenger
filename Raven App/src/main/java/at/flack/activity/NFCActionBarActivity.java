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

import java.nio.charset.Charset;

import safe.KeyEntity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

import at.flack.MainActivity;
import at.flack.utils.HandshakeProcessor;
import encryption.SecureKeyGenerator;
import exchange.ExchangeInformation;

abstract public class NFCActionBarActivity extends AppCompatActivity implements CreateNdefMessageCallback {

	public enum HANDSHAKE_TYPE {
		SMS_HANDSHAKE((byte) 0), FACEBOOK_HANDSHAKE((byte) 1), MAIL_HANDSHAKE((byte) 2);

		public byte type;

		HANDSHAKE_TYPE(byte type) {
			this.type = type;
		}
	}

	private static String myID;
	private static String otherID;
	protected static NfcAdapter nfcAdapter;
	protected static boolean isNfcDevice;

	public void setMyID(String myID) {
		this.myID = myID;
	}

	public void setOtherID(String otherID) {
		this.otherID = otherID;
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		SecureKeyGenerator skg = new SecureKeyGenerator();
		skg.addNounce(skg.getAndroidNounce());
		skg.doRandom();
		HANDSHAKE_TYPE type = initNFCActivity();
		ExchangeInformation ei = new ExchangeInformation(otherID, myID, skg.getFirstKey(), skg.getSecondKey(),
				KeyEntity.DEFAULT, (byte) (type.type + 1));
		NdefMessage msg = new NdefMessage(new NdefRecord[] {
				createMimeRecord("application/vnd.com.example.android.beam", ei.getEncdodedBytes()),
				NdefRecord.createApplicationRecord("at.flack") });
		MainActivity.tempSafe.put(new String(ei.getRandomConfirmation()), ei.getKey());
		return msg;
	}

	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("USASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	protected void runNFC() {
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("advanced_key_share", false))
			return;
		nfcAdapter = NfcAdapter.getDefaultAdapter(this.getApplicationContext());
		isNfcDevice = (nfcAdapter == null) ? false : true;

		if (isNfcDevice) {
			nfcAdapter.setNdefPushMessageCallback(this, this);
		} else {
			return;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}

	public void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (rawMsgs == null)
			return;

		NdefMessage msg = (NdefMessage) rawMsgs[0];

		Intent data = new Intent();
		data.putExtra("QR_RETURNED_Bytes", msg.getRecords()[0].getPayload());
		HandshakeProcessor hsp = new HandshakeProcessor(this);
		hsp.processReceivedExchangeInformation(data, this, myID);
		reloadAfterKeyExchange();
	}

	protected abstract void reloadAfterKeyExchange();

	public abstract HANDSHAKE_TYPE initNFCActivity();

}
