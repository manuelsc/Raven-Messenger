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
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.util.BASE64DecoderStream;

public class MailControlAndroid {

	private static ArrayList<MailProfile> mps = new ArrayList<MailProfile>();
	
	public static void receiveEmailIMAPAndroid(MailProfile mailprofile, String folder) throws Exception{
		receiveEmailIMAPAndroid(mailprofile, folder, 0, 25);
	}
	
	public static void receiveEmailPOPAndroid(MailProfile mailprofile, String folder) throws Exception{
		receiveEmailPOPAndroid(mailprofile, folder, 0, 25);
	}
	
    public static void sendMailAndroid(final MailProfile mailProfile, String recipient, String subject, String text) throws Exception {  
    	Properties props = new Properties();   
        props.setProperty("mail.transport.protocol", "smtp");   
        props.setProperty("mail.host", mailProfile.getSmtpHost());   
        props.put("mail.smtp.auth", "true");   
        props.put("mail.smtp.port", mailProfile.getSmtpPort());   
        
        try{
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailProfile.getUsername(), mailProfile.getPassword());
			}
		  });
        MimeMessage message = new MimeMessage(session);   
        DataHandler handler = new DataHandler(new ByteArrayDataSource(text.getBytes(), "text/plain"));  
        message.setSender(new InternetAddress(mailProfile.getEmail()));   
        message.setSubject(subject);   
        message.setFrom(new InternetAddress(mailProfile.getEmail()));
        message.setDataHandler(handler);   
        if (recipient.indexOf(',') > 0)   
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));   
        else  
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));   
        Transport.send(message);   
        }catch(Exception e){
        	e.printStackTrace();
        }
    }   
	
	public static void receiveEmailPOPAndroid(MailProfile mailprofile, String folder, int offset, int limit) throws Exception{
		Properties props = new Properties();
        props.setProperty("mail.store.protocol", "pop3");
        props.put("mail.pop3.port", mailprofile.getPop3Port());   
	        props.setProperty("mail.pop3.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
	        props.setProperty("mail.pop3.socketFactory.fallback",
					"false");
	        props.setProperty("mail.pop3.port", "" + mailprofile.getPop3Port());
	        props.setProperty("mail.pop3.socketFactory.port", ""
					+ mailprofile.getPop3Port());
	        
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(mailprofile.getPop3Host(), mailprofile.getEmail(), mailprofile.getPassword());
            Folder inbox = store.getFolder(folder);
            inbox.open(Folder.READ_ONLY); 
            if(limit > inbox.getMessageCount()) limit = inbox.getMessageCount()-1;
            javax.mail.Message[] msg = inbox.getMessages(inbox.getMessageCount()-offset-limit, inbox.getMessageCount()-offset);
            String content = null;
            javax.mail.Message m;
            try{
	            for(int i=msg.length-1; i >= 0; i--){
	            	m = msg[i];
	            	 Object msgContent = m.getContent();                   
	            	     if (msgContent instanceof Multipart) {
	            	         Multipart multipart = (Multipart) msgContent;
	            	         for (int j = 0; j < multipart.getCount(); j++) {
	            	         BodyPart bodyPart = multipart.getBodyPart(j);
	            	         String disposition = bodyPart.getDisposition();
	            	         if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) { 
	            	              DataHandler handler = bodyPart.getDataHandler();                 
	            	          }
	            	          else { 
	            	        	  if(bodyPart instanceof IMAPBodyPart){
		            	                 content = ((IMAPBodyPart)bodyPart).getContent().toString();  // the changed code 
		            	                 if(((IMAPBodyPart)bodyPart).getContent() instanceof MimeMultipart){
		            	                	 Multipart multi2 = (Multipart) ((IMAPBodyPart)bodyPart).getContent();
		            	                	 for (int k = 0; k < multi2.getCount(); k++) 
		            	                		 content =multi2.getBodyPart(k).getContent().toString();
		            	        		}
		            	        	}    
	            	            }
	            	        }
	            	     }
	            	     else                
	            	         content= m.getContent().toString();
	            	
	            		if(m.getContentType().startsWith("com.sun.mail.util.BASE64DecoderStream"))
	            			content = ((BASE64DecoderStream) m.getContent()).toString();
		            	mailprofile.addReceivedMessage(
		            			new Email(
		            					MimeUtility.decodeText(m.getFrom()[0].toString()), 
		            					MimeUtility.decodeText(m.getAllRecipients()[0].toString()), 
		            					MimeUtility.decodeText(m.getSubject()), m.getReceivedDate(), 
		            					content, 
		            					new ArrayList<File>()
		            			)
		            	);
	            }
            } catch(Exception e){}
            finally{
            	if(inbox != null)
            		inbox.close(true);
            	if(store != null)
            		store.close();
            }
    
	}
	
	public static void receiveEmailIMAPAndroid(MailProfile mailprofile, String folder, int offset, int limit) throws Exception{
		Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.put("mail.imap.port", mailprofile.getImapPort());   
        	props.setProperty("mail.imap.socketFactory.class",
   					"javax.net.ssl.SSLSocketFactory");
   	        props.setProperty("mail.imap.socketFactory.fallback",
   					"false");
   	        props.setProperty("mail.imap.port", "" + mailprofile.getImapPort());
   	        props.setProperty("mail.imap.socketFactory.port", ""
   					+ mailprofile.getImapPort());

            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(mailprofile.getImapHost(), mailprofile.getEmail(), mailprofile.getPassword());
            Folder inbox = store.getFolder(folder);
            inbox.open(Folder.READ_ONLY); 
            if(limit > inbox.getMessageCount()) limit = inbox.getMessageCount()-1;
            javax.mail.Message[] msg = inbox.getMessages(inbox.getMessageCount()-offset-limit, inbox.getMessageCount()-offset);
            String content;
            javax.mail.Message m;
            try{
	            for(int i=msg.length-1; i >= 0; i--){
	            	m = msg[i];
	            		content = m.getContent().toString();           		
	            		 Object msgContent = m.getContent();
                   
	            	     if (msgContent instanceof Multipart) {
	            	         Multipart multipart = (Multipart) msgContent;
	            	         for (int j = 0; j < multipart.getCount(); j++) {
	            	         BodyPart bodyPart = multipart.getBodyPart(j);
	            	         String disposition = bodyPart.getDisposition();
	            	         if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) { 
	            	             DataHandler handler = bodyPart.getDataHandler();                    
	            	          }
	            	          else { 
	            	        	if(bodyPart instanceof IMAPBodyPart){
	            	                 content = ((IMAPBodyPart)bodyPart).getContent().toString();  // the changed code 
	            	                 if(((IMAPBodyPart)bodyPart).getContent() instanceof MimeMultipart){
	            	                	 Multipart multi2 = (Multipart) ((IMAPBodyPart)bodyPart).getContent();
	            	                	 for (int k = 0; k < multi2.getCount(); k++) 
	            	                		 content =multi2.getBodyPart(k).getContent().toString();
	            	        		}
	            	        	}
	            	        	
	            	            }
	            	        }
	            	     }
	            	    
	            	     else                
	            	         content= m.getContent().toString();
	            		if(content.startsWith("com.sun.mail.util.BASE64DecoderStream")){
	            			
	            		}
		            	mailprofile.addReceivedMessage(
		            			new Email(
		            					MimeUtility.decodeText(m.getFrom()[0].toString()), 
		            					MimeUtility.decodeText(m.getAllRecipients()[0].toString()), 
		            					MimeUtility.decodeText(m.getSubject()), m.getReceivedDate(), 
		            					content, 
		            					new ArrayList<File>()
		            			)
		            	);
	           
	            } 
            } catch(Exception e){e.printStackTrace();} 
            finally{
            	if(inbox != null)
            		inbox.close(true);
            	if(store != null)
            		store.close();
            	
            	
            }
    
	}
	

}
