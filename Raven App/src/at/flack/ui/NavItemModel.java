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

public class NavItemModel {

	private int icon;
	private String title;
	private String counter;
	private boolean divider;
	private boolean isGroupHeader = false;

	private Bitmap profile_picture;
	private boolean isHeader = false;;

	public NavItemModel(String title) {
		this(-1, title, null, true);
		isGroupHeader = true;
	}

	public NavItemModel(int icon, String title, String counter, boolean divider) {
		super();
		this.icon = icon;
		this.title = title;
		this.counter = counter;
		this.divider = divider;
	}

	public NavItemModel(Bitmap profile_picture, String title) {
		this.profile_picture = profile_picture;
		this.title = title;
		this.isHeader = true;
	}

	public void setDivider(boolean b) {
		this.divider = b;
	}

	public boolean hasDivider() {
		return divider;
	}

	public boolean isHeader() {
		return isHeader;
	}

	public Bitmap getProfilePicture() {
		return profile_picture;
	}

	public void setProfilePicture(Bitmap profile_picture) {
		this.profile_picture = profile_picture;
	}

	public boolean isGroupHeader() {
		return isGroupHeader;
	}

	public int getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public String getCounter() {
		return counter;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

}
