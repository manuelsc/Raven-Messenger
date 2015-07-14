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

import android.view.View;
import android.widget.ImageView;
import at.flack.R;

public class LoadMoreAdapter {

	private View mainview;
	private ImageView text;
	private View prog;

	public LoadMoreAdapter(View mainview) {
		this.mainview = mainview;
		prog = (View) mainview.findViewById(R.id.progressbar);
		text = (ImageView) mainview.findViewById(R.id.title);
	}

	public boolean isEnabled() {
		return text.getVisibility() == View.VISIBLE;
	}

	public void setEnabled(boolean b) {
		if (b) {
			text.setVisibility(View.VISIBLE);
			prog.setVisibility(View.INVISIBLE);
		} else {
			text.setVisibility(View.INVISIBLE);
			prog.setVisibility(View.VISIBLE);
		}
		mainview.setEnabled(b);
	}

	public void setVisible(boolean b) {
		mainview.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
	}

	public View getView() {
		return mainview;
	}

}