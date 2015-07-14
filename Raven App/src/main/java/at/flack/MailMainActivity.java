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

package at.flack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mail.Email;
import mail.MailAccounts;
import safe.KeyEntity;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import at.flack.activity.MailOverview;
import at.flack.activity.NewMailActivity;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.LoadMoreAdapter;
import at.flack.ui.ProfilePictureCache;

import com.gc.materialdesign.views.Button;
import com.gc.materialdesign.widgets.SnackBar;
import com.melnykov.fab.FloatingActionButton;

import encryption.Base64;
import encryption.Message;
import exceptions.MessageDecrypterException;

public class MailMainActivity extends Fragment {

	private ListView contactList;
	private View progressbar;
	private SharedPreferences prefs;
	private EditText password;
	private Button login_button;
	private LoadMoreAdapter loadmore;
	private SwipeRefreshLayout swipe;
	private int limit = 12;
	private static MailMainActivity me;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		prefs = getActivity().getSharedPreferences("mail", Context.MODE_PRIVATE);
		if (!prefs.getString("mailaddress", "").equals("")) {
			View rootView = inflater.inflate(R.layout.fragment_mail_main, container, false);

			loadmore = new LoadMoreAdapter(inflater.inflate(R.layout.contacts_loadmore, contactList, false));
			contactList = (ListView) rootView.findViewById(R.id.listview);
			TextView padding = new TextView(getActivity());
			padding.setHeight(10);
			contactList.addHeaderView(padding);
			contactList.setHeaderDividersEnabled(false);
			contactList.addFooterView(loadmore.getView(), null, false);
			contactList.setFooterDividersEnabled(false);
			progressbar = rootView.findViewById(R.id.load_screen);
			FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
			fab.attachToListView(contactList);
			fab.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent newMail = new Intent(getActivity(), NewMailActivity.class);
					getActivity().startActivity(newMail);
				}
			});

			progressbar = rootView.findViewById(R.id.load_screen);
			if (MainActivity.getMailcontacts() == null)
				progressbar.setVisibility(View.VISIBLE);
			updateContactList(((MainActivity) this.getActivity()));

			swipe = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
			swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					if (getActivity() instanceof MainActivity) {
						MainActivity.mailprofile = null;
						((MainActivity) getActivity()).emailLogin(1, 0, limit);
					}

				}
			});
			contactList.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {

				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					int topRowVerticalPosition = (contactList == null || contactList.getChildCount() == 0) ? 0
							: contactList.getChildAt(0).getTop();
					swipe.setEnabled(topRowVerticalPosition >= 0);
				}
			});

			contactList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if (MainActivity.getListItems() == null) {
						updateContactList((MainActivity) getActivity());
					}
					openMessageActivity(getActivity(), arg2 - 1);
				}

			});

			loadmore.getView().setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					loadmore.setEnabled(false);
					MainActivity.mailprofile = null;
					limit += 12;
					((MainActivity) getActivity()).emailLogin(1, 0, limit);
				}
			});

			setRetainInstance(true);
			return rootView;
		} else {
			View rootView = inflater.inflate(R.layout.fragment_email_login, container, false);
			final EditText mail = (EditText) rootView.findViewById(R.id.email);
			final EditText password = (EditText) rootView.findViewById(R.id.password);

			final EditText host = (EditText) rootView.findViewById(R.id.host);
			final EditText port = (EditText) rootView.findViewById(R.id.port);
			final EditText smtphost = (EditText) rootView.findViewById(R.id.smtphost);
			final EditText smtpport = (EditText) rootView.findViewById(R.id.smtpport);

			final Button login_button = (Button) rootView.findViewById(R.id.login_button);

			final RadioGroup radioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup);
			final RadioButton imap = (RadioButton) rootView.findViewById(R.id.radioButtonImap);

			radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					host.setVisibility(View.VISIBLE);
					port.setVisibility(View.VISIBLE);
					smtphost.setVisibility(View.VISIBLE);
					smtpport.setVisibility(View.VISIBLE);
				}
			});

			login_button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					int at = mail.getText().toString().indexOf("@");
					int dot = mail.getText().toString().lastIndexOf(".");
					if (mail.getText().length() <= 0 || at < 0 || dot < 0) {
						Toast.makeText(MailMainActivity.this.getActivity(),
								getResources().getString(R.string.facebook_login_please_enter_valid_mail),
								Toast.LENGTH_SHORT).show();
						return;
					}
					if (password.getText().length() <= 0) {
						Toast.makeText(MailMainActivity.this.getActivity(),
								getResources().getString(R.string.facebook_login_please_enter_valid_pw),
								Toast.LENGTH_SHORT).show();
						return;
					}

					String hostPart = mail.getText().toString().substring(at + 1, dot);

					MailAccounts mailacc = null;
					try {
						mailacc = MailAccounts.valueOf(hostPart.toUpperCase(Locale.GERMAN));
					} catch (IllegalArgumentException e) {
					}
					if (mailacc == null) {
						radioGroup.setVisibility(View.VISIBLE);
						if (host.getText().toString().isEmpty() || port.getText().toString().isEmpty()
								|| smtphost.getText().toString().isEmpty() || smtpport.getText().toString().isEmpty()) {
							Toast.makeText(
									MailMainActivity.this.getActivity(),
									MailMainActivity.this.getActivity().getResources().getString(
											R.string.activity_mail_enter_more_information), Toast.LENGTH_LONG).show();
							return;
						}
						prefs.edit().putString("mailsmtp", smtphost.getText().toString()).apply();
						prefs.edit().putInt("mailsmtpport", Integer.parseInt(smtpport.getText().toString()));
						prefs.edit().putString("mailimap", host.getText().toString()).apply();
						prefs.edit().putInt("mailimapport", Integer.parseInt(port.getText().toString())).apply();
						prefs.edit().putBoolean("mailuseimap", imap.isChecked()).apply();
					}

					login_button.setEnabled(false);
					login_button.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
					prefs.edit().putString("mailaddress", mail.getText().toString()).apply();
					prefs.edit().putString("mailpassword", password.getText().toString()).apply();

					if (mailacc != null) {
						prefs.edit().putString("mailsmtp", mailacc.getSmtpHost()).apply();
						prefs.edit().putInt("mailsmtpport", mailacc.getSMTPPort());
						prefs.edit().putString("mailimap", mailacc.getHost()).apply();
						prefs.edit().putInt("mailimapport", mailacc.getPort()).apply();
						prefs.edit().putBoolean("mailuseimap", true).apply();
					}

					prefs.edit().commit();
					((MainActivity) getActivity()).emailLogin(0);
				}
			});

			setRetainInstance(true);

			return rootView;
		}
	}

	public static MailMainActivity getInstance() {
		return getInstance(false);
	}

	public static MailMainActivity getInstance(boolean force) {
		if (me == null || force)
			me = new MailMainActivity();
		return me;
	}

	public static MailMainActivity forceReinstance() {
		me = new MailMainActivity();
		return me;
	}

	public void wrongCredentials(int returned) {
		if (MailMainActivity.this.getActivity() == null)
			return;
		Resources res = MailMainActivity.this.getActivity().getResources();
		if (password != null && login_button != null) {
			if (MailMainActivity.this.getActivity() != null)
				Toast.makeText(MailMainActivity.this.getActivity(),
						getResources().getString(R.string.facebook_login_incorrect_or_offline), Toast.LENGTH_SHORT)
						.show();
			password.setText("");
			login_button.getBackground().setColorFilter(null);
			login_button.setEnabled(true);

		} else {
			if (MailMainActivity.this.getActivity() != null) {
				final SnackBar snackbar = new SnackBar(MailMainActivity.this.getActivity(), res
						.getString(R.string.snackbar_cannot_load_mails), res.getString(R.string.snackbar_retry),
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (MailMainActivity.this.getActivity() instanceof MainActivity) {
									MainActivity ac = (MainActivity) MailMainActivity.this.getActivity();
									ac.emailLogin(1);
								}
							}
						});
				snackbar.show();
			}
		}
		if (returned == 0) {
			prefs.edit().clear().apply();
			if (MailMainActivity.this.getActivity() != null) {
				((MainActivity) getActivity()).redrawMailFragment();
				((MainActivity) getActivity()).drawMailFragment();
			}
		}
	}

	public Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
		Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mutableBitmap);
		drawable.setBounds(0, 0, widthPixels, heightPixels);
		drawable.draw(canvas);

		return mutableBitmap;
	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (arg2 < 0 || MainActivity.getMailcontacts() == null || MainActivity.getMailcontacts().size() == 0)
			return;

		Intent mailIntent = new Intent(activity, MailOverview.class);
		mailIntent.putExtra("CONTACT_MAIL", MainActivity.getMailItems().get(arg2).getFromMail());
		mailIntent.putExtra("CONTACT_NAME", MainActivity.getMailItems().get(arg2).getFromName());
		mailIntent.putExtra("MY_MAIL", MainActivity.getMailItems().get(arg2).getToMail());

		mailIntent.putExtra("profilePicture", MainActivity.getMailItems().get(arg2).getPicture());

		mailIntent.putExtra("MAIL_TEXT", MainActivity.getMailItems().get(arg2).getLastMessage());
		mailIntent.putExtra("MAIL_TITLE", MainActivity.getMailItems().get(arg2).getTitle());
		mailIntent.putExtra("MAIL_DATE", MainActivity.getMailItems().get(arg2).getDate().toString());

		try {
			KeyEntity key = KeySafe.getInstance(activity).get(MainActivity.getMailItems().get(arg2).getFromMail());
			if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY)
				mailIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}

		activity.startActivityForResult(mailIntent, 2);
	}

	public void updateContactList(MainActivity activity) {
		if (MainActivity.getMailcontacts() == null)
			return;
		MainActivity.setMailItems(new ArrayList<ContactModel>());
		ContactModel model = null;
		Resources res = activity.getResources();
		for (Email mail : MainActivity.getMailcontacts()) {//
			model = new ContactModel(ProfilePictureCache.getInstance(activity).get(mail.getSender()),
					mail.getSubject(), mail.getText(), getDate(mail.getDate()), mail.isRead(), mail.getSender(), mail
							.getRecipient(), mail.getSenderName());
			if (KeySafe.getInstance(activity).contains(mail.getSender()) && Base64.isBase64(mail.getSubject())) {
				KeyEntity key = KeySafe.getInstance(activity).get(mail.getSender());
				if (key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
					model.setEncrypted(View.VISIBLE);
					try {
						model.setTitle(new Message(mail.getSubject()).decryptedMessage(key));
					} catch (MessageDecrypterException e) {
					}
				}
			}
			// Pre Shared Exchange
			if ((mail.getText().toString().length() == 10 || mail.getText().toString().length() == 9)
					&& mail.getText().toString().charAt(0) == '%') { // 4
				if (Base64.isPureBase64(mail.getText().toString().substring(1, 9))) {
					model.setTitle(res.getString(R.string.handshake_message));
				}
			}
			// DH Exchange
			if ((mail.getText()).toString().length() >= 120 && mail.getText().toString().length() < 125
					&& mail.getText().toString().charAt(0) == '%') { // DH
																		// Exchange!
				if (Base64.isPureBase64(mail.getText().toString().substring(1,
						mail.getText().toString().lastIndexOf("=")))) {
					model.setTitle(res.getString(R.string.handshake_message));
				}
			}
			MainActivity.getMailItems().add(model);
		}

		if (contactList != null && contactList.getAdapter() == null) {
			ContactAdapter arrayAdapter = new ContactAdapter(activity, MainActivity.getMailItems(), 0);
			arrayAdapter.notifyDataSetChanged();
			contactList.setAdapter(arrayAdapter);
			contactList.setSelection(0);
		} else if (MainActivity.getMailItems().size() > 0 && contactList != null) {
			((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
					MainActivity.getMailItems(), 0);
		}

		if (swipe != null)
			swipe.setRefreshing(false);
		if (progressbar != null)
			progressbar.setVisibility(View.INVISIBLE);
		if (loadmore != null)
			loadmore.setEnabled(true);
	}

	public int getPositionOfItem(String s) {
		if (MainActivity.getMailcontacts() == null)
			return -1;
		for (int i = 0; i < MainActivity.getMailcontacts().size(); i++) {
			if (MainActivity.getMailcontacts().get(i).getSender().equals(s))
				return i;
		}
		return -1;
	}

	public String getDate(Date date) {
		Date today = new Date(System.currentTimeMillis());
		if (date.getDate() == today.getDate() && date.getMonth() == today.getMonth()
				&& date.getYear() == today.getYear())
			return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
		else
			return new SimpleDateFormat("dd. MMM", Locale.getDefault()).format(date);
	}

}
