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

package encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import meta.Core;


public class ThreefishCrypt {

	private static byte[] iv = new byte[]{
		68, 58, -88, -97,
		23, 32, -50, -33, 
		112, 24, -72, -13,
		102, -121, 106, 87,
		6, -77, 87, 84,
		102, 45, -41, -115,
		75, -6, -28, 97,
		6, -124, 83, -95
	};
	
	public static byte [] encryptNoPadding(byte[] key_aes, byte [] clean) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException{
		SecretKey secret = new SecretKeySpec(key_aes, "Threefish-256");
		Cipher cipher;
		try {
			if(Core.PROVIDER.equals(Core.SPONGEY_CASTLE))
				cipher = Cipher.getInstance("Threefish-256/CBC/NoPadding", new org.spongycastle.jce.provider.BouncyCastleProvider());
			else
				cipher = Cipher.getInstance("Threefish-256/CBC/NoPadding", new BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
			return cipher.doFinal(clean);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte [] decryptNoPadding(byte[] key_aes, byte [] encrypted) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		
		SecretKey secret = new SecretKeySpec(key_aes, "Threefish-256");
		Cipher cipher;
		try {
			if(Core.PROVIDER.equals(Core.SPONGEY_CASTLE))
				cipher = Cipher.getInstance("Threefish-256/CBC/NoPadding", new org.spongycastle.jce.provider.BouncyCastleProvider());
			else
				cipher = Cipher.getInstance("Threefish-256/CBC/NoPadding", new BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			
			return cipher.doFinal(encrypted);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	public static byte [] encrypt(byte[] key_aes, byte [] clean) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException{
		SecretKey secret = new SecretKeySpec(key_aes, "Threefish-256");
		Cipher cipher;
		try {
			if(Core.PROVIDER.equals(Core.SPONGEY_CASTLE))
				cipher = Cipher.getInstance("Threefish-256/CBC/PKCS7Padding", new org.spongycastle.jce.provider.BouncyCastleProvider());
			else
				cipher = Cipher.getInstance("Threefish-256/CBC/PKCS7Padding", new BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
			return cipher.doFinal(clean);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte [] decrypt(byte[] key_aes, byte [] encrypted) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		
		SecretKey secret = new SecretKeySpec(key_aes, "Threefish-256");
		Cipher cipher;
		try {
			if(Core.PROVIDER.equals(Core.SPONGEY_CASTLE))
				cipher = Cipher.getInstance("Threefish-256/CBC/PKCS5Padding", new org.spongycastle.jce.provider.BouncyCastleProvider());
			else
				cipher = Cipher.getInstance("Threefish-256/CBC/PKCS5Padding", new BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			
			return cipher.doFinal(encrypted);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
