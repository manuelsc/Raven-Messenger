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

package meta;

import java.lang.reflect.Field;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Core {
	public static final long VERSION = 19;
	
	public static String PROVIDER = "SC";
			
	public static final String SPONGEY_CASTLE = "SC";
		
	public static final String BOUNCY_CASTLE = "BC";
	
	public static void setProvider(String provider){
		Core.PROVIDER = provider;
		if(Core.PROVIDER.equals(Core.SPONGEY_CASTLE))
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		else
			Security.addProvider(new BouncyCastleProvider());
	}
	
	public static void removeCryptographyRestrictions() {
	    if (!isRestrictedCryptography()) {
	        return;
	    }
	    try {
	        final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
	        final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
	        final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

	        final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
	        isRestrictedField.setAccessible(true);
	        isRestrictedField.set(null, false);

	        final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
	        defaultPolicyField.setAccessible(true);
	        final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

	        final Field perms = cryptoPermissions.getDeclaredField("perms");
	        perms.setAccessible(true);
	       

	        final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
	        instance.setAccessible(true);
	        defaultPolicy.add((Permission) instance.get(null));

	    } catch (final Exception e) {

	    }
	}

	private static boolean isRestrictedCryptography() {
	    return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
	}
}
