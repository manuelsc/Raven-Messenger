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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import api.ChatAPI;
import api.CurrentTyping;
import api.FacebookContact;
import api.FacebookImageMessage;
import api.FacebookMessage;
import api.FacebookMessageObject;
import at.flack.GoogleAnalyticsTrack;
import at.flack.GoogleAnalyticsTrack.TrackerName;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.exchange.KeySafe;
import at.flack.receiver.FacebookReceiver;
import at.flack.ui.LoadMoreAdapter;
import at.flack.ui.MessageAdapter;
import at.flack.ui.MessageModel;
import at.flack.ui.ProfilePictureCache;
import at.flack.ui.SmileyKonverter;
import at.flack.utils.DHandshakeProcessor;
import at.flack.utils.HandshakeProcessor;
import at.flack.utils.ImageDownloader;
import at.flack.utils.ImageManager;
import at.flack.utils.ImageUrlChecker;
import at.flack.utils.NotificationService;
import encryption.Base64;
import encryption.Message;
import exceptions.KeyAlreadyMappedException;
import exceptions.MessageEncrypterException;
import exchange.ECDHExchange;
import hash.BLAKE512;
import json.JSONException;
import safe.KeyEntity;

public class FbMessageOverview extends NFCActionBarActivity implements EmojiconGridFragment.OnEmojiconClickedListener,
		EmojiconsFragment.OnEmojiconBackspaceClickedListener {

	private String contactName, tid, id;
	private long myId;
	private Bitmap profilePicture, ownProfilePicture;
	private ListView sms_inbox_list;
	private EditText message_text;
	private ImageView emoji;
	private KeyEntity key;
	private ArrayAdapter<MessageModel> arrayAdapter;
	private LinearLayout.LayoutParams params;
	private DisplayMetrics metrics;

	private boolean emojiActive;
	private int page = 0;
	private ArrayList<MessageModel> messages_array;
	private LoadMoreAdapter loadmore;
	private SmileyKonverter smiley_helper;
	private boolean isGroupChat;
	private ProfilePictureCache cache;
	private SharedPreferences sharedPrefs;
	private boolean send_seen;
	private boolean loaded_more = false;
	private boolean just_wrote;
	private ImageUrlChecker iuc;
	private static FacebookReceiver fb_receiver;
	private Resources res;
	private boolean encrypt_messages;
	private View progressbar;
	private ArrayList<FacebookMessageObject> temp;
	private NotificationService notificatinService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_message_overview);
		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		Intent intent = getIntent();
		res = this.getResources();
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		send_seen = sharedPrefs.getBoolean("send_seen", true);
		notificatinService = new NotificationService(this);

		contactName = intent.getStringExtra("CONTACT_NAME");
		tid = intent.getStringExtra("CONTACT_TID");
		id = intent.getStringExtra("CONTACT_ID");
		myId = intent.getLongExtra("MY_ID", 0);
		isGroupChat = intent.getBooleanExtra("isGroupChat", false);
		try {
			cache = ProfilePictureCache.getInstance(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		key = (KeyEntity) intent.getSerializableExtra("CONTACT_KEY");
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;

		progressbar = this.findViewById(R.id.load_screen);
		progressbar.setVisibility(View.VISIBLE);

		profilePicture = cache.get(contactName);
		ownProfilePicture = cache.get("2MyprofilePicture");
		if (!isGroupChat)
			cache = null;

		messages_array = new ArrayList<MessageModel>();

		this.setTitle(contactName);

		sms_inbox_list = (ListView) this.findViewById(R.id.sms_listview);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		loadmore = new LoadMoreAdapter(inflater.inflate(R.layout.message_loadmore, sms_inbox_list, false));

		message_text = (EditText) this.findViewById(R.id.message);
		smiley_helper = new SmileyKonverter();
		final ImageView send = (ImageView) this.findViewById(R.id.sendMessage);
		emoji = (ImageView) this.findViewById(R.id.emoji);
		final View fragment = FbMessageOverview.this.findViewById(R.id.emojicons);
		fragment.setVisibility(View.INVISIBLE);
		params = (LinearLayout.LayoutParams) fragment.getLayoutParams();
		metrics = getBaseContext().getResources().getDisplayMetrics();

		sms_inbox_list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
				if (arg0 == null)
					return;
				if ((MessageModel) arg0.getItemAtPosition(arg2) != null
						&& ((MessageModel) arg0.getItemAtPosition(arg2)).isImage()) {
					final String name = imgParse(((MessageModel) arg0.getItemAtPosition(arg2)).getMessage());
					if (!ImageManager.imageAlreadyThere(name)) {
						new Thread() {
							public void run() {
								Bitmap a = null;
								try {
									if (new ImageUrlChecker().isImageUrl(((MessageModel) arg0.getItemAtPosition(arg2))
											.getMessage())) {
										a = new ImageDownloader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
												((MessageModel) arg0.getItemAtPosition(arg2)).getMessage()).get();
									} else {
										a = new ImageDownloader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
												((MessageModel) arg0.getItemAtPosition(arg2)).getMessage(),
												"attachment", loadCookie()).get();
									}
								} catch (InterruptedException | ExecutionException e) {
									e.printStackTrace();
								} catch (StreamCorruptedException e) {
									e.printStackTrace();
								} catch (FileNotFoundException e) {

									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}

								if (ImageManager.saveImage(FbMessageOverview.this, a, name)) {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											ImageManager.openImageViewer(FbMessageOverview.this, name);
										}
									});
								} else {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(FbMessageOverview.this, "Error while loading picture",
													Toast.LENGTH_SHORT).show();
										}
									});
									ImageManager.removeImage(name);
								}
							}
						}.start();

					} else {
						ImageManager.openImageViewer(FbMessageOverview.this, name);
					}
				}
			}

		});

		sms_inbox_list.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem == 0 && loaded_more) {
				}
			}
		});

		send.setEnabled(!send_seen);
		message_text.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (send_seen)
					send.setEnabled(message_text.getText().toString().length() > 0);
				if (message_text.getText().toString().length() > 0 && !encrypt_messages)
					send.setBackgroundResource(R.drawable.ic_action_send2);
				else if (message_text.getText().toString().length() > 0 && encrypt_messages)
					send.setBackgroundResource(R.drawable.ic_action_send3);
				else
					send.setBackgroundResource(R.drawable.ic_action_send);
			}
		});

		message_text.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (emojiActive) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
						emoji.callOnClick();
					else
						emoji.performClick();
				}
			}
		});

		emoji.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				emojiActive = !emojiActive;
				if (fragment.getVisibility() == View.VISIBLE) {
					params.height = 0;
					fragment.setLayoutParams(params);
					fragment.setVisibility(View.INVISIBLE);
				} else {
					params.height = (int) ((metrics.density * 220) + 0.5f);
					fragment.setLayoutParams(params);
					fragment.setVisibility(View.VISIBLE);
				}
			}
		});

		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = message_text.getText().toString();
				if (message.length() > 0) {
					if (key != null && encrypt_messages && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {
						try {
							Message mes = new Message(message);
							sendFbMessage(mes.encryptedMessage(key), id, tid);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						} catch (MessageEncrypterException e) {
							e.printStackTrace();
						}
						arrayAdapter.add(new MessageModel("me", ownProfilePicture, smiley_helper.parseAndroid(message),
								getDate(System.currentTimeMillis(), null), false, 0));
					} else {
						try {
							sendFbMessage(smiley_helper.parseFacebook(message), id, tid);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
						arrayAdapter.add(new MessageModel("me", ownProfilePicture, smiley_helper.parseAndroid(message),
								getDate(System.currentTimeMillis(), null), false, 1));
						message_text.setText("");
					}
					just_wrote = true;
					message_text.setText("");
					sms_inbox_list.setAdapter(arrayAdapter);
					sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
				} else {
					if (!send_seen) {
						changeReadStatus(!id.equals("") ? id : tid);
						send_seen = true;
					}
				}
			}
		});

		if (send_seen) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					changeReadStatus(!id.equals("") ? id : tid);
				}
			}.start();

		}

		sms_inbox_list.addHeaderView(loadmore.getView());

		loadmore.getView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (loadmore.isEnabled()) {
					loaded_more = true;
					loadmore.setEnabled(false);
					loadMore();
				}
			}
		});
		sms_inbox_list.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

		runNFC();
		Tracker t = ((GoogleAnalyticsTrack) this.getApplication()).getTracker(TrackerName.APP_TRACKER);
		t.setScreenName("Facebook Message Overview");
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("contactName", contactName);
		savedInstanceState.putString("tid", tid);
		savedInstanceState.putString("id", id);
		savedInstanceState.putLong("myId", myId);
		savedInstanceState.putSerializable("key", key);
		savedInstanceState.putInt("page", page);
		savedInstanceState.putBoolean("isGroupChat", isGroupChat);
		savedInstanceState.putBoolean("encrypt_messages", encrypt_messages);
		super.onSaveInstanceState(savedInstanceState);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		contactName = savedInstanceState.getString("contactName");
		tid = savedInstanceState.getString("tid");
		id = savedInstanceState.getString("id");
		myId = savedInstanceState.getLong("myId");
		key = (KeyEntity) savedInstanceState.getSerializable("key");
		page = savedInstanceState.getInt("page");
		isGroupChat = savedInstanceState.getBoolean("isGroupChat");
		encrypt_messages = savedInstanceState.getBoolean("encrypt_messages");
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) { // QR Code Activity (return QR code from scan)
			if (resultCode == RESULT_OK) {
				if (data.getByteArrayExtra("QR_RETURNED_Bytes") != null) {
					new HandshakeProcessor(this).processReceivedExchangeInformation(data, this, myId + "");
					reloadAfterKeyExchange();
				}
				if (data.getBooleanExtra("request_auto_exchange", false) == true) {
					if (KeySafe.getInstance(this).contains(tid)) {
						Dialog dialog = new Dialog(this, res.getString(R.string.popup_keyalreadymapped_title), res
								.getString(R.string.popup_dh_key_already_mapped));
						dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								finishDHHandshake(true);
							}
						});
						dialog.setOnCancelButtonClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								return;
							}
						});
						dialog.show();

					} else {
						finishDHHandshake(false);
					}
				}
				if (data.getBooleanExtra("reload", false) == true) {
					reloadAfterKeyExchange();
				}
			}
		} else if (requestCode == 2) { // After KeyInformationActivity
			if (resultCode == RESULT_OK) {
				if (data.getBooleanExtra("reload", false)) {
					reloadAfterKeyExchange();
					FbMessageOverview.this.getSharedPreferences(tid, Context.MODE_PRIVATE).edit().putLong("key_delted",
							System.currentTimeMillis()).apply();
					finish();
				}
			}
		}
	}

	private void finishDHHandshake(boolean force) {
		try {
			ECDHExchange dh = new ECDHExchange();
			try {
				sendFbMessage("%" + dh.getEncodedPublicKey(), id, tid);
				KeySafe.getInstance(this)
						.put(tid,
								new KeyEntity(dh.getPrivateKey(), null, System.currentTimeMillis(),
										KeyEntity.ECDH_PRIVATE_KEY), force);
				KeySafe.getInstance(this).save();
			} catch (InterruptedException | ExecutionException e) {
				Toast.makeText(this, "ERROR: Can't exchange keys :(", Toast.LENGTH_LONG).show();
			} catch (KeyAlreadyMappedException e) {
				Toast.makeText(this, "ERROR: Can't save ECDH Key", Toast.LENGTH_LONG).show();
			}
		} catch (InvalidKeyException | IllegalStateException | InvalidAlgorithmParameterException
				| NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
			Toast.makeText(this, "ERROR: ECDH Exception", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	@Override
	public void onEmojiconClicked(Emojicon emojicon) {
		EmojiconsFragment.input(message_text, emojicon);
	}

	@Override
	public void onEmojiconBackspaceClicked(View v) {
		EmojiconsFragment.backspace(message_text);
	}

	public void changeTypingStatus(long myid, long userid, boolean fromMobile, int status) {
		android.support.v7.app.ActionBar ac = getSupportActionBar();
		if (ac == null || !(userid + "").equals(id))
			return;
		if (status == CurrentTyping.IS_WRITING) {
			if (!isGroupChat)
				ac.setSubtitle(this.getResources().getString(R.string.fb_is_writing));
			else
				ac.setSubtitle(this.getResources().getString(R.string.fb_is_writing_group));
		} else if (status == CurrentTyping.IS_NOT_WRITING)
			ac.setSubtitle("");

	}

	public void changeReadReceipt(long time, long reader) {
		if ((reader + "").equals(id)) {
			arrayAdapter.getItem(arrayAdapter.getCount() - 1).setDate(
					this.getResources().getString(R.string.message_seen) + " "
							+ new SimpleDateFormat("HH:mm").format(new Date(time)));
			sms_inbox_list.setAdapter(arrayAdapter);
			sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
		}
	}

	public void cancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	public void addNewMessage(FacebookMessageObject mes) throws Exception {
		if (iuc == null)
			iuc = new ImageUrlChecker();
		if (arrayAdapter == null || mes == null)
			return;
		if (!mes.getTid().startsWith(URLDecoder.decode(tid))) {
			Log.d("tidmatcher", mes.getTid() + " does not match " + tid);
			return;

		}
		if (just_wrote && mes.getId() == myId) {
			Log.d("tidmatcher", "It's a-me");
			just_wrote = false;
			return;
		}
		MessageModel model = null;
		if (key != null) {
			if (mes instanceof FacebookMessage)
				model = decryptMessages(((FacebookMessage) mes).getName(), ((FacebookMessage) mes).getMessage(), System
						.currentTimeMillis(), (((FacebookMessage) mes).getId() == myId) ? 0 : 1,
						((FacebookMessage) mes).getReadReceipt());
			if (mes instanceof FacebookImageMessage)
				model = decryptImageMessage(((FacebookImageMessage) mes).getName(), ((FacebookImageMessage) mes)
						.getImage(), ((FacebookImageMessage) mes).getPreview(), System.currentTimeMillis(),
						(((FacebookImageMessage) mes).getId() == myId) ? 0 : 1, ((FacebookImageMessage) mes)
								.getReadReceipt());

		} else {
			if (mes instanceof FacebookMessage)
				model = addMessage(((FacebookMessage) mes).getName(), ((FacebookMessage) mes).getMessage(), System
						.currentTimeMillis(), (((FacebookMessage) mes).getId() == myId) ? 0 : 1,
						((FacebookMessage) mes).getReadReceipt());
			if (mes instanceof FacebookImageMessage)
				model = addImageMessage(((FacebookImageMessage) mes).getName(),
						((FacebookImageMessage) mes).getImage(), ((FacebookImageMessage) mes).getPreview(), System
								.currentTimeMillis(), (((FacebookImageMessage) mes).getId() == myId) ? 0 : 1,
						((FacebookImageMessage) mes).getReadReceipt(), 1);
		}

		ArrayList<MessageModel> ta = new ArrayList<MessageModel>();
		ta.add(model);
		searchForHandshake(ta);
		if (ta.size() >= 1)
			arrayAdapter.add(ta.get(0));
		sms_inbox_list.setAdapter(arrayAdapter);
		sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);

		new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				cancelNotification(FbMessageOverview.this, 8);
			}
		}.start();
		if (send_seen && mes.getId() != myId) {
			new Thread() {
				public void run() {
					changeReadStatus(!id.equals("") ? id : tid);
				}
			}.start();
		}
	}

	public void fillListWithMessages(int page, boolean clearfirst, long time) throws Exception {
		try {
			getFbMessages(tid, page, clearfirst, time);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		} catch (ExecutionException e) {
			e.printStackTrace();
			return;
		}

	}

	public void fillListWithMessagesAfterLoading(ArrayList<FacebookMessageObject> messages) throws Exception {
		ArrayList<MessageModel> temp = new ArrayList<MessageModel>();
		iuc = new ImageUrlChecker();
		if (messages != null) {
			MessageModel te = null;
			if (key != null) {
				for (int i = 0; i < messages.size(); i++) {
					if (messages.get(i) instanceof FacebookMessage)
						te = decryptMessages(messages.get(i).getName(), ((FacebookMessage) messages.get(i))
								.getMessage(), ((FacebookMessage) messages.get(i)).getTime(),
								(messages.get(i).getId() == myId) ? 0 : 1, messages.get(i).getReadReceipt());
					if (messages.get(i) instanceof FacebookImageMessage)
						te = decryptImageMessage(messages.get(i).getName(), ((FacebookImageMessage) messages.get(i))
								.getImage(), ((FacebookImageMessage) messages.get(i)).getPreview(),
								((FacebookImageMessage) messages.get(i)).getTime(),
								(messages.get(i).getId() == myId) ? 0 : 1, messages.get(i).getReadReceipt());
					if (te != null)
						temp.add(te);
				}
			} else {
				for (int i = 0; i < messages.size(); i++) {
					if (messages.get(i) instanceof FacebookMessage)
						te = addMessage(messages.get(i).getName(), ((FacebookMessage) messages.get(i)).getMessage(),
								((FacebookMessage) messages.get(i)).getTime(), (messages.get(i).getId() == myId) ? 0
										: 1, messages.get(i).getReadReceipt());
					if (messages.get(i) instanceof FacebookImageMessage)
						te = addImageMessage(messages.get(i).getName(), ((FacebookImageMessage) messages.get(i))
								.getImage(), ((FacebookImageMessage) messages.get(i)).getPreview(),
								((FacebookImageMessage) messages.get(i)).getTime(),
								(messages.get(i).getId() == myId) ? 0 : 1, messages.get(i).getReadReceipt(), 1);
					if (te != null)
						temp.add(te);
				}
			}
		}
		searchForHandshake(temp);
		if (sms_inbox_list.getFooterViewsCount() == 0) {
			TextView padding = new TextView(this);
			padding.setHeight(15);
			sms_inbox_list.addFooterView(padding);
		}

		if (progressbar != null)
			progressbar.setVisibility(View.INVISIBLE);

		messages_array.addAll(0, temp);

		arrayAdapter = new MessageAdapter(this, messages_array);
		sms_inbox_list.setAdapter(arrayAdapter);

		if (page == 0)
			sms_inbox_list.setSelection(arrayAdapter.getCount() - 1);
		else
			sms_inbox_list.setSelection(7);

		loadmore.setEnabled(true);
	}

	public void loadMore() {
		page++;
		try {
			if(temp != null && temp.size() > 1) {
				//Log.d("timeisprecious","works | "+((FacebookMessage)temp.get(0)).getTime()+" | "+((FacebookMessage)temp.get(0)).getMessage());
				fillListWithMessages(page, false, ((FacebookMessage)temp.get(0)).getTime());
			}else {
				//Log.d("timeisprecious","current");
				fillListWithMessages(page, false, System.currentTimeMillis());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class FacebookMessageTask extends AsyncTask<Container, Void, ArrayList<FacebookMessageObject>> {

		private Exception exception;

		@Override
		protected ArrayList<FacebookMessageObject> doInBackground(Container... params) {
			try {
				if (messages_array != null && params[0].clearfirst)
					messages_array = new ArrayList<MessageModel>();
				temp = params[0].api.parseMessages(params[0].api.getMessages(params[0].tid, params[0].page, params[0].time));

			} catch (IOException | JSONException e) {
				this.exception = e;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return temp;
		}

		@Override
		protected void onPostExecute(ArrayList<FacebookMessageObject> result) {
			try {
				fillListWithMessagesAfterLoading(temp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	class FacebookSendMessageTask extends AsyncTask<Container, Void, Boolean> {

		private Exception exception;

		@Override
		protected Boolean doInBackground(Container... params) {
			boolean bu = false;
			try {
				params[0].api.sendMessage(params[0].message, params[0].id, params[0].tid);
				bu = true;
			} catch (Exception e) {
				this.exception = e;
				e.printStackTrace();
			}
			return bu;
		}
	}

	class FacebookSendRead extends AsyncTask<String, Void, ArrayList<FacebookContact>> {

		private Exception exception;

		@Override
		protected ArrayList<FacebookContact> doInBackground(String... params) {
			ArrayList<FacebookContact> temp = null;
			try {
				if (MainActivity.fb_api != null)
					MainActivity.fb_api.changeReadStatus(params[0]);
			} catch (Exception e) {
				this.exception = e;
				e.printStackTrace();
			}
			return temp;
		}
	}

	public void getFbMessages(String tid, int page, boolean clearfirst, long time) throws InterruptedException, ExecutionException {
		new FacebookMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Container(MainActivity.fb_api,
				tid, page, clearfirst, time));
	}

	public void sendFbMessage(String message, String id, String tid) throws InterruptedException, ExecutionException {
		new FacebookSendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new Container(
				MainActivity.fb_api, message, id, tid)));
	}

	public void changeReadStatus(String s) {
		new FacebookSendRead().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, s);
	}

	private MessageModel decryptMessages(String name, String mess, long date, long type, String read_receipt)
			throws Exception {
		if (mess == null)
			return null;
		Message mes = new Message(mess);
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		if (isGroupChat && isNotMe)
			picture = cache.get(name);
		name = isNotMe ? name : null;
		String date_s = (read_receipt == null || read_receipt.equals("")) ? getDate(date, name) : read_receipt;
		if (Base64.isBase64(mes.toString())) {
			try {
				String msg_dec = mes.decryptedMessage(key);
				if (iuc == null)
					iuc = new ImageUrlChecker();
				if (iuc.isImageUrl(msg_dec))
					return addImageMessage(name, msg_dec, null, date, type, read_receipt, 0);
				else
					return new MessageModel(name, picture, msg_dec, date_s, isNotMe, 0);
			} catch (Exception e) {
				return new MessageModel(name, picture, mes.toString(), date_s, isNotMe, 2);
			}
		}

		return addMessage(name, mess, date, type, read_receipt);
	}

	protected void reloadAfterKeyExchange() {
		if (KeySafe.getInstance(this).contains(tid)) {
			key = KeySafe.getInstance(this).get(tid);
		}
		encrypt_messages = key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY;
		this.invalidateOptionsMenu();
	}

	private MessageModel decryptImageMessage(String name, String img, String preview, long date, long type,
			String read_receipt) throws Exception {
		if (img == null)
			return null;
		Message mes = new Message(img);
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		if (isGroupChat && isNotMe)
			picture = cache.get(name);
		name = isNotMe ? name : null;
		String date_s = (read_receipt == null || read_receipt.equals("")) ? getDate(date, name) : read_receipt;
		if (Base64.isBase64(mes.toString())) {
			try {
				return new MessageModel(name, picture, mes.decryptedMessage(key), null, date_s, isNotMe, 0);
			} catch (Exception e) {
				return new MessageModel(name, picture, mes.toString(), null, date_s, isNotMe, 2);
			}
		}
		String n2 = imgParse(img);
		if (ImageManager.imageAlreadyThere(n2)) {
			return new MessageModel(name, picture, img, ImageManager.getSavedImagePreview(n2), date_s, isNotMe, 1);
		} else
			return new MessageModel(name, picture, img, null, date_s, isNotMe, 1);
	}

	private MessageModel addMessage(String name, String mes, long date, long type, String read_receipt)
			throws Exception {
		if (mes == null)
			return null;
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		if (isGroupChat && isNotMe)
			picture = cache.get(name);
		String date_s = (read_receipt == null || read_receipt.equals("")) ? getDate(date, name) : read_receipt;
		name = isNotMe ? name : null;
		if (iuc == null)
			iuc = new ImageUrlChecker();
		if (iuc.isImageUrl(mes.toString()))
			return addImageMessage(name, mes.toString(), null, date, type, read_receipt, 1);
		return new MessageModel(name, picture, smiley_helper.parseAndroid(mes.toString()), date_s, isNotMe, 1);
	}

	private MessageModel addImageMessage(String name, String img, String preview, long date, long type,
			String read_receipt, int encryptiontype) throws Exception {
		boolean isNotMe = (type == 1);
		Bitmap picture = isNotMe ? profilePicture : ownProfilePicture;
		if (isGroupChat && isNotMe)
			picture = cache.get(name);
		String date_s = (read_receipt == null || read_receipt.equals("")) ? getDate(date, name) : read_receipt;
		name = isNotMe ? name : null;
		String n2 = imgParse(img);
		if (ImageManager.imageAlreadyThere(n2)) {
			return new MessageModel(name, picture, img, ImageManager.getSavedImagePreview(n2), date_s, isNotMe,
					encryptiontype);
		} else
			return new MessageModel(name, picture, img, null, date_s, isNotMe, encryptiontype);
	}

	public void searchForHandshake(ArrayList<MessageModel> erg) {
		MessageModel mm;
		int look_depth = erg.size() - 1;
		if (look_depth < 0)
			look_depth = 0;
		boolean showing_key_exchanged_notification = false;
		for (int i = erg.size() - 1; i >= 0; i--) {
			mm = erg.get(i);
			if (mm == null)
				continue;
			if (mm.getMessage().length() > 0 && mm.getMessage().charAt(0) == '%'
					&& (mm.getMessage().length() == 10 || mm.getMessage().length() == 9)) {
				if (Base64.isPureBase64(mm.getMessage().substring(1, 9))) {
					String str_dec = new String(Base64.decode(mm.getMessage().substring(1, 9), Base64.NO_WRAP));
					if (MainActivity.tempSafe.containsKey(str_dec) && i >= look_depth) {
						Intent returnInten = new Intent();
						returnInten.putExtra("ADD_NEW_KEY_CONFIRMATIONCODE", str_dec);
						returnInten.putExtra("ADD_NEW_KEY_TELEPHONNUMBER", tid);
						HandshakeProcessor hsp = new HandshakeProcessor(this);
						hsp.processReceivedRandomConfirmationCode(returnInten);
						reloadAfterKeyExchange();
					} else {
						erg.remove(i);
						erg.add(i, new MessageModel(res.getString(R.string.message_handshake_completed), true));
						break;
					}
				}
			}

			// Diffie Hellman Exchange
			if (mm.getMessage().length() >= 120
					&& mm.getMessage().length() <= 125
					&& mm.getMessage().charAt(0) == '%'
					&& !FbMessageOverview.this.getSharedPreferences(tid, Context.MODE_PRIVATE).getString("key_pub", "")
							.equals(mm.getMessage())) { // DH Exchange!
				Log.d("dhexchange", "stage 1");
				if (Base64.isPureBase64(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=")))) {
					Log.d("dhexchange", "stage 2");
					if ((key == null || key.getVersion() == KeyEntity.ECDH_PRIVATE_KEY) && mm.isNotMe() && page == 0
							&& i >= look_depth) {
						new DHandshakeProcessor(this, tid) {
							@Override
							public void sendHandshake(String handshaketext) {
								try {
									sendFbMessage(handshaketext, id, tid);
								} catch (InterruptedException e) {
									e.printStackTrace();
								} catch (ExecutionException e) {
									e.printStackTrace();
								}
							}
						}.processDHExchange(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=") + 1));
						reloadAfterKeyExchange();
						FbMessageOverview.this.getSharedPreferences(tid, Context.MODE_PRIVATE).edit().putString(
								"key_pub", mm.getMessage()).apply();
					}

				}
			} else if (FbMessageOverview.this.getSharedPreferences(tid, Context.MODE_PRIVATE).getString("key_pub", "")
					.equals(mm.getMessage())) {
				break;
			}
		}

		for (int i = erg.size() - 1; i >= 0; i--) {
			mm = erg.get(i);
			if (mm == null || mm.getMessage() == null)
				continue;
			if (mm.getMessage().length() >= 120 && mm.getMessage().length() <= 125 && mm.getMessage().charAt(0) == '%') {
				if (Base64.isPureBase64(mm.getMessage().substring(1, mm.getMessage().lastIndexOf("=")))) {
					if (showing_key_exchanged_notification)
						erg.remove(i);
					if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY
							&& !showing_key_exchanged_notification) {
						showing_key_exchanged_notification = true;
						erg.remove(i);
						erg.add(i, new MessageModel(res.getString(R.string.message_handshake_completed), true));
					} else {
						if (!showing_key_exchanged_notification && i < look_depth) {
							showing_key_exchanged_notification = true;
							erg.remove(i);
							erg.add(i, new MessageModel(res.getString(R.string.message_handshake_initiated), true));
						}
					}
				}
			}
		}

	}

	public String imgParse(String name) {
		String erg = "";
		try {
			erg = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".")) + ".jpg";
		} catch (StringIndexOutOfBoundsException e) {
			try {
				erg = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".") + 4);
			} catch (StringIndexOutOfBoundsException e2) {
				e2.printStackTrace();
			}
		}
		return erg.toLowerCase(Locale.getDefault());
	}

	public String getDate(long date, String name) {
		long cur = System.currentTimeMillis();
		if (!isGroupChat || name == null) {
			StringBuilder erg;
			if ((long) (cur / 86400000) == (long) (date / 86400000))
				erg = new StringBuilder(res.getString(R.string.message_overview_date_today) + " ")
						.append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
			else if (date >= cur - 518400000)
				erg = new StringBuilder(new SimpleDateFormat("EE HH:mm", Locale.getDefault()).format(date));
			else
				erg = new StringBuilder(new SimpleDateFormat("dd. MMMM HH:mm", Locale.getDefault()).format(date));
			return erg.toString();
		} else {
			StringBuilder erg = new StringBuilder(name).append(", ");
			if ((long) (cur / 86400000) == (long) (date / 86400000))
				erg = erg.append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date));
			else if (date >= cur - 518400000)
				erg = erg.append(new SimpleDateFormat("EE HH:mm", Locale.getDefault()).format(date));
			else
				erg = erg.append(new SimpleDateFormat("dd. MMMM HH:mm", Locale.getDefault()).format(date));
			return erg.toString();
		}
	}

	public String printText(String s) {
		String erg = "";
		for (int i = 0; i < s.length(); i++) {
			erg += (byte) (s.charAt(i)) + ";";
		}
		return erg;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SharedPreferences prefs = getSharedPreferences("app", 0);
		boolean isDark = "Dark".equals(prefs.getString("theme", "Dark"));
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_overview, menu);

		MenuItem item_encrypted = menu.findItem(R.id.action_message_encrypted);
		MenuItem item_unencrypted = menu.findItem(R.id.action_qr);
		MenuItem phone = menu.findItem(R.id.action_phone_number);
		MenuItem mail = menu.findItem(R.id.action_mail_reply);
		MenuItem key_info = menu.findItem(R.id.action_key_information);
		if (key == null) {
			item_encrypted.setVisible(false);
			key_info.setVisible(false);
		} else if (key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY) {

			item_unencrypted.setVisible(false);
			key_info.setVisible(true);

			if (encrypt_messages)
				item_encrypted.setIcon(R.drawable.ic_action_message_encrypted_light);
			else
				item_encrypted.setIcon(R.drawable.ic_not_encrypted_white);
		} else {
			item_encrypted.setVisible(false);
			key_info.setVisible(false);
		}

		phone.setVisible(false);
		mail.setVisible(false);

		menu.findItem(R.id.action_message_encrypted).setIcon(
				isDark ? R.drawable.ic_action_message_encrypted_light : R.drawable.ic_action_message_encrypted);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		case R.id.action_qr: {
			if (isGroupChat) {
				Toast.makeText(this, "Sorry, encrypted group chats are not supported right now", Toast.LENGTH_LONG)
						.show();
				return true;
			}
			Intent qr = new Intent(FbMessageOverview.this, QRActivity.class);
			qr.putExtra("forfb", myId + "");
			qr.putExtra("fbuser", tid);
			qr.putExtra("otherID", tid);
			qr.putExtra("myID", myId);
			qr.putExtra("type", NFCActionBarActivity.HANDSHAKE_TYPE.FACEBOOK_HANDSHAKE);
			startActivityForResult(qr, 1);

			return true;
		}
		case R.id.action_key_information: {
			Intent keyinfo = new Intent(FbMessageOverview.this, KeyInformationActivity.class);
			keyinfo.putExtra("hashed_key", new BLAKE512().digest(key.getBothKeys()));
			keyinfo.putExtra("creationdate", key.getTimeStamp());
			keyinfo.putExtra("algo", key.getVersion());
			keyinfo.putExtra("primary", tid);
			startActivityForResult(keyinfo, 2);

			return true;
		}
		case R.id.action_message_encrypted: {
			encrypt_messages = !encrypt_messages;
			if (encrypt_messages) {
				item.setIcon(R.drawable.ic_action_message_encrypted_light);
			} else {
				item.setIcon(R.drawable.ic_not_encrypted_white);
			}
			message_text.setText(message_text.getText());
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	public String loadCookie() throws StreamCorruptedException, FileNotFoundException, IOException {
		ObjectInputStream inputStream = null;
		String erg = null;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(new File(this.getFilesDir(), "cookie.dat")));
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

	public void onResume() {
		super.onResume();
		if (notificatinService != null)
			notificatinService.startTempService();
		fb_receiver = new FacebookReceiver();
		fb_receiver.setMainActivityHandler(this);
		registerReceiver(fb_receiver, new IntentFilter("at.flack.receiver.FacebookReceiver"));
		if (MainActivity.fb_api == null) {
			finish();
		}

		page = 0;
		try {
			fillListWithMessages(0, true, System.currentTimeMillis());
		} catch (Exception e) {
			finish();
		}
		reloadAfterKeyExchange();

	}

	@Override
	public void onBackPressed() {
		if (emojiActive && emoji != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
				emoji.callOnClick();
			else
				emoji.performClick();
		} else
			finish();
	}

	public void onPause() {
		super.onPause();
		if (notificatinService != null)
			notificatinService.stopNotificationService();
		unregisterReceiver(fb_receiver);
	}

	@Override
	public HANDSHAKE_TYPE initNFCActivity() {
		setMyID(myId + "");
		setOtherID(id);
		return NFCActionBarActivity.HANDSHAKE_TYPE.FACEBOOK_HANDSHAKE;
	}

}

class Container {
	public ChatAPI api;
	public String tid;
	public int page;
	public long time;
	public String id;
	String message;
	public boolean clearfirst;

	public Container(ChatAPI api, String tid, int page, boolean clearfirst, long time) {
		this.api = api;
		this.tid = tid;
		this.page = page;
		this.clearfirst = clearfirst;
		this.time = time;
	}

	public Container(ChatAPI api, String message, String id, String tid) {
		this.api = api;
		this.tid = tid;
		this.id = id;
		this.message = message;
	}

}
