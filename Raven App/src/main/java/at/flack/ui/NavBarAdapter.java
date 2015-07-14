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

import java.util.ArrayList;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import at.flack.R;

public class NavBarAdapter extends ArrayAdapter<NavItemModel> {

	private final Context context;
	private final ArrayList<NavItemModel> modelsArrayList;

	public NavBarAdapter(Context context, ArrayList<NavItemModel> modelsArrayList) {

		super(context, R.layout.nav_drawer_item, modelsArrayList);

		this.context = context;
		this.modelsArrayList = modelsArrayList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;
		if (modelsArrayList.get(position).isHeader()) {
			rowView = inflater.inflate(R.layout.nav_drawer_header, parent, false);
			ImageView picture = (ImageView) rowView.findViewById(R.id.item_picture_profiles);
			TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
			if (modelsArrayList.get(position).getProfilePicture() != null) {
				picture.setBackgroundColor(0x00000000);
				picture.setImageBitmap(modelsArrayList.get(position).getProfilePicture());
			}
			titleView.setText(modelsArrayList.get(position).getTitle());
			rowView.setEnabled(false);
			rowView.setOnClickListener(null);
		} else {

			if (!modelsArrayList.get(position).isGroupHeader()) {
				rowView = inflater.inflate(R.layout.nav_drawer_item, parent, false);

				ImageView imgView = (ImageView) rowView.findViewById(R.id.item_picture_profiles);
				TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
				TextView counterView = (TextView) rowView.findViewById(R.id.item_counter);
				View divider = (View) rowView.findViewById(R.id.nav_divider);

				if (!modelsArrayList.get(position).hasDivider())
					divider.setVisibility(View.INVISIBLE);
				imgView.setImageResource(modelsArrayList.get(position).getIcon());
				titleView.setText(modelsArrayList.get(position).getTitle());
				if (!modelsArrayList.get(position).getCounter().equals("0")) {
					counterView.setText(modelsArrayList.get(position).getCounter());
					counterView.setEnabled(true);
					counterView.setBackgroundResource(R.drawable.rectangle);
				} else {
					counterView.setEnabled(false);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
						counterView.setBackground(null);
					else
						counterView.setBackgroundDrawable(null);

				}
			} else {
				rowView = inflater.inflate(R.layout.nav_drawer_group, parent, false);
				TextView titleView = (TextView) rowView.findViewById(R.id.header);
				titleView.setText(modelsArrayList.get(position).getTitle());
				titleView.setOnClickListener(new OnClickListener() {
					int i = 0;

					@Override
					public void onClick(View v) {
						i++;
						if (i == 10) {
							PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("in_debug_mode",
									true).apply();
							Toast.makeText(context, "Debug mode enabled", Toast.LENGTH_LONG).show();
						}
					}

				});

			}
		}

		return rowView;
	}
}