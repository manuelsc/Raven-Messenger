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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import safe.KeyEntity;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
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
import android.widget.TextView;
import android.widget.Toast;
import api.FacebookContact;
import at.flack.activity.FbMessageOverview;
import at.flack.activity.NewFbContactActivity;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.LoadMoreAdapter;
import at.flack.ui.ProfilePictureCache;
import at.flack.ui.SmileyKonverter;
import at.flack.utils.ImageDownloader;

import com.gc.materialdesign.views.Button;
import com.melnykov.fab.FloatingActionButton;

import encryption.Base64;
import encryption.Message;
import exceptions.MessageDecrypterException;

public class FacebookMainActivity extends Fragment {

	private ListView contactList;
	private View progressbar;
	private LoadMoreAdapter loadmore;
	private int page = 0;
	private SwipeRefreshLayout swipe;
	private SmileyKonverter smiley_helper;
	private static FacebookMainActivity me;
	private View cantloadContacts;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (((MainActivity) getActivity()).existsCookie()) {
			View rootView = inflater.inflate(R.layout.fragment_fb_main, container, false);

			contactList = (ListView) rootView.findViewById(R.id.listview);
			loadmore = new LoadMoreAdapter(inflater.inflate(R.layout.contacts_loadmore, contactList, false));
			cantloadContacts = rootView.findViewById(R.id.nothing_found);

			TextView padding = new TextView(getActivity());
			padding.setHeight(10);
			contactList.addHeaderView(padding);
			contactList.setHeaderDividersEnabled(false);
			contactList.addFooterView(loadmore.getView(), null, false);
			contactList.setFooterDividersEnabled(false);
			smiley_helper = new SmileyKonverter();
			swipe = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
			swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					if (getActivity() instanceof MainActivity)
						((MainActivity) getActivity()).facebookLogin();
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

			loadmore.getView().setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					loadmore.setEnabled(false);
					loadMore();
				}
			});

			setProgressbar(rootView.findViewById(R.id.load_screen));
			if (MainActivity.getFbcontacts() == null)
				getProgressbar().setVisibility(View.VISIBLE);
			updateContacts(getActivity());
			contactList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					openMessageActivity(getActivity(), arg2 - 1);
				}

			});

			FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
			fab.attachToListView(contactList);

			fab.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent qr = new Intent(getActivity(), NewFbContactActivity.class);
					startActivityForResult(qr, 1);
				}
			});

			updateProfilePictures(getActivity());

			setRetainInstance(true);

			return rootView;
		} else {
			View rootView = inflater.inflate(R.layout.fragment_facebook_login, container, false);
			final EditText mail = (EditText) rootView.findViewById(R.id.email);
			final EditText password = (EditText) rootView.findViewById(R.id.password);
			final Button login_button = (Button) rootView.findViewById(R.id.login_button);

			login_button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mail.getText().length() <= 0) {
						Toast.makeText(FacebookMainActivity.this.getActivity(),
								getResources().getString(R.string.facebook_login_please_enter_valid_mail),
								Toast.LENGTH_SHORT).show();
						return;
					}
					if (password.getText().length() <= 0) {
						Toast.makeText(FacebookMainActivity.this.getActivity(),
								getResources().getString(R.string.facebook_login_please_enter_valid_pw),
								Toast.LENGTH_SHORT).show();
						return;
					}
					login_button.setEnabled(false);
					login_button.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
					if (((MainActivity) getActivity()).facebookLogin(mail.getText().toString(), password.getText()
							.toString())) {
					} else {
						Toast.makeText(FacebookMainActivity.this.getActivity(),
								getResources().getString(R.string.facebook_login_incorrect_or_offline),
								Toast.LENGTH_SHORT).show();
						password.setText("");
						login_button.getBackground().setColorFilter(null);
						login_button.setEnabled(true);
					}
				}
			});

			setRetainInstance(true);

			return rootView;
		}
	}

	public static FacebookMainActivity getInstance() {
		return getInstance(false);
	}

	public static FacebookMainActivity getInstance(boolean force) {
		if (me == null || force)
			me = new FacebookMainActivity();
		return me;
	}

	public static FacebookMainActivity forceReinstance() {
		me = new FacebookMainActivity();
		return me;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int i) {
		page = i;
	}

	public void loadMore() {
		if (this.getActivity() == null)
			return;
		page++;
		((MainActivity) getActivity()).loadFacebookContacts(page);

	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (arg2 < 0 || activity == null || MainActivity.getFbcontacts() == null
				|| MainActivity.getFbcontacts().size() == 0)
			return;

		Intent smsIntent = new Intent(activity, FbMessageOverview.class);
		smsIntent.putExtra("CONTACT_ID", MainActivity.getFbcontacts().get(arg2).getFbId());
		smsIntent.putExtra("MY_ID", MainActivity.fb_api.getMyID());
		smsIntent.putExtra("CONTACT_NAME", MainActivity.getFbcontacts().get(arg2).getName());
		smsIntent.putExtra("CONTACT_TID", MainActivity.getFbcontacts().get(arg2).getTid());

		smsIntent.putExtra("isGroupChat", MainActivity.getFbcontacts().get(arg2).getFbId().equals(""));
		try {
			KeyEntity key = KeySafe.getInstance(activity).get(MainActivity.getFbcontacts().get(arg2).getTid());
			if (key != null)
				smsIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}
		activity.startActivityForResult(smsIntent, 2);
	}

	public void showErrorMessage(Activity ac, final boolean b) {
		if (cantloadContacts != null)
			ac.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						cantloadContacts.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
					} catch (Exception e) {
					}
				}
			});
	}

	public void updateContacts(Activity activity) {
		if (MainActivity.getFbcontacts() == null || MainActivity.getFbcontacts().size() <= 0) {
			showErrorMessage(activity, true);
			if (getProgressbar() != null)
				getProgressbar().setVisibility(View.INVISIBLE);
			return;
		}
		showErrorMessage(activity, false);

		MainActivity.setFblistItems(new ArrayList<ContactModel>());
		ContactModel model = null;
		Resources res = activity.getResources();
		for (FacebookContact fbc : MainActivity.getFbcontacts()) {
			model = new ContactModel(ProfilePictureCache.getInstance(activity).get(fbc.getName()), shortName(fbc
					.getName()), fbc.getLastMessage(), fbc.getTime(), fbc.isUnread(), fbc.isOnline(), fbc.isMobile());
			if (KeySafe.getInstance(activity).contains(fbc.getTid())) {
				KeyEntity key = KeySafe.getInstance(activity).get(fbc.getTid());
				if (key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
					model.setEncrypted(View.VISIBLE);
					try {
						model.setLastMessage(new Message(model.getLastMessage()).decryptedMessage(key));
					} catch (MessageDecrypterException e) {
					}
				}
			}
			// Pre Shared Exchange
			if ((fbc.getLastMessage().toString().length() == 10 || fbc.getLastMessage().toString().length() == 9)
					&& fbc.getLastMessage().toString().charAt(0) == '%') { // 4
				if (Base64.isPureBase64(fbc.getLastMessage().toString().substring(1, 9))) {
					model.setLastMessage(res.getString(R.string.handshake_message));
				}
			}
			// DH Exchange
			if (fbc.getLastMessage().toString().length() >= 120 && fbc.getLastMessage().toString().length() < 125
					&& fbc.getLastMessage().toString().charAt(0) == '%') { // DH
																			// Exchange!
				if (Base64.isPureBase64(fbc.getLastMessage().toString().substring(1,
						fbc.getLastMessage().toString().lastIndexOf("=")))) {
					model.setLastMessage(res.getString(R.string.handshake_message));
				}
			}
			if (smiley_helper != null)
				model.setLastMessage(smiley_helper.parseAndroid(model.getLastMessage()));
			MainActivity.getFblistItems().add(model);
		}

		if (contactList != null && contactList.getAdapter() == null) {
			ContactAdapter arrayAdapter = new ContactAdapter(activity, MainActivity.getFblistItems(), 1);
			contactList.setAdapter(arrayAdapter);
			contactList.setSelection(0);

		} else {
			if (contactList != null) {
				((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
						MainActivity.getFblistItems(), 1);

			}
		}
		if (swipe != null)
			swipe.setRefreshing(false);
		if (getProgressbar() != null)
			getProgressbar().setVisibility(View.INVISIBLE);
		if (loadmore != null)
			loadmore.setEnabled(true);

	}

	public String shortName(String s) {
		if (s.length() <= 21)
			return s;
		return s.substring(0, 18) + "...";
	}

	public void updateProfilePictures(Activity activity) {
		new FacebookFillCache().execute(activity);
	}

	public Bitmap getProfilePicture(String s) {
		try {
			return new ImageDownloader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, s).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String loadCookie() throws StreamCorruptedException, FileNotFoundException, IOException {
		if (getActivity() == null)
			return "";
		ObjectInputStream inputStream = null;
		String erg = null;
		try {
			inputStream = new ObjectInputStream(
					new FileInputStream(new File(getActivity().getFilesDir(), "cookie.dat")));
			erg = (String) inputStream.readObject();
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

	class FacebookFillCache extends AsyncTask<Activity, Void, Void> {

		private Exception exception;

		@Override
		protected Void doInBackground(final Activity... params) {
			if (MainActivity.getFbcontacts() == null) {
				params[0].runOnUiThread(new Runnable() {
					@Override
					public void run() {

						showErrorMessage(params[0], true);
						if (getProgressbar() != null)
							getProgressbar().setVisibility(View.INVISIBLE);
					}
				});
				return null;
			}
			showErrorMessage(params[0], false);
			boolean bool = false;
			for (FacebookContact fbc : MainActivity.getFbcontacts()) {
				if (!ProfilePictureCache.getInstance(params[0]).contains(fbc.getName())
						&& fbc.getProfilePicture() != null) {
					ProfilePictureCache.getInstance(params[0]).put(fbc.getName(),
							ImageDownloader.downloadImage(fbc.getProfilePicture()));
					bool = true;
				}
			}
			ProfilePictureCache.getInstance(params[0]).save(params[0]);
			if (bool) {
				params[0].runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateContacts(params[0]);
					}
				});
			}

			return null;
		}
	}

	public int getPositionOfItem(String s) {
		if (MainActivity.getFbcontacts() == null)
			return -1;
		for (int i = 0; i < MainActivity.getFbcontacts().size(); i++) {
			if (MainActivity.getFbcontacts().get(i).getName().equals(s))
				return i;
		}
		return -1;
	}

	public View getProgressbar() {
		return progressbar;
	}

	public void setProgressbar(View progressbar) {
		this.progressbar = progressbar;
	}

}
