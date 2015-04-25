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

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;

public class UpdateProcessor {

	private Activity activity;
	private SharedPreferences prefs;

	public UpdateProcessor(Activity activity, SharedPreferences prefs) {
		this.activity = activity;
		this.prefs = prefs;
	}

	public void updateMe(String runningVersion, String currentVersion) {
		runningVersion = runningVersion.replaceAll("[.]", "");
		try {
			int versioncode = Integer.parseInt(runningVersion);
			if (versioncode < 1503241) {
				new File(activity.getFilesDir(), "keysafe.dat").delete();
				new File(activity.getFilesDir(), "salt.dat").delete();
			}

			prefs.edit().putString("version", currentVersion).apply();
		} catch (Exception e) {
		}

	}

}
