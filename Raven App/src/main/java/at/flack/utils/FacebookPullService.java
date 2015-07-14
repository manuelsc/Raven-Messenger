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

package at.flack.utils;

import java.io.IOException;

import json.JSONException;
import json.JSONObject;

import org.apache.http.Header;
import org.json.JSONArray;

import api.ChatAPI;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

public class FacebookPullService {

	private String userAgent;
	private String cookie;
	private int seq;
	private static int sticky_token;
	private static String sticky_pool;
	private static SyncHttpClient client = new SyncHttpClient();
	private static ChatAPI api = new ChatAPI();

	public FacebookPullService(String userAgent, String cookie) {
		this.userAgent = userAgent;
		this.cookie = cookie;

		client.addHeader("Origin", "https://m.facebook.com");
		client.addHeader("User-Agent", userAgent);
		client.addHeader("Content-Type", "application/x-www-form-urlencoded");
		client.addHeader("Accept", "*/*");
		client.addHeader("Referer", "https://m.facebook.com/");
		client.addHeader("Accept-Language", "de-DE,en-US;q=0.8");
		client.addHeader("Cookie", cookie);
		client.setConnectTimeout(54000);
		client.setResponseTimeout(54000);
		client.setTimeout(54000);
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public int getStickyToken() {
		return sticky_token;
	}

	public void setStickyToken(int sticky_token) {
		this.sticky_token = sticky_token;
	}

	public String getStickyPool() {
		return sticky_pool;
	}

	public void setStickyPool(String sticky_pool) {
		this.sticky_pool = sticky_pool;
	}

	public static synchronized void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.get(url, params, responseHandler);
	}

	public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.post(url, params, responseHandler);
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public byte[] downloadImage(String image, final FacebookPullProcessor fpp) throws IOException {
		FacebookPullService.get(image, null, new AsyncHttpResponseHandler() {

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
			}

			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				fpp.process(arg2);
			}
		});
		return null;

	}

	public void getPull(String channel_url, String myID, final FacebookPullProcessor fpp) throws Exception {

		String reqUrl = "https://" + channel_url + ".facebook.com/pull?channel=p_" + myID + "&seq=" + seq
				+ "&profile=mobile&partition=-2&sticky_token=" + sticky_token + "&sticky_pool="
				+ (sticky_pool != null ? sticky_pool : "")
				+ "&cb=&state=active&m_sess=&__dyn=&__req=g&__ajax__=true&__user=" + myID;
		FacebookPullService.get(reqUrl, null, new JsonHttpResponseHandler() {
			@Override
			public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
				getSequenz(errorResponse);

				FacebookPullService.sticky_pool = api.getSticky_pool();
				FacebookPullService.sticky_token = api.getChannel_sticky_token();
				fpp.process(api.parsePull(errorResponse));

			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
			}
		});
	}

	private void getSequenz(String s) {
		int index = s.indexOf("{");
		if (index < 0)
			return;
		s = s.substring(index, s.length());
		try {
			JSONObject o = new JSONObject(s);
			seq = o.getInt("seq");
		} catch (JSONException e) {
			return;
		}
	}

}
