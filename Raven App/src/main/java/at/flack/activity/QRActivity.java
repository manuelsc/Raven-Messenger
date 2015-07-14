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
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import at.flack.R;
import at.flack.ui.RoundedImageView;

import com.gc.materialdesign.views.Button;

public class QRActivity extends NFCActionBarActivity {
	private Button qr_lesen;
	private Button qr_erzeugen;
	private Button exchange_auto;
	private TextView schluessel;
	private Intent qrDroid;
	private static String myID = "";
	private static String otherID = "";
	private HANDSHAKE_TYPE type;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acitivity_qr_overview);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		qr_lesen = (Button) findViewById(R.id.qr_lesen);
		qr_erzeugen = (Button) findViewById(R.id.qr_create);
		exchange_auto = (Button) findViewById(R.id.exchange_auto);
		schluessel = (TextView) findViewById(R.id.view);
		ImageView img = (ImageView) findViewById(R.id.ImageView0123);
		BitmapDrawable draw = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_nfc);
		img.setImageBitmap(RoundedImageView.getCroppedBitmap(draw.getBitmap(), 300));

		Intent intent = this.getIntent();
		final String forfb = intent.getStringExtra("forfb");
		final String user_id = intent.getStringExtra("fbuser");
		final String forsms = intent.getStringExtra("forsms");
		final String formail = intent.getStringExtra("formail");

		myID = intent.getStringExtra("myID");
		otherID = intent.getStringExtra("otherID");
		type = (HANDSHAKE_TYPE) intent.getSerializableExtra("type");

		runNFC();
		findViewById(R.id.nfc_support).setVisibility(
				isNfcDevice && nfcAdapter.isNdefPushEnabled() ? View.VISIBLE : View.INVISIBLE);
		exchange_auto.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("request_auto_exchange", true);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		});

		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("advanced_key_share", false)) {
			qr_erzeugen.setVisibility(View.INVISIBLE);
			qr_lesen.setVisibility(View.INVISIBLE);
			schluessel.setVisibility(View.INVISIBLE);
		}

		qr_erzeugen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(view.getContext(), QRCreator.class);
				myIntent.putExtra("forfb", forfb);
				myIntent.putExtra("forsms", forsms);
				myIntent.putExtra("formail", formail);
				myIntent.putExtra("fbuser", user_id);
				try {
					startActivityForResult(myIntent, 1);
				} catch (ActivityNotFoundException activity) {

				}
			}
		});

		qr_lesen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {

				qrDroid = new Intent("la.droid.qr.scan");
				qrDroid.putExtra("la.droid.qr.complete", true);
				try {
					startActivityForResult(qrDroid, 0);
				} catch (ActivityNotFoundException activity) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								try {
									startActivity(new Intent(Intent.ACTION_VIEW, Uri
											.parse("market://details?id=la.droid.qr")));
								} catch (ActivityNotFoundException e) {
								}
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(QRActivity.this);
					builder.setMessage(QRActivity.this.getResources().getString(R.string.no_qr_dialog_message))
							.setPositiveButton(QRActivity.this.getResources().getString(R.string.yes),
									dialogClickListener).setNegativeButton(
									QRActivity.this.getResources().getString(R.string.no), dialogClickListener).show();
				}

			}
		});
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:

			finish();
			return true;
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0 && data != null && data.getExtras() != null) {
			String result = data.getExtras().getString("la.droid.qr.result");
			Intent returnIntent = new Intent();
			returnIntent.putExtra("QR_RETURNED_Bytes", result.getBytes());
			setResult(RESULT_OK, returnIntent);
			finish();
		}
	}

	@Override
	protected void reloadAfterKeyExchange() {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("reload", true);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	@Override
	public HANDSHAKE_TYPE initNFCActivity() {
		setMyID(myID);
		setOtherID(otherID);
		return type;
	}

}
