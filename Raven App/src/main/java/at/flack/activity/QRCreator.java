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

import safe.KeyEntity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.qr.Contents;
import at.flack.qr.QRCodeEncoder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import encryption.SecureKeyGenerator;
import exchange.ExchangeInformation;

public class QRCreator extends AppCompatActivity {

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrcode_reator);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		final ImageView imageView = (ImageView) findViewById(R.id.qrCode);

		final TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		final String forfb = getIntent().getStringExtra("forfb");
		final String forsms = getIntent().getStringExtra("forsms");
		final String formail = getIntent().getStringExtra("formail");
		final String user_id = getIntent().getStringExtra("fbuser");

		if (forfb != null)
			setTitle("Facebook");
		else if (formail != null)
			setTitle("E-Mail");
		if (forsms != null)
			setTitle("SMS");

		imageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				String teleNr = mTelephonyMgr.getLine1Number();
				SecureKeyGenerator skg = new SecureKeyGenerator();
				skg.addNounce(skg.getAndroidNounce());

				skg.doRandom();
				ExchangeInformation ei = null;
				if (forfb != null)
					ei = new ExchangeInformation(forfb, user_id, skg.getFirstKey(), skg.getSecondKey(),
							KeyEntity.DEFAULT, ExchangeInformation.FACEBOOK);
				else if (forsms != null)
					ei = new ExchangeInformation(forsms, teleNr, skg.getFirstKey(), skg.getSecondKey(),
							KeyEntity.DEFAULT, ExchangeInformation.SMS);
				else if (formail != null)
					ei = new ExchangeInformation(formail, user_id, skg.getFirstKey(), skg.getSecondKey(),
							KeyEntity.DEFAULT, ExchangeInformation.MAIL);

				MainActivity.tempSafe.put(new String(ei.getRandomConfirmation()), ei.getKey());
				int qrCodeDimention = 500;

				QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(new String(ei.getEncdodedBytes()), null,
						Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

				try {
					Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
					imageView.setImageBitmap(bitmap);
				} catch (WriterException e) {
					e.printStackTrace();
				}

				Intent returnIntent = new Intent();
				setResult(RESULT_OK, returnIntent);
			}
		});
		imageView.performClick();

	}

	public String printArray(byte[] ba) {
		String s = "";
		for (byte b : ba)
			s = s + b + ";";
		return s;
	}

}
