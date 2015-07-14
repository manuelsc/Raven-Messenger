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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.Button;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import at.flack.R;
import at.flack.exchange.KeySafe;
import safe.KeyEntity;

public class KeyInformationActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_key_overview);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		final Intent intent = getIntent();
		final ImageView image = (ImageView) this.findViewById(R.id.keyCode);
		final TextView algo = (TextView) this.findViewById(R.id.algo);
		final TextView creationdate = (TextView) this.findViewById(R.id.creation_date);
		final Button deleteKey = (Button) this.findViewById(R.id.deleteKey);
		final Resources res = this.getResources();

		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE: {
					KeySafe.getInstance(this).remove(intent.getStringExtra("primary"));
					KeySafe.getInstance(this).save();
					Toast.makeText(KeyInformationActivity.this,
							res.getString(R.string.activity_keyinformation_delete_key_done), Toast.LENGTH_LONG).show();
					Intent re = new Intent();
					re.putExtra("reload", true);
					setResult(RESULT_OK, re);
					finish();
					break;
				}
				case DialogInterface.BUTTON_NEGATIVE:
					break;
				}
			}
		};

		deleteKey.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(KeyInformationActivity.this);
				builder.setMessage(res.getString(R.string.activity_keyinformation_delete_key_dialog))
						.setPositiveButton(res.getString(R.string.yes), dialogClickListener).setNegativeButton(
								res.getString(R.string.no), dialogClickListener).show();
			}
		});

		if (intent.getByteArrayExtra("hashed_key") != null)
			image.setImageDrawable(makeKeyImage(this, intent.getByteArrayExtra("hashed_key")));
		String encryption = "???";
		String hash = "???";
		if (intent.getByteExtra("algo", KeyEntity.DEFAULT) == KeyEntity.BLAKE_AES_NONE_1) {
			encryption = "AES Encrypted";
			hash = "Blake Hash (512 bit)";
		}
		if (intent.getByteExtra("algo", KeyEntity.DEFAULT) == KeyEntity.BLAKE_AES_MARS_1) {
			encryption = "AES + MARS Encrypted [old]";
			hash = "Blake Hash (512 bit)";
		}
		if (intent.getByteExtra("algo", KeyEntity.DEFAULT) == KeyEntity.BLAKE_AES_MARS_2) {
			encryption = "AES + MARS Encrypted";
			hash = "Blake Hash (512 bit)";
		}
		if (intent.getByteExtra("algo", KeyEntity.DEFAULT) == KeyEntity.BLAKE_AES_SERPENT_1) {
			encryption = "AES + SERPENT Encrypted";
			hash = "Blake Hash (512 bit)";
		}
		if (intent.getByteExtra("algo", KeyEntity.DEFAULT) == KeyEntity.BLAKE_AES_THREEFISH_1) {
			encryption = "AES + THREEFISH Encrypted";
			hash = "Blake Hash (512 bit)";
		}
		algo.setText(hash + " \n" + encryption);
		creationdate.setText(getDateForLock(intent.getLongExtra("creationdate", System.currentTimeMillis()),
				"dd. MMMM yyyy \nHH:mm:ss"));

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static String getDateForLock(long milliSeconds, String dateFormat) {
		DateFormat formatter = new SimpleDateFormat(dateFormat, Locale.getDefault());
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds);
		return formatter.format(calendar.getTime());
	}

	public static Drawable makeKeyImage(Context mContext, byte[] hashed_key) {
		Paint p = null;
		Bitmap bkg = null;
		bkg = Bitmap.createBitmap(250, 250, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(bkg);

		float xc, yc;
		int count = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				p = new Paint();
				int l = (int) (((0xFF & 0xFFL) << 24) | ((hashed_key[(count + 2) % hashed_key.length] & 0xFFL) << 16)
						| ((hashed_key[(count + 1) % hashed_key.length] & 0xFFL) << 8) | ((hashed_key[count] & 0xFFL) << 0));
				p.setColor(l);
				xc = (float) (x * 31.25);
				yc = (float) (y * 31.25);
				c.drawRect(xc, yc, (float) 31.25 + xc - 1, (float) 31.25 + yc - 1, p);
				count++;
			}
		}
		return new BitmapDrawable(mContext.getResources(), bkg);
	}
}
