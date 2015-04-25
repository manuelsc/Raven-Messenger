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

public class MessageModel {

	private Bitmap picture;
	private String message;
	private String date;
	private boolean isNotMe;
	private Bitmap preview;
	private int safeLevel; // 0 = encrypted, 1 = not encrypted, 2 = cant encrypt
	private String name;
	private boolean isImage;
	private boolean notification;

	public MessageModel(String text, boolean notification) {
		this.notification = notification;
		this.message = text;
	}

	public MessageModel(String name, Bitmap picture, String message, String date, boolean isItMe, int safeLevel) {
		super();
		this.picture = picture;
		this.message = message;
		this.date = date;
		this.isNotMe = isItMe;
		this.safeLevel = safeLevel;
		this.name = name;
	}

	public MessageModel(String name, Bitmap picture, String image, Bitmap preview, String date, boolean isItMe,
			int safeLevel) {
		super();
		this.picture = picture;
		this.message = image;
		this.preview = preview;
		this.date = date;
		this.isNotMe = isItMe;
		this.safeLevel = safeLevel;
		this.name = name;
		isImage = true;
	}

	public boolean isNotification() {
		return notification;
	}

	public boolean isImage() {
		return isImage;
	}

	public Bitmap getPreview() {
		return preview;
	}

	public void setPreview(Bitmap preview) {
		this.preview = preview;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setIsSafe(int safe) {
		this.safeLevel = safe;
	}

	public int getSafe() {
		return safeLevel;
	}

	public boolean isNotMe() {
		return isNotMe;
	}

	public void setIsNotMe(boolean isItMe) {
		this.isNotMe = isItMe;
	}

	public Bitmap getPicture() {
		return picture;
	}

	public String getMessage() {
		return message;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setMessage(String msg) {
		this.message = msg;
	}

	public void setPicture(Bitmap picture) {
		this.picture = picture;
	}

	public String toString() {
		return message;
	}

	public int describeContents() {
		return 0;
	}

}