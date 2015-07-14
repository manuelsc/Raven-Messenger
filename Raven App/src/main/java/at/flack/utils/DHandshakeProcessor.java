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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import safe.KeyEntity;
import android.app.Activity;
import android.widget.Toast;
import at.flack.exchange.KeySafe;
import encryption.Base64;
import exceptions.KeyAlreadyMappedException;
import exchange.ECDHExchange;

abstract public class DHandshakeProcessor {

	private Activity activity;
	private String primary_key;

	public DHandshakeProcessor(Activity activity, String primary_key) {
		this.activity = activity;
		this.primary_key = primary_key;
	}

	public abstract void sendHandshake(String handshaketext);

	public void processDHExchange(String ex) {
		if (KeySafe.getInstance(activity).contains(primary_key)) {
			try {
				ECDHExchange dh = new ECDHExchange();
				KeyEntity priv = KeySafe.getInstance(activity).get(primary_key);
				dh.importPrivateKey(priv.getFirstKey());
				dh.generateSharedSecret(dh.importPublicKey(Base64.decode(ex, Base64.NO_WRAP)), priv.getTimeStamp());
				KeySafe.getInstance(activity).put(primary_key, dh.getSharedSecret(), true);
				KeySafe.getInstance(activity).save();
			} catch (InvalidKeyException | IllegalStateException | InvalidAlgorithmParameterException
					| NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
				Toast.makeText(activity, "ERROR: DH Exception", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (KeyAlreadyMappedException e) {
			} catch (IOException e) {
				Toast.makeText(activity, "ERROR: KeySafe write Exception", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		} else {
			try {
				ECDHExchange dh = new ECDHExchange();
				dh.generateSharedSecret(dh.importPublicKey(Base64.decode(ex, Base64.NO_WRAP)));
				sendHandshake("%" + dh.getEncodedPublicKey());
				KeySafe.getInstance(activity).put(primary_key, dh.getSharedSecret(), true);
				KeySafe.getInstance(activity).save();
			} catch (InvalidKeyException | IllegalStateException | InvalidAlgorithmParameterException
					| NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
				Toast.makeText(activity, "ERROR: DH Exception", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (KeyAlreadyMappedException e) {
			} catch (IOException e) {
				Toast.makeText(activity, "ERROR: KeySafe write Exception", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}
}
