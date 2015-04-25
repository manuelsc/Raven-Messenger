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

package at.flack.sms;

import java.util.ArrayList;

import safe.KeyEntity;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Base64;
import android.widget.Toast;
import encryption.Message;

public class SMSTool {

	private Activity activity;
	private SmsManager sms = SmsManager.getDefault();
	private boolean success;

	public SMSTool(Activity activity) {
		this.activity = activity;
	}

	public static ArrayList<String> divideMessage(String text) {
		ArrayList<String> erg = new ArrayList<String>();
		int max = (int) (text.length() / 153) + 1;
		for (int i = 0; i < max; i++) {
			if (i < max - 1)
				erg.add(text.substring(i * 152, i * 152 + 152) + "$");
			else
				erg.add(text.substring(i * 152, text.length()));
		}
		return erg;
	}

	public boolean sendEncryptedSMS(String phoneNumber, String message, KeyEntity key) {
		try {
			if (key != null) {
				Message mes = new Message(message);
				return sendSMS(phoneNumber, mes.encryptedMessage(key), true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean sendSMS(String phoneNumber, String message) {
		return sendSMS(phoneNumber, message, false);
	}

	public boolean sendSMS(String phoneNumber, String message, boolean specialDivide) {
		if (phoneNumber == null || phoneNumber.length() <= 3) {
			Toast.makeText(activity.getBaseContext(), "ERROR: Invalid phone number!", Toast.LENGTH_LONG).show();
			return false;
		}
		String SENT = "SMS_SENT";

		ArrayList<String> parts = null;

		if (specialDivide)
			parts = divideMessage(message);
		else
			parts = sms.divideMessage(message);

		ArrayList<PendingIntent> sendIntents = new ArrayList<PendingIntent>();

		for (int i = 0; i < parts.size(); i++) {
			sendIntents.add(PendingIntent.getBroadcast(activity, 0, new Intent(SENT), 0));
		}

		sms.sendMultipartTextMessage(phoneNumber, null, parts, sendIntents, null);

		// Save SMS if default app
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
				&& Telephony.Sms.getDefaultSmsPackage(activity).equals(activity.getPackageName())) {
			ContentValues values = new ContentValues();
			values.put("address", phoneNumber);
			values.put("body", message);
			activity.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
		}

		return success;

	}

	public void sendConfirmationCode(String phoneNumber, byte[] confirmationCode) {
		sendSMS(phoneNumber, "%" + Base64.encodeToString(confirmationCode, 0, confirmationCode.length, Base64.NO_WRAP));
	}
}
