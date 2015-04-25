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

package mail;

import hash.BLAKE512;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MailProfile implements Serializable {

	private static final long serialVersionUID = 6739903367018949913L;
	private String smtpHost;
	private String pop3Host;
	private String imapHost;
	private String username;
	private String password;
	private String email;
	private int smtpPort;
	private int Pop3Port;
	private int ImapPort;
	
	private boolean active = false;
	private ArrayList<Email> receivedMessages;
	private ArrayList<Email> sentMessages;
	private TreeMap<String, String> keyMap = new TreeMap<String, String>();
	
	static byte [] temp = {125, 13, 42,  43, 55, 108, 66, 77, 88, 99, 42, 15, 9,  35,  59,  26 };
	private static IvParameterSpec ips = new IvParameterSpec(temp);
	
	public MailProfile(String smtpHost, int smtpPort, String pop3Host, int pop3Port, String imapHost, int imapPort,
			String username, String password, String email) {
		this.smtpHost = smtpHost;
		this.pop3Host = pop3Host;
		this.imapHost = imapHost;
		this.username = username;
		this.password = password;
		this.email = email;
		this.receivedMessages = new ArrayList<Email>();
		this.sentMessages = new ArrayList<Email>();
		this.smtpPort = smtpPort;
		this.ImapPort = imapPort;
		this.Pop3Port = pop3Port;
	}

	public int getPop3Port() {
		return Pop3Port;
	}

	public void setPop3Port(int pop3Port) {
		Pop3Port = pop3Port;
	}

	public int getImapPort() {
		return ImapPort;
	}

	public void setImapPort(int imapPort) {
		ImapPort = imapPort;
	}

	public int getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}
	
	public MailAuthenticator getPasswordAuthentication() {
		return new MailAuthenticator(username, password);
	}

	public void clearSentMessages(){
		sentMessages.removeAll(sentMessages);
	}
	
	public void clearReceivedMessages(){
		receivedMessages.removeAll(receivedMessages);
	}
	
	public void addReceivedMessage(Email email) {
		receivedMessages.add(email);
	}

	public ArrayList<Email> getReceivedMessages() {
		return this.receivedMessages;
	}

	public ArrayList<Email> getSentMessages() {
		return this.sentMessages;
	}

	public void addSentMessage(Email email) {
		sentMessages.add(email);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getPop3Host() {
		return pop3Host;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public String getImapHost() {
		return imapHost;
	}

	public boolean getState() {
		return active;
	}

	public void setState(boolean state) {
		this.active = state;
	}

	public String getEmail() {
		return email;
	}

	/**
	 * Schreibt alle konfigurierten Profile des Benutzers in eine vordefinierte
	 * Zieldatei am Dateisystem.
	 * 
	 * @param profiles
	 *            - Eine ArrayList des Types "MailProfile", die alle
	 *            konfigurierten Profile des Benutzers enth�lt.
	 */
	public static void writeToFile(ArrayList<MailProfile> profiles,
			String secretPassword) {
		try {
			Cipher cipher = null;
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			System.out.println(ips);
			cipher.init(Cipher.ENCRYPT_MODE, fromStringToAESkey(secretPassword), ips);
			SealedObject sealedObject = null;
			sealedObject = new SealedObject(profiles, cipher);
			CipherOutputStream cipherOutputStream = null;
			cipherOutputStream = new CipherOutputStream(
					new BufferedOutputStream(new FileOutputStream(
							"E:\\My Documents\\Diplomarbeit\\profiles.raven")),
					cipher);
			ObjectOutputStream outputStream = null;
			outputStream = new ObjectOutputStream(cipherOutputStream);
			outputStream.writeObject(sealedObject);
			outputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	
	public static SecretKey fromStringToAESkey(String s) {
		return new SecretKeySpec(new BLAKE512().digest(s.getBytes()), "AES");
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<MailProfile> loadFromFile(File data, String secretPassword) {
		ArrayList<MailProfile> userList = null;
		System.out.println(ips);
		try{
			Cipher cipher = null;
		cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		// Code to write your object to file
		cipher.init(Cipher.DECRYPT_MODE, fromStringToAESkey(secretPassword), ips);
		CipherInputStream cipherInputStream = null;
		cipherInputStream = new CipherInputStream(new BufferedInputStream(
				new FileInputStream(data)), cipher);

		ObjectInputStream inputStream = null;
		inputStream = new ObjectInputStream(cipherInputStream);
		SealedObject sealedObject = null;
		sealedObject = (SealedObject) inputStream.readObject();
		userList = (ArrayList<MailProfile>) sealedObject.getObject(cipher);
		inputStream.close();
		
		
		} catch(Exception ex){
			ex.printStackTrace();
		}
		
		return userList;
		
	}

	@Override
	public boolean equals(Object obj) {
		if ((MailProfile) obj == null) {
			return false;
		}

		if (this.email.equals(((MailProfile) obj).email)) {
			return true;
		}
		return false;
	}

	public void addKey(String contact, String key) {
		if (keyMap.containsKey(contact)) {
			return;
		}
		keyMap.put(contact, key);
	}
	
	public Set<String> getKeyMapEntries(){
		return keyMap.keySet();
	}
}
