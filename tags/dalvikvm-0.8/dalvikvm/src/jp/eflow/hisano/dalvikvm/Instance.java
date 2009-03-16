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

import java.util.Hashtable;

final class Instance {
	final Clazz clazz;

	Object parentInstance;

	private final Hashtable fieldsOfClasses = new Hashtable();

	Instance(final Clazz clazz) {
		this.clazz = clazz;

		Clazz current = clazz;
		do {
			Hashtable fields = new Hashtable();
			Field[] currentFields = current.instanceFields;
			for (int i = 0, length = currentFields.length; i < length; i++) {
				Field field = currentFields[i];
				fields.put(field.name, field.copy());
			}
			fieldsOfClasses.put(current.name, fields);
			current = current.classLoader.loadClass(current.superClass);
		} while (current != null);
	}

	public String toString() {
		return clazz.getName() + "@" + Integer.toHexString(hashCode());
	}

	Field getField(final String className, final String fieldName) {
		String currentClassName = className;
		while (true) {
			Hashtable fields = (Hashtable)fieldsOfClasses.get(currentClassName);
			if (fields == null) {
				return null;
			}
			Field field = (Field)fields.get(fieldName);
			if (field != null) {
				return field;
			}
			Clazz currentClazz = clazz.classLoader.loadClass(currentClassName);
			if (currentClazz == null) {
				return null;
			}
			currentClassName = currentClazz.superClass;
		}
	}
}
