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

package safe;

import java.io.File;

public class KeySafePC extends KeySafeAbstract{

	public KeySafePC(KeyEntity universal_key) {
		super(null, universal_key);
	}

	@Override
	public File fileLocation() {
		String OS = (System.getProperty("os.name")).toUpperCase();
		String workingDir = "";
		if (OS.contains("WIN")){
			workingDir = System.getenv("APPDATA")+"/";
		}
		else {
			workingDir = System.getProperty("user.home")+"/.";
		}
		
		
		if(! new File(workingDir+"Raven").exists()) new File(workingDir+"Raven").mkdir();
		return new File(workingDir+"Raven/keySafe.dat");
	}


}
