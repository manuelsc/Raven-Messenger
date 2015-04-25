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

package at.flack.utils;

import java.io.IOException;

import safe.KeyEntity;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import api.FacebookContact;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.activity.NewMailActivity;
import at.flack.dialogs.AlreadyMappedDialog;
import at.flack.exchange.KeySafe;
import at.flack.sms.SMSTool;
import encryption.Base64;
import exceptions.KeyAlreadyMappedException;
import exceptions.WrongByteFormatException;
import exchange.ExchangeInformation;

public class HandshakeProcessor {

	private Activity ac;

	public HandshakeProcessor(Activity ac) {
		this.ac = ac;
	}

	public void processReceivedRandomConfirmationCode(Intent data) {
		if (MainActivity.tempSafe.containsKey(data.getStringExtra("ADD_NEW_KEY_CONFIRMATIONCODE"))) {
			KeyEntity key = MainActivity.tempSafe.get(data.getStringExtra("ADD_NEW_KEY_CONFIRMATIONCODE"));
			try {
				KeySafe.getInstance(ac).put(data.getStringExtra("ADD_NEW_KEY_TELEPHONNUMBER"), key, false);
				KeySafe.getInstance(ac).save();
				MainActivity.tempSafe.remove(data.getStringExtra("ADD_NEW_KEY_CONFIRMATIONCODE"));
			} catch (KeyAlreadyMappedException e) {
			}
		}
	}

	public void processReceivedExchangeInformation(Intent data, Activity activity, String myid) {
		ExchangeInformation ei = null;
		try {
			ei = new ExchangeInformation(data.getByteArrayExtra("QR_RETURNED_Bytes"));
			if (!ei.getMyID().equals(myid)) {

			}
			if (ei.getVersion() == 0x01) {
				try {
					KeySafe.getInstance(ac).put(ei, false);
				} catch (KeyAlreadyMappedException e) {
					showAlreadyMappedDialog(ei, new String(ei.getRandomConfirmation()), 0, activity);
					return;
				}
				finishUpSMS(ei);

			} else if (ei.getVersion() == 0x02) {
				try {
					KeySafe.getInstance(ac).put(ei, false);
				} catch (KeyAlreadyMappedException e) {
					showAlreadyMappedDialog(ei, new String(ei.getRandomConfirmation()), 1, activity);
					return;
				}
				finishUpFb(ei);
			} else if (ei.getVersion() == 0x03) {
				try {
					KeySafe.getInstance(ac).put(ei, false);
				} catch (KeyAlreadyMappedException e) {
					showAlreadyMappedDialog(ei, new String(ei.getRandomConfirmation()), 2, activity);
					return;
				}
				finishUpMail(ei, activity);
			}

		} catch (WrongByteFormatException e1) {
			Toast.makeText(ac, "Failed - Wrong Byte Format!", Toast.LENGTH_SHORT).show();
			e1.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			Toast.makeText(ac, "Failed - Array Index Byte Format!", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

	}

	private void finishUpSMS(ExchangeInformation ei) {
		KeySafe.getInstance(ac).save();
		new SMSTool(ac).sendConfirmationCode(ei.getPhoneNumber(), ei.getRandomConfirmation());
	}

	private void finishUpMail(ExchangeInformation ei, Activity activity) {
		KeySafe.getInstance(activity).save();
		new NewMailActivity().sendMail(ei.getID(), activity.getResources().getString(
				R.string.mail_activity_handshake_mail_subject), "%"
				+ Base64.encodeToString(ei.getRandomConfirmation(), Base64.NO_WRAP));
	}

	private void finishUpFb(ExchangeInformation ei) {
		KeySafe.getInstance(ac).save();

		try {
			final FacebookContact temp = findFacebookContact(ei.getID());
			final byte[] confirm = ei.getRandomConfirmation();
			if (temp != null) {
				new Thread() {
					public void run() {
						try {
							MainActivity.fb_api.sendMessage("%" + Base64.encodeToString(confirm, Base64.NO_WRAP), temp);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}

			else
				throw new IOException("Cant resolve Contact");
		} catch (IOException e) {
			Toast.makeText(ac, "Error: Cant send confirmation | caused by: " + e.toString(), Toast.LENGTH_SHORT).show();
		}
	}

	public void showAlreadyMappedDialog(final ExchangeInformation ei, final String rcc, final int i,
			final Activity activity) {
		AlreadyMappedDialog a = new AlreadyMappedDialog() {
			@Override
			public void onYes() {
				try {
					KeySafe.getInstance(activity).put(ei, true);
				} catch (KeyAlreadyMappedException e) {
				}

				KeySafe.getInstance(activity).save();

				MainActivity.tempSafe.remove(rcc);

				if (i == 0) // SMS
					finishUpSMS(ei);
				if (i == 1) // Fb
					finishUpFb(ei);
				if (i == 2)
					finishUpMail(ei, activity);
			}
		};

		a.show(ac.getFragmentManager(), "tag");
	}

	private FacebookContact findFacebookContact(String id) {
		for (FacebookContact fc : MainActivity.getFbcontacts())
			if (fc.getTid().equals(id))
				return fc;
		return null;
	}

}
