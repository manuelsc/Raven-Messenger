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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Date;

import mail.Email;
import mail.MailControlAndroid;
import safe.KeyEntity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.Toast;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.exchange.KeySafe;
import encryption.Message;
import exceptions.MessageEncrypterException;

public class NewMailActivity extends ActionBarActivity {

	private KeyEntity key;
	private boolean encrypt_messages;
	private EditText send_to, subject, body;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mail_new);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		send_to = (EditText) this.findViewById(R.id.send_to);
		subject = (EditText) this.findViewById(R.id.subject);
		body = (EditText) this.findViewById(R.id.body);

		Intent intent = getIntent();
		send_to.setText(intent.getStringExtra("MAIL") != null ? intent.getStringExtra("MAIL") : "");
		subject.setText(intent.getStringExtra("SUBJECT") != null ? intent.getStringExtra("SUBJECT") : "");
		body.setText(intent.getStringExtra("BODY") != null ? ("\n-------------------------\n" + intent
				.getStringExtra("BODY")) : "");
		key = (KeyEntity) intent.getSerializableExtra("KEY");
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;

		send_to.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					reloadAfterKeyExchange();
				}
			}
		});

	}

	private void reloadAfterKeyExchange() {
		key = KeySafe.getInstance(this).get(send_to.getText().toString());
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;
		this.invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.new_mail, menu);

		MenuItem item_encrypted = menu.findItem(R.id.action_message_encrypted);
		if (key == null) {
			item_encrypted.setVisible(false);
		} else {
			item_encrypted.setVisible(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		case R.id.sendMail: {
			if (send_to.getText().toString().length() != 0 && subject.getText().toString().length() != 0
					&& body.getText().toString().length() != 0) {
				if (key != null && encrypt_messages) {
					try {
						sendMail(send_to.getText().toString(), new Message(subject.getText().toString())
								.encryptedMessage(key), new Message(body.getText().toString()).encryptedMessage(key));
					} catch (MessageEncrypterException e) {
						Toast.makeText(this, "Error encrypting mail", Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
				} else {
					sendMail(send_to.getText().toString(), subject.getText().toString(), body.getText().toString());
				}
				Toast.makeText(this, getResources().getString(R.string.mail_send), Toast.LENGTH_SHORT).show();
				finish();
			} else {
				Toast.makeText(this, getResources().getString(R.string.mail_activity_new_empty_mail), Toast.LENGTH_LONG)
						.show();
			}

			return true;
		}
		case R.id.action_message_encrypted: {
			encrypt_messages = !encrypt_messages;
			if (encrypt_messages) {
				item.setIcon(R.drawable.ic_action_message_encrypted_light);
			} else {
				item.setIcon(R.drawable.ic_not_encrypted_white);
			}
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	public void sendMail(String mail, String subject, String body) {
		new MailSend().execute(mail, subject, body);
		ArrayList<Email> all;
		try {
			all = loadMails();
			if (all == null)
				all = new ArrayList<Email>();
		} catch (Exception e) {
			all = new ArrayList<Email>();
		}

		String myMail;
		try {
			myMail = getSharedPreferences("mail", Context.MODE_PRIVATE).getString("mailaddress", "");
			if (myMail.equals(""))
				myMail = "me";
		} catch (Exception e) {
			myMail = "me";
		}
		all.add(0, new Email(mail, myMail, subject, new Date(System.currentTimeMillis()), body, null));
		try {
			saveMails(all);
		} catch (Exception e) {
		}
		;
	}

	class MailSend extends AsyncTask<String, Void, Void> {
		private Exception exception;

		@Override
		protected Void doInBackground(String... params) {
			if (params.length == 3) {
				try {
					MailControlAndroid.sendMailAndroid(MainActivity.mailprofile, params[0], params[1], params[2]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	public ArrayList<Email> loadMails() throws StreamCorruptedException, FileNotFoundException, IOException {
		ObjectInputStream inputStream = null;
		ArrayList<Email> erg = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(NewMailActivity.this.getFilesDir(),
					"mails_outgoing.dat")));
			erg = (ArrayList<Email>) inputStream.readObject();
		} catch (ClassNotFoundException e) {
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return erg;
	}

	public void saveMails(ArrayList<Email> mails) {
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(new File(NewMailActivity.this.getFilesDir(),
					"mails_outgoing.dat")));
			outputStream.writeObject(mails);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (outputStream != null)
					outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
