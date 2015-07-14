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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import safe.KeyEntity;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import api.FacebookContact;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.ProfilePictureCache;
import at.flack.utils.ImageDownloader;

public class NewFbContactActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

	private ListView contactList;
	private ProfilePictureCache fb_img;
	private ArrayList<ContactModel> contacts;
	private ArrayList<FacebookContact> fbcontacts;
	private long lastsubmit = 0;
	private String searchWord;
	private View progressbar, searchBig;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_contact);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		contactList = (ListView) this.findViewById(R.id.listview);
		progressbar = this.findViewById(R.id.load_screen);
		searchBig = this.findViewById(R.id.search_big);
		searchBig.setVisibility(View.VISIBLE);
		TextView padding = new TextView(this);
		padding.setHeight(10);
		contactList.addHeaderView(padding);
		contactList.setHeaderDividersEnabled(false);
		contactList.addFooterView(padding, null, false);
		contactList.setFooterDividersEnabled(false);

		updateContacts(null);
		contactList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (contacts == null) {
					updateContacts(null);
				}
				openMessageActivity(NewFbContactActivity.this, arg2 - 1);
			}

		});

		try {
			fb_img = ProfilePictureCache.getInstance(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.new_contact, menu);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchMenuItem = menu.findItem(R.id.search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		searchView.setIconifiedByDefault(false);
		searchView.setIconified(false);
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setSubmitButtonEnabled(true);
		searchView.setOnQueryTextListener(this);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_message_encrypted: {
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (arg2 < 0 || activity == null || fbcontacts == null)
			return;
		Intent smsIntent = new Intent(activity, FbMessageOverview.class);
		smsIntent.putExtra("CONTACT_ID", fbcontacts.get(arg2).getFbId());
		smsIntent.putExtra("MY_ID", MainActivity.fb_api.getMyID());
		smsIntent.putExtra("CONTACT_NAME", fbcontacts.get(arg2).getName());
		smsIntent.putExtra("CONTACT_TID", fbcontacts.get(arg2).getFbId());
		smsIntent.putExtra("isGroupChat", false);
		try {
			KeyEntity key = KeySafe.getInstance(this).get(fbcontacts.get(arg2).getTid());
			if (key != null)
				smsIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}
		activity.startActivityForResult(smsIntent, 2);
	}

	public void updateContacts(ArrayList<FacebookContact> fbcontacts) {
		if (fbcontacts == null) {
			return;
		}

		for (FacebookContact contact : fbcontacts) {
			contacts.add(new ContactModel(fb_img.get(contact.getName()), contact.getName(), contact.getLastMessage(),
					"", false).drawOnline(false));
		}

		if (contactList != null && contactList.getAdapter() == null) {
			ContactAdapter arrayAdapter = new ContactAdapter(this, contacts, 1);
			contactList.setAdapter(arrayAdapter);
			contactList.setSelection(0);

		} else {
			if (contactList != null) {
				((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
						contacts, 1);
			}
		}

		progressbar.setVisibility(View.INVISIBLE);
	}

	public void searchFriends(String searchTerm) {
		new FacebookSearchFriendsTask().execute(searchTerm);
	}

	class FacebookSearchFriendsTask extends AsyncTask<String, Void, ArrayList<FacebookContact>> {

		private Exception exception;

		@Override
		protected ArrayList<FacebookContact> doInBackground(String... params) {

			if (MainActivity.fb_api == null)
				return null;
			final ArrayList<FacebookContact> temp;
			try {

				temp = MainActivity.fb_api.parseSearchFriends(MainActivity.fb_api.friendSearch(params[0]));
				fbcontacts = temp;
				contacts = new ArrayList<ContactModel>();
				for (FacebookContact fbc : temp) {
					if (!fb_img.contains(fbc.getName()) && fbc.getProfilePicture() != null) {
						fb_img.put(fbc.getName(), ImageDownloader.downloadImage(fbc.getProfilePicture()));
					}
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateContacts(temp);

					}
				});

			} catch (IOException | StringIndexOutOfBoundsException e) {
				this.exception = e;
				e.printStackTrace();
			}

			return null;
		}

	}

	public final Bitmap fetchThumbnail(Context context, Uri uri) {
		if (uri == null)
			return null;
		FileDescriptor fileDescriptor;
		try {
			fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r").getFileDescriptor();
		} catch (FileNotFoundException e) {
			return null;
		}

		if (fileDescriptor != null) {
			return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
		}
		return null;
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		if (System.currentTimeMillis() - lastsubmit > 750) {
			new Thread() {
				public void run() {
					while (true) {
						if (System.currentTimeMillis() - lastsubmit > 750 && searchWord.length() != 0) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									searchBig.setVisibility(View.INVISIBLE);
									progressbar.setVisibility(View.VISIBLE);
								}
							});
							searchFriends(searchWord);
							break;
						}
						try {
							Thread.sleep(750);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

		}
		lastsubmit = System.currentTimeMillis();
		searchWord = arg0;
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		return true;
	}

}
