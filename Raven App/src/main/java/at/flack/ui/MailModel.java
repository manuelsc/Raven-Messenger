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

public class MailModel {

	private Bitmap picture;
	private String message;
	private String date;
	private Bitmap preview;
	private int safeLevel;
	private String name;
	private boolean header;
	private boolean title;
	private boolean useHTML;

	/**
	 * Used for E-Mail title
	 * 
	 * @param text
	 */
	public MailModel(String text) {
		this.message = text;
		this.title = true;
	}

	/**
	 * Used for E-Mail header
	 * 
	 * @param from
	 * @param to
	 * @param date
	 * @param picture
	 */
	public MailModel(String from, String to, String date, Bitmap picture, int safeLevel) {
		this.picture = picture;
		this.message = from;
		this.name = to;
		this.date = date;
		this.header = true;
		this.safeLevel = safeLevel;
	}

	/**
	 * Used for E-Mail body text
	 * 
	 * @param name
	 * @param picture
	 * @param message
	 * @param date
	 * @param safeLevel
	 */
	public MailModel(String message, boolean useHTML) {
		this.message = message;
		this.useHTML = useHTML;
	}

	public boolean useHTML() {
		return useHTML;
	}

	public boolean isHeader() {
		return header;
	}

	public void setHeader(boolean header) {
		this.header = header;
	}

	public boolean isTitle() {
		return title;
	}

	public void setTitle(boolean title) {
		this.title = title;
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

}