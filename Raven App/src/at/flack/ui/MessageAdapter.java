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
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.flack.R;

public class MessageAdapter extends ArrayAdapter<MessageModel> {

	private final Context context;
	private final ArrayList<MessageModel> modelsArrayList;

	public MessageAdapter(Context context, ArrayList<MessageModel> modelsArrayList) {

		super(context, R.layout.contact_item, modelsArrayList);

		this.context = context;
		this.modelsArrayList = modelsArrayList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;
		if (modelsArrayList.get(position).isNotification()) {
			rowView = inflater.inflate(R.layout.message_handshake, parent, false);
			TextView titleView = (TextView) rowView.findViewById(R.id.comment);
			titleView.setText(modelsArrayList.get(position).getMessage());
		} else if (!modelsArrayList.get(position).isImage()) { // Text Message
			if (modelsArrayList.get(position).isNotMe())
				rowView = inflater.inflate(R.layout.message_person, parent, false);
			else
				rowView = inflater.inflate(R.layout.message_me, parent, false);

			ImageView imgView = (ImageView) rowView.findViewById(R.id.profile_picture);
			TextView titleView = (TextView) rowView.findViewById(R.id.comment);
			TextView dateView = (TextView) rowView.findViewById(R.id.date_comment);
			ImageView safeIcon = (ImageView) rowView.findViewById(R.id.safeicon);

			if (modelsArrayList.get(position).getPicture() != null)
				imgView.setImageBitmap(RoundedImageView.getCroppedBitmap(modelsArrayList.get(position).getPicture(),
						300));
			else {
				ShapeDrawable drawable = new ShapeDrawable(new OvalShape());

				if (modelsArrayList.get(position).getName() == null)
					drawable.getPaint().setColor(ContactAdapter.colors[0]);
				else
					drawable.getPaint().setColor(
							ContactAdapter.colors[Math.abs(ContactAdapter.betterHashCode(modelsArrayList.get(position)
									.getName()))
									% ContactAdapter.colors.length]);

				int sdk = android.os.Build.VERSION.SDK_INT;
				if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					imgView.setBackgroundDrawable(drawable);
				} else {
					imgView.setBackground(drawable);
				}
			}
			dateView.setText(modelsArrayList.get(position).getDate());
			if (modelsArrayList.get(position) != null && modelsArrayList.get(position).getMessage() != null)
				titleView.setText(modelsArrayList.get(position).getMessage());
			if (modelsArrayList.get(position).getSafe() == 0)
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_encrypted));
			if (modelsArrayList.get(position).getSafe() == 2)
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_error_encrypt));

		} else { // Image
			if (modelsArrayList.get(position).isNotMe())
				rowView = inflater.inflate(R.layout.message_image_person, parent, false);
			else
				rowView = inflater.inflate(R.layout.message_image_me, parent, false);

			ImageView imgView = (ImageView) rowView.findViewById(R.id.profile_picture);
			ImageView preview = (ImageView) rowView.findViewById(R.id.image);
			TextView dateView = (TextView) rowView.findViewById(R.id.date_comment);
			ImageView safeIcon = (ImageView) rowView.findViewById(R.id.safeicon);
			if (modelsArrayList.get(position).getPicture() != null)
				imgView.setImageBitmap(RoundedImageView.getCroppedBitmap(modelsArrayList.get(position).getPicture(),
						300));
			else {
				ShapeDrawable drawable = new ShapeDrawable(new OvalShape());

				if (modelsArrayList.get(position).getName() == null)
					drawable.getPaint().setColor(ContactAdapter.colors[0]);
				else
					drawable.getPaint().setColor(
							ContactAdapter.colors[Math.abs(ContactAdapter.betterHashCode(modelsArrayList.get(position)
									.getName()))
									% ContactAdapter.colors.length]);
				int sdk = android.os.Build.VERSION.SDK_INT;
				if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					imgView.setBackgroundDrawable(drawable);
				} else {
					imgView.setBackground(drawable);
				}

			}
			dateView.setText(modelsArrayList.get(position).getDate());
			if (modelsArrayList.get(position).getPreview() != null) {
				preview.setImageBitmap(modelsArrayList.get(position).getPreview());
				preview.setPadding(0, 0, 0, 0);
				if (modelsArrayList.get(position).isNotMe())
					preview.setBackgroundColor(0xffffffff);
				else
					preview.setBackgroundColor(0xff1985D7);
			}
			if (modelsArrayList.get(position).getSafe() == 0)
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_encrypted));
			if (modelsArrayList.get(position).getSafe() == 2)
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_error_encrypt));

		}
		return rowView;
	}

	public void refill(ArrayList<MessageModel> mod) {
		if (mod.size() == 0)
			return;
		this.modelsArrayList.clear();
		this.modelsArrayList.addAll(mod);
		this.notifyDataSetChanged();
	}
}