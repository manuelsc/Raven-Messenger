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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.flack.R;

public class MailAdapter extends ArrayAdapter<MailModel> {

	private final Context context;
	private final ArrayList<MailModel> modelsArrayList;

	public MailAdapter(Context context, ArrayList<MailModel> modelsArrayList) {

		super(context, R.layout.contact_item, modelsArrayList);

		this.context = context;
		this.modelsArrayList = modelsArrayList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;
		if (modelsArrayList.get(position).isTitle()) {
			rowView = inflater.inflate(R.layout.mail_title_item, parent, false);
			TextView titleView = (TextView) rowView.findViewById(R.id.textTitle);
			titleView.setText(modelsArrayList.get(position).getMessage());
		} else if (modelsArrayList.get(position).isHeader()) {
			rowView = inflater.inflate(R.layout.mail_head_item, parent, false);

			ImageView imgView = (ImageView) rowView.findViewById(R.id.profile_picture);
			TextView titleView = (TextView) rowView.findViewById(R.id.textTitle);
			TextView dateView = (TextView) rowView.findViewById(R.id.textDate);
			TextView toMail = (TextView) rowView.findViewById(R.id.to_mail);
			ImageView safeIcon = (ImageView) rowView.findViewById(R.id.encryptedIcon);

			if (modelsArrayList.get(position).getPicture() != null)
				imgView.setImageBitmap(RoundedImageView.getCroppedBitmap(modelsArrayList.get(position).getPicture(),
						300));
			else {
				ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
				drawable.getPaint().setColor(
						ContactAdapter.colors[Math.abs(ContactAdapter.betterHashCode(modelsArrayList.get(position)
								.getMessage()))
								% ContactAdapter.colors.length]);

				int sdk = android.os.Build.VERSION.SDK_INT;
				if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					imgView.setBackgroundDrawable(drawable);
				} else {
					imgView.setBackground(drawable);
				}

			}
			dateView.setText(modelsArrayList.get(position).getDate());
			titleView.setText(modelsArrayList.get(position).getMessage());
			toMail.setText(modelsArrayList.get(position).getName());
			if (modelsArrayList.get(position).getSafe() == 0) {
				titleView.setTextColor(0xFF03b303);
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_encrypted));
			}
			if (modelsArrayList.get(position).getSafe() == 2) {
				titleView.setTextColor(0xFFcc0000);
				safeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_error_encrypt));
			}
			if (modelsArrayList.get(position).getSafe() == 1) {
				safeIcon.setVisibility(View.INVISIBLE);
			}

		} else {
			rowView = inflater.inflate(R.layout.mail_body_item, parent, false);

			WebView body = (WebView) rowView.findViewById(R.id.textBody);
			body.getSettings().setBuiltInZoomControls(true);
			body.getSettings().setSupportZoom(true);
			body.getSettings().setDisplayZoomControls(false);
			try {
				if (modelsArrayList.get(position).useHTML())
					body.loadData(URLEncoder.encode(modelsArrayList.get(position).getMessage(), "utf-8").replaceAll(
							"\\+", "%20"), "text/html; charset=utf-8", "UTF-8");
				else
					body.loadData(URLEncoder.encode(modelsArrayList.get(position).getMessage(), "utf-8").replaceAll(
							"\\+", "%20"), "text/plain; charset=utf-8", "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}
		return rowView;
	}
}