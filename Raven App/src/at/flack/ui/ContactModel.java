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

package at.flack.ui;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class ContactModel implements Parcelable {

	private Bitmap picture;
	private String title;
	private String last_message;
	private String date;
	private int encrypted;
	private boolean unread;
	private boolean online;
	private boolean mobile;
	private String fromMail;
	private String toMail;
	private long smsID = 0;
	private boolean mail;
	private String fromName;
	private boolean drawOnline = true;

	public ContactModel(Bitmap picture, String title, String last_message, String date, boolean unread) {
		super();
		this.picture = picture;
		this.title = title;
		this.last_message = last_message;
		this.date = date;
		this.unread = unread;
		encrypted = View.INVISIBLE;
	}

	public ContactModel(Bitmap picture, String title, String last_message, String date, boolean unread, boolean online,
			boolean mobile) {
		super();
		this.picture = picture;
		this.title = title;
		this.last_message = last_message;
		this.date = date;
		this.unread = unread;
		this.mobile = mobile;
		this.online = online;
		encrypted = View.INVISIBLE;
	}

	// MAIL
	public ContactModel(Bitmap picture, String title, String last_message, String date, boolean unread, String from,
			String to, String fromName) {
		super();
		this.picture = picture;
		this.title = title;
		this.last_message = last_message;
		this.date = date;
		this.unread = unread;
		this.fromMail = from;
		this.toMail = to;
		this.fromName = fromName != null ? fromName : "";
		this.mail = true;
		encrypted = View.INVISIBLE;
	}

	public ContactModel drawOnline(boolean b) {
		drawOnline = b;
		return this;
	}

	public boolean drawOnline() {
		return drawOnline;
	}

	public String getFromName() {
		return fromName;
	}

	public String getFromMail() {
		return fromMail;
	}

	public void setFromMail(String fromMail) {
		this.fromMail = fromMail;
	}

	public String getToMail() {
		return toMail;
	}

	public void setToMail(String toMail) {
		this.toMail = toMail;
	}

	public boolean isMail() {
		return mail;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isMobile() {
		return mobile;
	}

	public void setMobile(boolean mobile) {
		this.mobile = mobile;
	}

	public boolean isUnread() {
		return unread;
	}

	public void setUnread(boolean unread) {
		this.unread = unread;
	}

	public int getEncrypted() {
		return encrypted;
	}

	public void setEncrypted(int encrypted) {
		this.encrypted = encrypted;
	}

	public Bitmap getPicture() {
		return picture;
	}

	public String getTitle() {
		return title;
	}

	public String getLastMessage() {
		return last_message;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setLastMessage(String msg) {
		this.last_message = msg;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setPicture(Bitmap picture) {
		this.picture = picture;
	}

	public String toString() {
		return title;
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public int hashCode() {
		return getTitle().hashCode() ^ getLastMessage().hashCode() ^ getDate().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return ((ContactModel) o).getTitle().equals(getTitle())
				&& ((ContactModel) o).getLastMessage().equals(getLastMessage())
				&& ((ContactModel) o).getDate().equals(getDate());
	}

	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeString(title);
		dest.writeString(last_message);
		dest.writeString(date);
		dest.writeParcelable(picture, parcelableFlags);
	}

	public static final Creator<ContactModel> CREATOR = new Creator<ContactModel>() {
		public ContactModel createFromParcel(Parcel source) {
			return new ContactModel(source);
		}

		public ContactModel[] newArray(int size) {
			return new ContactModel[size];
		}
	};

	private ContactModel(Parcel source) {
		title = source.readString();
		last_message = source.readString();
		date = source.readString();
		picture = source.readParcelable(getClass().getClassLoader());
	}

	public void setThreadID(long id) {
		this.smsID = id;
	}

	public long getThreadID() {
		return smsID;
	}

}
