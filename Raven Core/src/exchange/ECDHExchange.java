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

package exchange;

import hash.BLAKE512;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;

import meta.Core;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;

import safe.KeyEntity;
import encryption.Base64;

public class ECDHExchange {

	
	private KeyPair keyPair;
	private KeyEntity key;
	
	public ECDHExchange() throws InvalidKeyException, IllegalStateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		switch(Core.PROVIDER){
			case Core.SPONGEY_CASTLE: {
				Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
				ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("Secp256k1");
				KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", Core.PROVIDER);
				g.initialize(ecSpec, new SecureRandom());
				keyPair = g.generateKeyPair();
				break;
			}
			case Core.BOUNCY_CASTLE: {
				Security.addProvider(new BouncyCastleProvider());
				org.bouncycastle.jce.spec.ECParameterSpec ecSpec =  org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("Secp256k1");
				KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", Core.PROVIDER);
				g.initialize(ecSpec, new SecureRandom());
				keyPair = g.generateKeyPair();
				break;
			}
		}

	}
	
	public String getEncodedPublicKey(){
		return Base64.encodeToString(getPublicKey(), Base64.NO_WRAP);
	}
	
	public byte[] getPublicKey(){
		return keyPair.getPublic().getEncoded();
	}
	
	public byte[] getPrivateKey(){
		return keyPair.getPrivate().getEncoded();
	}
	
	public String getEncodedPrivateKey(){
		return Base64.encodeToString(getPrivateKey(), Base64.NO_WRAP);
	}
	
	public KeyEntity getSharedSecret() throws IOException{
		if(key == null) throw new IOException("Key not generated, call generateSharedSecret first.");
		return key;
	}

	public void generateSharedSecret(PublicKey bPublic) throws InvalidKeyException{
		generateSharedSecret(bPublic, System.currentTimeMillis());
	}
	
	public void generateSharedSecret(PublicKey bPublic, long creationTime) throws InvalidKeyException{
			KeyAgreement aKeyAgree = null;
			try {
				aKeyAgree = KeyAgreement.getInstance("ECDH", Core.PROVIDER);
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				e.printStackTrace();
				return;
			}
			aKeyAgree.init(keyPair.getPrivate());
		    aKeyAgree.doPhase(bPublic, true);

		    byte[] aSecret = aKeyAgree.generateSecret();

		    key = new KeyEntity(new BLAKE512().digest(aSecret), creationTime, KeyEntity.DEFAULT);
	}
	
	public void importPrivateKey(byte [] b) throws InvalidKeySpecException{
		PKCS8EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(
				b);
	    KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("ECDH", Core.PROVIDER);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		keyPair = new KeyPair(keyPair.getPublic(), keyFactory.generatePrivate(publicKeySpec));
	}
	
	public PublicKey importPublicKey(byte [] b) throws InvalidKeySpecException{
	    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				b);
	    KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("ECDH", Core.PROVIDER);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	    return keyFactory.generatePublic(publicKeySpec);
	}
	
}
