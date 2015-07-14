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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.gc.materialdesign.R;
import com.gc.materialdesign.views.Slider;
import com.gc.materialdesign.views.Slider.OnValueChangedListener;

public class ColorSelector2 extends android.app.Dialog implements OnValueChangedListener {

	protected int color = Color.BLACK;
	protected View colorView;

	protected com.gc.materialdesign.widgets.ColorSelector.OnColorSelectedListener onColorSelectedListener;
	protected Slider red, green, blue;

	public ColorSelector2(Context context, Integer color,
			com.gc.materialdesign.widgets.ColorSelector.OnColorSelectedListener onColorSelectedListener2) {
		super(context, android.R.style.Theme_Translucent);

		this.onColorSelectedListener = onColorSelectedListener2;
		if (color != null)
			this.color = color;
		setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (ColorSelector2.this.onColorSelectedListener != null)
					ColorSelector2.this.onColorSelectedListener.onColorSelected(ColorSelector2.this.color);
			}
		});

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color_selector);

		colorView = findViewById(R.id.viewColor);
		colorView.setBackgroundColor(color);
		colorView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		colorView.post(new Runnable() {

			@Override
			public void run() {
				LinearLayout.LayoutParams params = (LayoutParams) colorView.getLayoutParams();
				params.height = colorView.getWidth();
				colorView.setLayoutParams(params);
			}
		});

		red = (Slider) findViewById(R.id.red);
		green = (Slider) findViewById(R.id.green);
		blue = (Slider) findViewById(R.id.blue);

		int r = (this.color >> 16) & 0xFF;
		int g = (this.color >> 8) & 0xFF;
		int b = (this.color >> 0) & 0xFF;

		red.setValue(r);
		green.setValue(g);
		blue.setValue(b);

		red.setOnValueChangedListener(this);
		green.setOnValueChangedListener(this);
		blue.setOnValueChangedListener(this);
	}

	@Override
	public void onValueChanged(int value) {
		color = Color.rgb(red.getValue(), green.getValue(), blue.getValue());
		colorView.setBackgroundColor(color);
	}

	public interface OnColorSelectedListener {
		public void onColorSelected(int color);
	}

}