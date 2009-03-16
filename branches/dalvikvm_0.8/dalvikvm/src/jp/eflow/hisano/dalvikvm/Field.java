/*
 * Developed by Koji Hisano <koji.hisano@eflow.jp>
 *
 * Copyright (C) 2009 eflow Inc. <http://www.eflow.jp/en/>
 *
 * This file is a part of Android Dalvik VM on Java.
 * http://code.google.com/p/android-dalvik-vm-on-java/
 *
 * This project is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jp.eflow.hisano.dalvikvm;

final class Field {
	final Clazz clazz;
	
	int flag;
	boolean isInstance;

	String name;
	String type;

	int intValue;
	long longValue;
	Object objectValue;
	
	Field(final Clazz clazz) {
		this.clazz = clazz;
	}

	public String toString() {
		String value;
		switch (type.charAt(0)) {
			case 'C':
				value = (char)intValue + " (char)";
				break;
			case 'B':
				value = (byte)intValue + " (byte)";
				break;
			case 'S':
				value = (short)intValue + " (short)";
				break;
			case 'I':
				value = intValue + " (int)";
				break;
			case 'Z':
				value = (intValue != 0) + " (boolean)";
				break;
			case 'J':
				value = longValue + " (long)";
				break;
			case 'L':
				value = objectValue + " (" + type.substring(1, type.length() -1) + ")";
				break;
			case '[':
				value = objectValue + " (" + type + ")";
				break;
			default:
				throw new VirtualMachineException("not supported field type: " + type);
		}
		return clazz.name + "." + name + " = " + value;
	}

	Field copy() {
		Field copy = new Field(clazz);
		copy.flag = flag;
		copy.name = name;
		copy.type = type;
		copy.intValue = intValue;
		copy.longValue = longValue;
		copy.objectValue = objectValue;
		return copy;
	}
}
