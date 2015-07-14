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

package at.flack.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.concurrent.ExecutionException;

import at.flack.MainActivity;
import at.flack.R;

public class QuickPrefsActivity extends AppCompatActivity {

	public static int fragment;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_prefs);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		fragment = 0;
		getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();

	}

	@Override
	public void onBackPressed() {
		if (QuickPrefsActivity.fragment == 0) {
			super.onBackPressed();
		} else {
			getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (QuickPrefsActivity.fragment == 0) {
				NavUtils.navigateUpFromSameTask(this);
				return true;
			} else {
				getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();
				return true;
			}
		}
		return false;
	}







}
