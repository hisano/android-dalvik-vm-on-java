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

package jp.eflow.hisano.dalvikvm.jvmtests;

public class StaticFieldsTest {
	public static char public_static_char = 'c';
	public static byte public_static_byte = 1;
	public static short public_static_short = 2;
	public static int public_static_int = 3;
	public static long public_static_long = 4;
	public static boolean public_static_boolean = true;
	public static String public_static_String = "String";

	protected static char protected_static_char = 'c';
	protected static byte protected_static_byte = 1;
	protected static short protected_static_short = 2;
	protected static int protected_static_int = 3;
	protected static long protected_static_long = 4;
	protected static boolean protected_static_boolean = true;
	protected static String protected_static_String = "String";

	static char default_static_char = 'c';
	static byte default_static_byte = 1;
	static short default_static_short = 2;
	static int default_static_int = 3;
	static long default_static_long = 4;
	static boolean default_static_boolean = true;
	static String default_static_String = "String";

	private static char private_static_char = 'c';
	private static byte private_static_byte = 1;
	private static short private_static_short = 2;
	private static int private_static_int = 3;
	private static long private_static_long = 4;
	private static boolean private_static_boolean = true;
	private static String private_static_String = "String";

	public static final char public_static_final_char = 'c';
	public static final byte public_static_final_byte = 1;
	public static final short public_static_final_short = 2;
	public static final int public_static_final_int = 3;
	public static final long public_static_final_long = 4;
	public static final boolean public_static_final_boolean = true;
	public static final String public_static_final_String = "String";

	protected static final char protected_static_final_char = 'c';
	protected static final byte protected_static_final_byte = 1;
	protected static final short protected_static_final_short = 2;
	protected static final int protected_static_final_int = 3;
	protected static final long protected_static_final_long = 4;
	protected static final boolean protected_static_final_boolean = true;
	protected static final String protected_static_final_String = "String";

	static final char default_static_final_char = 'c';
	static final byte default_static_final_byte = 1;
	static final short default_static_final_short = 2;
	static final int default_static_final_int = 3;
	static final long default_static_final_long = 4;
	static final boolean default_static_final_boolean = true;
	static final String default_static_final_String = "String";

	private static final char private_static_final_char = 'c';
	private static final byte private_static_final_byte = 1;
	private static final short private_static_final_short = 2;
	private static final int private_static_final_int = 3;
	private static final long private_static_final_long = 4;
	private static final boolean private_static_final_boolean = true;
	private static final String private_static_final_String = "String";

	public static char public_static_char_without_value;
	public static byte public_static_byte_without_value;
	public static short public_static_short_without_value;
	public static int public_static_int_without_value;
	public static long public_static_long_without_value;
	public static boolean public_static_boolean_without_value;
	public static String public_static_String_without_value;

	protected static char protected_static_char_without_value;
	protected static byte protected_static_byte_without_value;
	protected static short protected_static_short_without_value;
	protected static int protected_static_int_without_value;
	protected static long protected_static_long_without_value;
	protected static boolean protected_static_boolean_without_value;
	protected static String protected_static_String_without_value;

	static char default_static_char_without_value;
	static byte default_static_byte_without_value;
	static short default_static_short_without_value;
	static int default_static_int_without_value;
	static long default_static_long_without_value;
	static boolean default_static_boolean_without_value;
	static String default_static_String_without_value;

	private static char private_static_char_without_value;
	private static byte private_static_byte_without_value;
	private static short private_static_short_without_value;
	private static int private_static_int_without_value;
	private static long private_static_long_without_value;
	private static boolean private_static_boolean_without_value;
	private static String private_static_String_without_value;

	public static void main(String[] args) {
		testCharFields();
		testByteFields();
		testShortFields();
		testIntFields();
		testLongFields();
		testBooleanFields();
		testStringFields();
	}

	private static void testCharFields() {
		if (public_static_char != 'c') {
			System.out.println("public_static_char");
		}
		public_static_char = 'd';
		if (public_static_char != 'd') {
			System.out.println("public_static_char");
		}

		if (protected_static_char != 'c') {
			System.out.println("protected_static_char");
		}
		protected_static_char = 'd';
		if (protected_static_char != 'd') {
			System.out.println("protected_static_char");
		}

		if (default_static_char != 'c') {
			System.out.println("default_static_char");
		}
		default_static_char = 'd';
		if (default_static_char != 'd') {
			System.out.println("default_static_char");
		}

		if (private_static_char != 'c') {
			System.out.println("private_static_char");
		}
		private_static_char = 'd';
		if (private_static_char != 'd') {
			System.out.println("private_static_char");
		}

		if (public_static_final_char != 'c') {
			System.out.println("public_static_final_char");
		}

		if (protected_static_final_char != 'c') {
			System.out.println("protected_static_final_char");
		}

		if (default_static_final_char != 'c') {
			System.out.println("default_static_final_char");
		}

		if (private_static_final_char != 'c') {
			System.out.println("private_static_final_char");
		}

		if (public_static_char_without_value != 0) {
			System.out.println("public_static_char_without_value");
		}
		public_static_char_without_value = 'd';
		if (public_static_char_without_value != 'd') {
			System.out.println("public_static_char_without_value");
		}

		if (protected_static_char_without_value != 0) {
			System.out.println("protected_static_char_without_value");
		}
		protected_static_char_without_value = 'd';
		if (protected_static_char_without_value != 'd') {
			System.out.println("protected_static_char_without_value");
		}

		if (default_static_char_without_value != 0) {
			System.out.println("default_static_char_without_value");
		}
		default_static_char_without_value = 'd';
		if (default_static_char_without_value != 'd') {
			System.out.println("default_static_char_without_value");
		}

		if (private_static_char_without_value != 0) {
			System.out.println("private_static_char_without_value");
		}
		private_static_char_without_value = 'd';
		if (private_static_char_without_value != 'd') {
			System.out.println("private_static_char_without_value");
		}
	}

	private static void testByteFields() {
		if (public_static_byte != 1) {
			System.out.println("public_static_byte");
		}
		public_static_byte = 2;
		if (public_static_byte != 2) {
			System.out.println("public_static_byte");
		}

		if (protected_static_byte != 1) {
			System.out.println("protected_static_byte");
		}
		protected_static_byte = 2;
		if (protected_static_byte != 2) {
			System.out.println("protected_static_byte");
		}

		if (default_static_byte != 1) {
			System.out.println("default_static_byte");
		}
		default_static_byte = 2;
		if (default_static_byte != 2) {
			System.out.println("default_static_byte");
		}

		if (private_static_byte != 1) {
			System.out.println("private_static_byte");
		}
		private_static_byte = 2;
		if (private_static_byte != 2) {
			System.out.println("private_static_byte");
		}

		if (public_static_final_byte != 1) {
			System.out.println("public_static_final_byte");
		}

		if (protected_static_final_byte != 1) {
			System.out.println("protected_static_final_byte");
		}

		if (default_static_final_byte != 1) {
			System.out.println("default_static_final_byte");
		}

		if (private_static_final_byte != 1) {
			System.out.println("private_static_final_byte");
		}

		if (public_static_byte_without_value != 0) {
			System.out.println("public_static_byte_without_value");
		}
		public_static_byte_without_value = 2;
		if (public_static_byte_without_value != 2) {
			System.out.println("public_static_byte_without_value");
		}

		if (protected_static_byte_without_value != 0) {
			System.out.println("protected_static_byte_without_value");
		}
		protected_static_byte_without_value = 2;
		if (protected_static_byte_without_value != 2) {
			System.out.println("protected_static_byte_without_value");
		}

		if (default_static_byte_without_value != 0) {
			System.out.println("default_static_byte_without_value");
		}
		default_static_byte_without_value = 2;
		if (default_static_byte_without_value != 2) {
			System.out.println("default_static_byte_without_value");
		}

		if (private_static_byte_without_value != 0) {
			System.out.println("private_static_byte_without_value");
		}
		private_static_byte_without_value = 2;
		if (private_static_byte_without_value != 2) {
			System.out.println("private_static_byte_without_value");
		}
	}

	private static void testShortFields() {
		if (public_static_short != 2) {
			System.out.println("public_static_short");
		}
		public_static_short = 3;
		if (public_static_short != 3) {
			System.out.println("public_static_short");
		}

		if (protected_static_short != 2) {
			System.out.println("protected_static_short");
		}
		protected_static_short = 3;
		if (protected_static_short != 3) {
			System.out.println("protected_static_short");
		}

		if (default_static_short != 2) {
			System.out.println("default_static_short");
		}
		default_static_short = 3;
		if (default_static_short != 3) {
			System.out.println("default_static_short");
		}

		if (private_static_short != 2) {
			System.out.println("private_static_short");
		}
		private_static_short = 3;
		if (private_static_short != 3) {
			System.out.println("private_static_short");
		}

		if (public_static_final_short != 2) {
			System.out.println("public_static_final_short");
		}

		if (protected_static_final_short != 2) {
			System.out.println("protected_static_final_short");
		}

		if (default_static_final_short != 2) {
			System.out.println("default_static_final_short");
		}

		if (private_static_final_short != 2) {
			System.out.println("private_static_final_short");
		}

		if (public_static_short_without_value != 0) {
			System.out.println("public_static_short_without_value");
		}
		public_static_short_without_value = 3;
		if (public_static_short_without_value != 3) {
			System.out.println("public_static_short_without_value");
		}

		if (protected_static_short_without_value != 0) {
			System.out.println("protected_static_short_without_value");
		}
		protected_static_short_without_value = 3;
		if (protected_static_short_without_value != 3) {
			System.out.println("protected_static_short_without_value");
		}

		if (default_static_short_without_value != 0) {
			System.out.println("default_static_short_without_value");
		}
		default_static_short_without_value = 3;
		if (default_static_short_without_value != 3) {
			System.out.println("default_static_short_without_value");
		}

		if (private_static_short_without_value != 0) {
			System.out.println("private_static_short_without_value");
		}
		private_static_short_without_value = 3;
		if (private_static_short_without_value != 3) {
			System.out.println("private_static_short_without_value");
		}
	}

	private static void testIntFields() {
		if (public_static_int != 3) {
			System.out.println("public_static_int");
		}
		public_static_int = 4;
		if (public_static_int != 4) {
			System.out.println("public_static_int");
		}

		if (protected_static_int != 3) {
			System.out.println("protected_static_int");
		}
		protected_static_int = 4;
		if (protected_static_int != 4) {
			System.out.println("protected_static_int");
		}

		if (default_static_int != 3) {
			System.out.println("default_static_int");
		}
		default_static_int = 4;
		if (default_static_int != 4) {
			System.out.println("default_static_int");
		}

		if (private_static_int != 3) {
			System.out.println("private_static_int");
		}
		private_static_int = 4;
		if (private_static_int != 4) {
			System.out.println("private_static_int");
		}

		if (public_static_final_int != 3) {
			System.out.println("public_static_final_int");
		}

		if (protected_static_final_int != 3) {
			System.out.println("protected_static_final_int");
		}

		if (default_static_final_int != 3) {
			System.out.println("default_static_final_int");
		}

		if (private_static_final_int != 3) {
			System.out.println("private_static_final_int");
		}

		if (public_static_int_without_value != 0) {
			System.out.println("public_static_int_without_value");
		}
		public_static_int_without_value = 4;
		if (public_static_int_without_value != 4) {
			System.out.println("public_static_int_without_value");
		}

		if (protected_static_int_without_value != 0) {
			System.out.println("protected_static_int_without_value");
		}
		protected_static_int_without_value = 4;
		if (protected_static_int_without_value != 4) {
			System.out.println("protected_static_int_without_value");
		}

		if (default_static_int_without_value != 0) {
			System.out.println("default_static_int_without_value");
		}
		default_static_int_without_value = 4;
		if (default_static_int_without_value != 4) {
			System.out.println("default_static_int_without_value");
		}

		if (private_static_int_without_value != 0) {
			System.out.println("private_static_int_without_value");
		}
		private_static_int_without_value = 4;
		if (private_static_int_without_value != 4) {
			System.out.println("private_static_int_without_value");
		}
	}

	private static void testLongFields() {
		if (public_static_long != 4) {
			System.out.println("public_static_long");
		}
		public_static_long = 5;
		if (public_static_long != 5) {
			System.out.println("public_static_long");
		}

		if (protected_static_long != 4) {
			System.out.println("protected_static_long");
		}
		protected_static_long = 5;
		if (protected_static_long != 5) {
			System.out.println("protected_static_long");
		}

		if (default_static_long != 4) {
			System.out.println("default_static_long");
		}
		default_static_long = 5;
		if (default_static_long != 5) {
			System.out.println("default_static_long");
		}

		if (private_static_long != 4) {
			System.out.println("private_static_long");
		}
		private_static_long = 5;
		if (private_static_long != 5) {
			System.out.println("private_static_long");
		}

		if (public_static_final_long != 4) {
			System.out.println("public_static_final_long");
		}

		if (protected_static_final_long != 4) {
			System.out.println("protected_static_final_long");
		}

		if (default_static_final_long != 4) {
			System.out.println("default_static_final_long");
		}

		if (private_static_final_long != 4) {
			System.out.println("private_static_final_long");
		}

		if (public_static_long_without_value != 0) {
			System.out.println("public_static_long_without_value");
		}
		public_static_long_without_value = 5;
		if (public_static_long_without_value != 5) {
			System.out.println("public_static_long_without_value");
		}

		if (protected_static_long_without_value != 0) {
			System.out.println("protected_static_long_without_value");
		}
		protected_static_long_without_value = 5;
		if (protected_static_long_without_value != 5) {
			System.out.println("protected_static_long_without_value");
		}

		if (default_static_long_without_value != 0) {
			System.out.println("default_static_long_without_value");
		}
		default_static_long_without_value = 5;
		if (default_static_long_without_value != 5) {
			System.out.println("default_static_long_without_value");
		}

		if (private_static_long_without_value != 0) {
			System.out.println("private_static_long_without_value");
		}
		private_static_long_without_value = 5;
		if (private_static_long_without_value != 5) {
			System.out.println("private_static_long_without_value");
		}
	}

	private static void testBooleanFields() {
		if (!public_static_boolean) {
			System.out.println("public_static_boolean");
		}
		public_static_boolean = false;
		if (public_static_boolean) {
			System.out.println("public_static_boolean");
		}

		if (!protected_static_boolean) {
			System.out.println("protected_static_boolean");
		}
		protected_static_boolean = false;
		if (protected_static_boolean) {
			System.out.println("protected_static_boolean");
		}

		if (!default_static_boolean) {
			System.out.println("default_static_boolean");
		}
		default_static_boolean = false;
		if (default_static_boolean) {
			System.out.println("default_static_boolean");
		}

		if (!private_static_boolean) {
			System.out.println("private_static_boolean");
		}
		private_static_boolean = false;
		if (private_static_boolean) {
			System.out.println("private_static_boolean");
		}

		if (!public_static_final_boolean) {
			System.out.println("public_static_final_boolean");
		}

		if (!protected_static_final_boolean) {
			System.out.println("protected_static_final_boolean");
		}

		if (!default_static_final_boolean) {
			System.out.println("default_static_final_boolean");
		}

		if (!private_static_final_boolean) {
			System.out.println("private_static_final_boolean");
		}

		if (public_static_boolean_without_value) {
			System.out.println("public_static_boolean_without_value");
		}
		public_static_boolean_without_value = true;
		if (!public_static_boolean_without_value) {
			System.out.println("public_static_boolean_without_value");
		}

		if (protected_static_boolean_without_value) {
			System.out.println("protected_static_boolean_without_value");
		}
		protected_static_boolean_without_value = true;
		if (!protected_static_boolean_without_value) {
			System.out.println("protected_static_boolean_without_value");
		}

		if (default_static_boolean_without_value) {
			System.out.println("default_static_boolean_without_value");
		}
		default_static_boolean_without_value = true;
		if (!default_static_boolean_without_value) {
			System.out.println("default_static_boolean_without_value");
		}

		if (private_static_boolean_without_value) {
			System.out.println("private_static_boolean_without_value");
		}
		private_static_boolean_without_value = true;
		if (!private_static_boolean_without_value) {
			System.out.println("private_static_boolean_without_value");
		}
	}

	private static void testStringFields() {
		if (public_static_String == null) {
			System.out.println("public_static_String");
		}
		public_static_String = null;
		if (public_static_String != null) {
			System.out.println("public_static_String");
		}

		if (protected_static_String == null) {
			System.out.println("protected_static_String");
		}
		protected_static_String = null;
		if (protected_static_String != null) {
			System.out.println("protected_static_String");
		}

		if (default_static_String == null) {
			System.out.println("default_static_String");
		}
		default_static_String = null;
		if (default_static_String != null) {
			System.out.println("default_static_String");
		}

		if (private_static_String == null) {
			System.out.println("private_static_String");
		}
		private_static_String = null;
		if (private_static_String != null) {
			System.out.println("private_static_String");
		}

		if (public_static_final_String == null) {
			System.out.println("public_static_final_String");
		}

		if (protected_static_final_String == null) {
			System.out.println("protected_static_final_String");
		}

		if (default_static_final_String == null) {
			System.out.println("default_static_final_String");
		}

		if (private_static_final_String == null) {
			System.out.println("private_static_final_String");
		}

		if (public_static_String_without_value != null) {
			System.out.println("public_static_String_without_value");
		}
		public_static_String_without_value = null;
		if (public_static_String_without_value != null) {
			System.out.println("public_static_String_without_value");
		}

		if (protected_static_String_without_value != null) {
			System.out.println("protected_static_String_without_value");
		}
		protected_static_String_without_value = null;
		if (protected_static_String_without_value != null) {
			System.out.println("protected_static_String_without_value");
		}

		if (default_static_String_without_value != null) {
			System.out.println("default_static_String_without_value");
		}
		default_static_String_without_value = null;
		if (default_static_String_without_value != null) {
			System.out.println("default_static_String_without_value");
		}

		if (private_static_String_without_value != null) {
			System.out.println("private_static_String_without_value");
		}
		private_static_String_without_value = null;
		if (private_static_String_without_value != null) {
			System.out.println("private_static_String_without_value");
		}
	}
}
