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
import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import at.flack.R;

public class ContactAdapter extends ArrayAdapter<ContactModel> implements Filterable {

	private final Context context;
	private ArrayList<ContactModel> modelsArrayList;
	private ArrayList<ContactModel> filteredList;
	private ContactFilter contactFilter;
	private int page;

	public static int[] colors = new int[] { 
			0xFFF44336, // Red
			0xFFE91E63, // Pink
			0xFF9C27B0, // Purple
			0xFF3F51B5, // Indigo
			0xFF2196F3, // Blue
			0xFF009688, // Teal
			0xFF4CAF50, // Green
			0xFFFF9800 // Orange
	};

	// Only used for background color generation (returned code % colors.length
	// => index for color array)
	public static int betterHashCode(String s) {
		if (s == null || s.length() <= 1)
			return 0;
		byte[] bytes = s.getBytes();
		int erg = bytes[0];
		for (int i = 1; i < bytes.length; i++)
			erg ^= bytes[i];
		return erg;
	}

	public ContactAdapter(Context context, ArrayList<ContactModel> modelsArrayList, int page) {

		super(context, R.layout.contact_item, modelsArrayList);

		this.context = context;
		this.modelsArrayList = modelsArrayList;
		this.filteredList = modelsArrayList;
		this.page = page;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;
		if (page == 0)
			rowView = inflater.inflate(R.layout.contact_item, parent, false);
		if (page == 1)
			rowView = inflater.inflate(R.layout.fb_contact_item, parent, false);

		ImageView imgView = (ImageView) rowView.findViewById(R.id.profile_picture);
		ImageView encryptedIcon = (ImageView) rowView.findViewById(R.id.encryptedIcon);
		TextView titleView = (TextView) rowView.findViewById(R.id.textTitle);
		TextView lastMessageView = (TextView) rowView.findViewById(R.id.textLastMessage);
		TextView dateView = (TextView) rowView.findViewById(R.id.textDate);
		ImageView online_handy;

		if (page == 1) {
			online_handy = (ImageView) rowView.findViewById(R.id.online_handy);
			if (getItem(position).isOnline())
				online_handy.setImageResource(R.drawable.online);
			if (getItem(position).isMobile())
				online_handy.setImageResource(R.drawable.mobile);
			if (!getItem(position).drawOnline())
				online_handy.setVisibility(View.INVISIBLE);
		}

		if (getItem(position).getPicture() != null)
			imgView.setImageBitmap(RoundedImageView.getCroppedBitmap(getItem(position).getPicture(), 300));
		else {
			ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
			if (getItem(position).isMail())
				drawable.getPaint().setColor(
						ContactAdapter.colors[Math.abs(betterHashCode(getItem(position).getFromName()))
								% ContactAdapter.colors.length]);
			else
				drawable.getPaint().setColor(
						ContactAdapter.colors[Math.abs(betterHashCode(getItem(position).getTitle()))
								% ContactAdapter.colors.length]);

			int sdk = android.os.Build.VERSION.SDK_INT;
			if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				imgView.setBackgroundDrawable(drawable);
			} else {
				imgView.setBackground(drawable);
			}

		}
		if (getItem(position).isUnread()) {
			lastMessageView.setTypeface(null, Typeface.BOLD);
			dateView.setTypeface(null, Typeface.BOLD);
		}
		encryptedIcon.setVisibility(getItem(position).getEncrypted());

		if (getItem(position).isMail())
			titleView.setText(!getItem(position).getFromName().equals("") ? getItem(position).getFromName() : getItem(
					position).getFromMail());
		else
			titleView.setText(getItem(position).getTitle());
		if (getItem(position).isMail())
			lastMessageView.setText(getItem(position).getTitle());
		else if (getItem(position).getLastMessage() != null)
			lastMessageView.setText(getItem(position).getLastMessage());
		if (page == 1)
			dateView.setText(shortDate(getItem(position).getDate()));
		else
			dateView.setText(getItem(position).getDate());
		if (getItem(position).getEncrypted() == View.VISIBLE) {
			titleView.setTextColor(Color.parseColor("#04A004"));
		}

		return rowView;
	}

	private String shortDate(String date) {
		if (date == null)
			return "";
		String erg = date;
		try {
			String[] split = date.split(" ");
			if (split.length == 1)
				return date;
			erg = erg.replace(split[split.length - 1], split[split.length - 1].substring(0, 3));
		} catch (Exception e) {
			return date;
		}
		return erg;
	}

	public String toString() {
		return modelsArrayList.toString();
	}

	public void refill(ArrayList<ContactModel> mod, int page) {
		if (mod.size() == 0)
			return;
		this.page = page;
		this.modelsArrayList.clear();
		this.modelsArrayList.addAll(mod);
		this.filteredList.clear();
		this.filteredList.addAll(mod);
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return filteredList.size();
	}

	@Override
	public ContactModel getItem(int i) {
		return filteredList.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public Filter getFilter() {
		if (contactFilter == null) {
			contactFilter = new ContactFilter();
		}
		return contactFilter;
	}

	private class ContactFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults filterResults = new FilterResults();
			if (constraint != null && constraint.length() > 0) {
				ArrayList<ContactModel> tempList = new ArrayList<ContactModel>();

				for (ContactModel contact : modelsArrayList) {
					if (contact.getTitle().toLowerCase(Locale.getDefault()).contains(
							constraint.toString().toLowerCase(Locale.getDefault()))
							|| contact.getLastMessage().toLowerCase(Locale.getDefault()).contains(
									constraint.toString().toLowerCase(Locale.getDefault()))) {
						tempList.add(contact);
					}
				}

				filterResults.count = tempList.size();
				filterResults.values = tempList;
			} else {
				filterResults.count = modelsArrayList.size();
				filterResults.values = modelsArrayList;
			}

			return filterResults;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			filteredList = (ArrayList<ContactModel>) results.values;
			notifyDataSetChanged();
		}
	}

}