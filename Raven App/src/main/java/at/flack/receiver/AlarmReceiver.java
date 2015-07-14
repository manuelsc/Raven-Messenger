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

package at.flack.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import at.flack.services.FacebookService;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (sharedPrefs.getBoolean("notification_fbs", true)) {
			Intent service = new Intent(context, FacebookService.class);
			context.startService(service);
		}
	}
}