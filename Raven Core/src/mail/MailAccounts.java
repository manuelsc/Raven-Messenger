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
 
public enum MailAccounts{

    RAVENAPP("imap.world4you.com", 993, "smtp.world4you.com", 587),
    HOTMAIL("imap-mail.outlook.com", 993, "smtp-mail.outlook.com", 587),
    OUTLOOK("imap-mail.outlook.com", 993, "smtp-mail.outlook.com", 587),
    GMAIL("imap.gmail.com", 993, "smtp.gmail.com", 587),
    GMX("imap.gmx.net", 993, "mail.gmx.net", 465),
    YAHOO("imap.mail.yahoo.com", 993, "smtp.mail.yahoo.com", 465)
    ;
     
    private String smtpHost;
    private int smtpport;
    private String host;
    private int port;
    
    private MailAccounts(String host, int port, String smtpHost, int smtpport){
        this.smtpHost = smtpHost;
        this.smtpport = smtpport;
        this.host = host;
        this.port = port;
    }
     
    public String getHost(){
        return host;
    }
    
    public int getPort(){
        return port;
    }
    
    public String getSmtpHost(){
        return smtpHost;
    }
    
    public int getSMTPPort(){
        return smtpport;
    }
     

}