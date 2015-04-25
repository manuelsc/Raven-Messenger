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

package at.flack.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SmileyKonverter {

	private HashMap<String, String> img_smiley;
	private HashMap<String, String> smiley_img;
	private HashMap<String, String> fb_smileys;

	public SmileyKonverter() {
		img_smiley = new HashMap<String, String>();
		smiley_img = new HashMap<String, String>();
		fb_smileys = new HashMap<String, String>();
		fillMapsForAndroid();
	}

	private void put(String a, String b) { // img, smiley
		img_smiley.put(a, b);
		smiley_img.put(b, a);
	}

	public String getAndroidSmiley(String smiley) {
		return img_smiley.get(smiley);
	}

	public String getFacebookSmiley(String smiley) {
		return smiley_img.get(smiley);
	}

	@Deprecated
	/**
	 * DONT USE THIS! CALL ChatAPI WITH SmileyMap.ANDROID (=0) IN ORDER TO DISPLAY SMILEYS PROPERLY!!!!
	 * @param s
	 * @return
	 */
	public String parseAndroid(String s) {
		String erg = s;
		erg = erg.replaceAll(Pattern.quote(">:o"), get(Character.toChars(0x1f621)));
		erg = erg.replaceAll(Pattern.quote("sunglasses emoticon"), get(Character.toChars(0x1f60e)));
		for (Map.Entry<String, String> entry : img_smiley.entrySet()) {
			if (s.indexOf("http://") >= 0 || s.indexOf("https://") >= 0)
				continue;
			erg = erg.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
		}

		for (Map.Entry<String, String> entry : fb_smileys.entrySet()) {
			erg = erg.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
		}

		return erg;
	}

	public String parseFacebook(String s) {
		String erg = s;
		for (Map.Entry<String, String> entry : smiley_img.entrySet()) {
			erg = erg.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
		}
		return erg;
	}

	private static String get(char[] t) {
		return new String(t);
	}

	private void fillMapsForAndroid() {
		put(":D", get(Character.toChars(0x1f604)));
		put(":)", get(Character.toChars(0x1f60a)));
		put(":(", get(Character.toChars(0x1f61f)));
		put(":P", get(Character.toChars(0x1f61c)));
		put(":o", get(Character.toChars(0x1f62e)));
		put(";)", get(Character.toChars(0x1f609)));
		put(">:(", get(Character.toChars(0x1f623)));
		put(":/", get(Character.toChars(0x1f615)));
		put(":'(", get(Character.toChars(0x1f622)));
		put("^_^", get(Character.toChars(0x1f606)));
		put("8-)", get(Character.toChars(0x1f60e)));
		put("B|", get(Character.toChars(0x1f60e)));
		put("<3", get(Character.toChars(0x2764)));
		put("3:)", get(Character.toChars(0x1f608)));
		put("O:)", get(Character.toChars(0x1f607)));
		put("-_-", get(Character.toChars(0x1f611)));
		put("o.O", get(Character.toChars(0x1f633)));
		put(">:o", get(Character.toChars(0x1f621)));
		put(":3", get(Character.toChars(0x263a)));
		put("(y)", get(Character.toChars(0x1f44d)));

		// For FB support
		fb_smileys.put("smile emoticon", get(Character.toChars(0x1f60a)));
		fb_smileys.put("frown emoticon", get(Character.toChars(0x1f61f)));
		fb_smileys.put("tongue emoticon", get(Character.toChars(0x1f61c)));
		fb_smileys.put("grin emoticon", get(Character.toChars(0x1f604)));
		fb_smileys.put("gasp emoticon", get(Character.toChars(0x1f62e)));
		fb_smileys.put("wink emoticon", get(Character.toChars(0x1f609)));
		fb_smileys.put("grumpy emoticon", get(Character.toChars(0x1f623)));
		fb_smileys.put("unsure emoticon", get(Character.toChars(0x1f615)));
		fb_smileys.put("cry emoticon", get(Character.toChars(0x1f622)));
		fb_smileys.put("kiki emoticon", get(Character.toChars(0x1f606)));
		fb_smileys.put("sunglasses emoticon", get(Character.toChars(0x1f60e)));
		fb_smileys.put("glasses emoticon", get(Character.toChars(0x1f60e)));
		fb_smileys.put("heart emoticon", get(Character.toChars(0x2764)));
		fb_smileys.put("devil emoticon", get(Character.toChars(0x1f608)));
		fb_smileys.put("angel emoticon", get(Character.toChars(0x1f607)));
		fb_smileys.put("squint emoticon", get(Character.toChars(0x1f611)));
		fb_smileys.put("confused emoticon", get(Character.toChars(0x1f633)));
		fb_smileys.put("upset emoticon", get(Character.toChars(0x1f621)));
		fb_smileys.put("colonthree emoticon", get(Character.toChars(0x263a)));
		fb_smileys.put("like emoticon", get(Character.toChars(0x1f44d)));
	}
}
