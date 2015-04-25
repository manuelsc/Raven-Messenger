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

package tools;

public class ByteHelper {

	private byte[] helper; 
	private int offset;
	
	public ByteHelper(byte [] ba){
		helper = ba;
		offset = ba.length;
	}
	
	public ByteHelper(byte [] ... ba){
		init(512, ba);
	}
	
	public ByteHelper(int length, byte [] ... ba){
		init(length, ba);
	}
	
	private void init(int length, byte [] ... ba){
		helper = new byte [length]; 
		offset = 0;
		fill(ba);
	}
	
	private void fill(byte [] ... ba){
		for(byte [] b : ba){
			if(offset + b.length >= helper.length){ init(helper.length*2, ba); return;}
			System.arraycopy(b, 0, helper, offset, b.length);
			offset += b.length;
		}
	}
	
	public byte [] get(){
		byte [] re = new byte [offset];
		System.arraycopy(helper, 0, re, 0, offset);
		return re;
	}
	
	public byte [] get(int off, int length){
		byte [] re = new byte [length];
		System.arraycopy(helper, off, re, 0, length);
		return re;
	}
}
