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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import safe.KeyEntity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import at.flack.R;
import at.flack.exchange.KeySafe;

public class DebugActivity extends ActionBarActivity {

	private ListView keysafe_list;
	private ArrayList<String> keysafe = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		this.setTitle("Debug Activity");

		keysafe_list = (ListView) this.findViewById(R.id.keysafe_list);

		update();
		keysafe_list.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
				KeySafe.getInstance(this).remove(keysafe.get(pos).split("=")[0]);
				KeySafe.getInstance(this).save();
				Toast.makeText(DebugActivity.this, "Removed from Keysafe", Toast.LENGTH_SHORT).show();
				update();
				return true;
			}
		});

	}

	public void update() {
		HashMap<String, KeyEntity> map = KeySafe.getInstance(this).getMap();
		keysafe = new ArrayList<String>();
		if (map != null)
			for (Map.Entry<String, KeyEntity> entry : map.entrySet()) {
				keysafe.add(entry.getKey());
			}

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, keysafe);
		keysafe_list.setAdapter(arrayAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
