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

final class Utils {
	static void setLong(final int[] ints, final int offset, final long value) {
		ints[offset] = (int)(value & 0xFFFFFFFF);
		ints[offset + 1] = (int)(value >>> 32);
	}

	static long getLong(final int[] ints, final int offset) {
		return (ints[offset] & 0xFFFFFFFFL) | ((long)ints[offset + 1] << 32);
	}

	static int toInt(final boolean value) {
		return value ? 1 : 0;
	}

	static String toClassName(final String type) {
		return type.substring(1, type.length() - 1);
	}
}
