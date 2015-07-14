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
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import at.flack.R;

public class QRReader extends AppCompatActivity {
	Intent qrDroid;

	TextView erg;
	String result;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrcode_reader);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		final Button button = (Button) findViewById(R.id.button_scan);
		erg = (TextView) findViewById(R.id.erg);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				qrDroid = new Intent("com.google.zxing.client.android.SCAN");
				qrDroid.putExtra("SCAN_MODE", "QR_CODE_MODE");

				try {
					startActivityForResult(qrDroid, 0);
				} catch (ActivityNotFoundException activity) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								startActivity(new Intent(Intent.ACTION_VIEW, Uri
										.parse("market://details?id=la.droid.qr")));
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(QRReader.this);
					builder.setMessage(QRReader.this.getResources().getString(R.string.no_qr_dialog_message))
							.setPositiveButton(QRReader.this.getResources().getString(R.string.yes),
									dialogClickListener).setNegativeButton(
									QRReader.this.getResources().getString(R.string.no), dialogClickListener).show();
				}
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			button.callOnClick();
		else
			button.performClick();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (0 == requestCode && null != data && data.getExtras() != null) {
			result = data.getExtras().getString("la.droid.qr.result");
			Intent returnIntent = new Intent();
			returnIntent.putExtra("QR_RETURNED_Bytes", result.getBytes());
			setResult(RESULT_OK, returnIntent);
			finish();
		}
	}

	public String printArray(byte[] array) {

		String erg = "";
		for (int i = 0; i < array.length; i++) {
			erg += array[i];
		}
		return erg;
	}

}
