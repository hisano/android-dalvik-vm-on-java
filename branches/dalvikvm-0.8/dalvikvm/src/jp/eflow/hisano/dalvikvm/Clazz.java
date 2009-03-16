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

final class Clazz {
	final ClassLoader classLoader;

	int flag;
	boolean isInterface;
	String name;

	String superClass;
	String[] interfaces;

	Field[] instanceFields;
	Field[] staticFields;
	Hashtable staticFieldMap;

	Method[] directMethods;
	Method[] virtualMethods;

	boolean binded;

	Clazz(final ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public String toString() {
		return (isInterface ? "interface " : "class ") + getName();
	}

	Method getVirtualMethod(final String name, final String descriptor) {
		Clazz current = this;
		do {
			Method[] currentMethods = current.virtualMethods;
			for (int i = 0, length = currentMethods.length; i < length; i++) {
				Method method = currentMethods[i];
				if (name.equals(method.name) && descriptor.equals(method.descriptor)) {
					return method;
				}
			}
			current = classLoader.loadClass(current.superClass);
		} while (current != null);
		return null;
	}

	Method getDirectMethod(final String name, final String descriptor) {
		Method[] currentMethods = directMethods;
		for (int i = 0, length = currentMethods.length; i < length; i++) {
			Method method = currentMethods[i];
			if (name.equals(method.name) && descriptor.equals(method.descriptor)) {
				return method;
			}
		}
		return null;
	}

	Field getStaticField(final String name) {
		return (Field)staticFieldMap.get(name);
	}

	String getName() {
		return name.replace('/', '.');
	}
}
