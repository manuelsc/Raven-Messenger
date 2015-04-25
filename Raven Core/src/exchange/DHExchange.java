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

import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
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
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

public class DHExchange {

	/**
	 * Use 1536 for Fast Prime Generation (less secure) or 2048 for slow prime generation (secure)
	 * @param strength
	 * @throws InvalidKeyException
	 * @throws IllegalStateException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 * @throws InvalidParameterSpecException
	 */
	public DHExchange(int strength) throws InvalidKeyException, IllegalStateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidParameterSpecException {
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		
		AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH", "SC");
		paramGen.init(strength); // number of bits
		AlgorithmParameters params = paramGen.generateParameters();
		DHParameterSpec dhSpec = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);

		BigInteger p = dhSpec.getP();
		BigInteger g = dhSpec.getG();
		
	    DHParameterSpec dhParams = new DHParameterSpec(p, g);
	    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "SC");
	    keyGen.initialize(dhParams, new SecureRandom());
	    KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH", "SC");
	    KeyPair aPair = keyGen.generateKeyPair();
	    KeyAgreement bKeyAgree = KeyAgreement.getInstance("DH", "SC");
	    KeyPair bPair = keyGen.generateKeyPair();

	    aKeyAgree.init(aPair.getPrivate());
	    bKeyAgree.init(bPair.getPrivate());
	    
	    aKeyAgree.doPhase(bPair.getPublic(), true);
	    bKeyAgree.doPhase(aPair.getPublic(), true);
	}
	
	public PublicKey importPublicKey(byte [] b) throws InvalidKeySpecException{
	    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				b);
	    KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("DH", "SC");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	    return keyFactory.generatePublic(publicKeySpec);
	}
	
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidParameterSpecException, IllegalStateException, InvalidKeySpecException {
		new DHExchange(1536);
	}
}
