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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mail.Email;
import safe.KeyEntity;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.flack.activity.MailOverview;
import at.flack.activity.NewMailActivity;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.LoadMoreAdapter;
import at.flack.ui.ProfilePictureCache;

import com.melnykov.fab.FloatingActionButton;

import encryption.Base64;
import encryption.Message;
import exceptions.MessageDecrypterException;

public class MailOutActivity extends Fragment {

	private ListView contactList;
	private View progressbar;
	private LoadMoreAdapter loadmore;
	private SwipeRefreshLayout swipe;
	private ArrayList<ContactModel> mailOutList;
	private static MailOutActivity me;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_mail_main, container, false);

		loadmore = new LoadMoreAdapter(inflater.inflate(R.layout.contacts_loadmore, contactList, false));
		contactList = (ListView) rootView.findViewById(R.id.listview);
		TextView padding = new TextView(getActivity());
		padding.setHeight(10);
		contactList.addHeaderView(padding);
		contactList.setHeaderDividersEnabled(false);

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
		if (mailOutList == null)
			progressbar.setVisibility(View.VISIBLE);
		updateContactList(((MainActivity) this.getActivity()));

		swipe = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
		swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (getActivity() instanceof MainActivity) {
					updateContactList(((MainActivity) getActivity()));
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
				if (mailOutList == null) {
					updateContactList((MainActivity) getActivity());
				}
				openMessageActivity(getActivity(), arg2 - 1);
			}

		});
		setRetainInstance(true);
		return rootView;

	}

	public static MailOutActivity getInstance() {
		return getInstance(false);
	}

	public static MailOutActivity getInstance(boolean force) {
		if (me == null || force)
			me = new MailOutActivity();
		return me;
	}

	public Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
		Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mutableBitmap);
		drawable.setBounds(0, 0, widthPixels, heightPixels);
		drawable.draw(canvas);

		return mutableBitmap;
	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (arg2 < 0 || mailOutList.size() == 0)
			return;

		Intent mailIntent = new Intent(activity, MailOverview.class);
		mailIntent.putExtra("CONTACT_MAIL", mailOutList.get(arg2).getFromMail());
		mailIntent.putExtra("CONTACT_NAME", mailOutList.get(arg2).getFromName());
		mailIntent.putExtra("MY_MAIL", mailOutList.get(arg2).getToMail());

		mailIntent.putExtra("profilePicture", mailOutList.get(arg2).getPicture());

		mailIntent.putExtra("MAIL_TEXT", mailOutList.get(arg2).getLastMessage());
		mailIntent.putExtra("MAIL_TITLE", mailOutList.get(arg2).getTitle());
		mailIntent.putExtra("MAIL_DATE", mailOutList.get(arg2).getDate().toString());
		mailIntent.putExtra("ALLOW_KEY_EXCHANGE", false);
		try {
			KeyEntity key = KeySafe.getInstance(activity).get(mailOutList.get(arg2).getFromMail());
			if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY)
				mailIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}

		activity.startActivityForResult(mailIntent, 2);
	}

	public void updateContactList(MainActivity activity) {
		ArrayList<Email> emails;
		try {
			emails = loadMails();
		} catch (IOException e1) {
			if (swipe != null)
				swipe.setRefreshing(false);
			if (progressbar != null)
				progressbar.setVisibility(View.INVISIBLE);
			return;
		}
		mailOutList = new ArrayList<ContactModel>();
		ContactModel model = null;
		Resources res = activity.getResources();
		for (Email mail : emails) {//
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
			mailOutList.add(model);
		}

		if (contactList != null && contactList.getAdapter() == null) {
			ContactAdapter arrayAdapter = new ContactAdapter(activity, mailOutList, 0);
			arrayAdapter.notifyDataSetChanged();
			contactList.setAdapter(arrayAdapter);
			contactList.setSelection(0);
		} else if (mailOutList.size() > 0 && contactList != null) {
			((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
					mailOutList, 0);
		}
		if (swipe != null)
			swipe.setRefreshing(false);
		if (progressbar != null)
			progressbar.setVisibility(View.INVISIBLE);
		if (loadmore != null)
			loadmore.setEnabled(true);
	}

	public String getDate(Date date) {
		Date today = new Date(System.currentTimeMillis());
		if (date.getDate() == today.getDate() && date.getMonth() == today.getMonth()
				&& date.getYear() == today.getYear())
			return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
		else
			return new SimpleDateFormat("dd. MMM", Locale.getDefault()).format(date);
	}

	public ArrayList<Email> loadMails() throws StreamCorruptedException, FileNotFoundException, IOException {
		if (getActivity() == null)
			return null;
		ObjectInputStream inputStream = null;
		ArrayList<Email> erg = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(getActivity().getFilesDir(),
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

}
