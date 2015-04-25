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

package mail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Email implements Serializable {
	
	private String sender;
	private String recipient;
	private String subject;
	private Date date;
	private String text;
	private boolean read = false;
	ArrayList<File> files;
	private String senderName = "";
	
	public Email(String sender, String recipient, String subject,
			Date date, String text, ArrayList<File> files){
		int ind = sender.indexOf("<");
		if(ind >= 0){
			int ind2 = sender.indexOf(">");
			if(ind2 > ind){
				this.sender = sender.substring(ind+1, ind2);
				this.senderName = sender.substring(0, ind);
				this.senderName = this.senderName.replaceAll("\"", ""); // remove "" (like used by amazon)
			}
		} else {
			this.sender = sender;
		}		
		this.recipient = recipient;
		this.subject = subject;
		this.date = date;
		this.text = text;
		this.files = files;
	}
	
	public void setRead(){
		this.read = true;
	}
	
	public boolean isRead(){
		return read;
	}
	
	public String getSenderName(){
		return senderName;
	}
	
	public String getSender(){
		return sender;
	}
	
	public String getRecipient(){
		return recipient;
	}
	
	public Date getDate(){
		return date;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public String getText(){
		return text;
	}
	
	public boolean hasFiles(){
		if(files.size() <= 0){
			return false;
		}
		return true;
	}
	
	public ArrayList<File> getFilesFromEmail(){
		return files;
	}
	
	public String toString(){
		return "["+sender+ "| "+senderName+"] ("+subject+ ") "+text; 
	}
	
}
