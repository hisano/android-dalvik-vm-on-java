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

package jp.eflow.hisano.dalvikvm.dvmtests;

public class Test0x52to0x5F {
	public char public_static_char = 'c';
	public byte public_static_byte = 1;
	public short public_static_short = 2;
	public int public_static_int = 3;
	public long public_static_long = 4;
	public boolean public_static_boolean = true;
	public String public_static_String = "String";

	protected char protected_static_char = 'c';
	protected byte protected_static_byte = 1;
	protected short protected_static_short = 2;
	protected int protected_static_int = 3;
	protected long protected_static_long = 4;
	protected boolean protected_static_boolean = true;
	protected String protected_static_String = "String";

	char default_static_char = 'c';
	byte default_static_byte = 1;
	short default_static_short = 2;
	int default_static_int = 3;
	long default_static_long = 4;
	boolean default_static_boolean = true;
	String default_static_String = "String";

	private char private_static_char = 'c';
	private byte private_static_byte = 1;
	private short private_static_short = 2;
	private int private_static_int = 3;
	private long private_static_long = 4;
	private boolean private_static_boolean = true;
	private String private_static_String = "String";

	public final char public_static_final_char = 'c';
	public final byte public_static_final_byte = 1;
	public final short public_static_final_short = 2;
	public final int public_static_final_int = 3;
	public final long public_static_final_long = 4;
	public final boolean public_static_final_boolean = true;
	public final String public_static_final_String = "String";

	protected final char protected_static_final_char = 'c';
	protected final byte protected_static_final_byte = 1;
	protected final short protected_static_final_short = 2;
	protected final int protected_static_final_int = 3;
	protected final long protected_static_final_long = 4;
	protected final boolean protected_static_final_boolean = true;
	protected final String protected_static_final_String = "String";

	final char default_static_final_char = 'c';
	final byte default_static_final_byte = 1;
	final short default_static_final_short = 2;
	final int default_static_final_int = 3;
	final long default_static_final_long = 4;
	final boolean default_static_final_boolean = true;
	final String default_static_final_String = "String";

	private final char private_static_final_char = 'c';
	private final byte private_static_final_byte = 1;
	private final short private_static_final_short = 2;
	private final int private_static_final_int = 3;
	private final long private_static_final_long = 4;
	private final boolean private_static_final_boolean = true;
	private final String private_static_final_String = "String";

	public char public_static_char_without_value;
	public byte public_static_byte_without_value;
	public short public_static_short_without_value;
	public int public_static_int_without_value;
	public long public_static_long_without_value;
	public boolean public_static_boolean_without_value;
	public String public_static_String_without_value;

	protected char protected_static_char_without_value;
	protected byte protected_static_byte_without_value;
	protected short protected_static_short_without_value;
	protected int protected_static_int_without_value;
	protected long protected_static_long_without_value;
	protected boolean protected_static_boolean_without_value;
	protected String protected_static_String_without_value;

	char default_static_char_without_value;
	byte default_static_byte_without_value;
	short default_static_short_without_value;
	int default_static_int_without_value;
	long default_static_long_without_value;
	boolean default_static_boolean_without_value;
	String default_static_String_without_value;

	private char private_static_char_without_value;
	private byte private_static_byte_without_value;
	private short private_static_short_without_value;
	private int private_static_int_without_value;
	private long private_static_long_without_value;
	private boolean private_static_boolean_without_value;
	private String private_static_String_without_value;

	public static void main(String[] args) {
		Test0x52to0x5F instance = new Test0x52to0x5F();
		instance.testCharFields();
		instance.testByteFields();
		instance.testShortFields();
		instance.testIntFields();
		instance.testLongFields();
		instance.testBooleanFields();
		instance.testStringFields();
	}

	private void testCharFields() {
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

	private void testByteFields() {
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

	private void testShortFields() {
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

	private void testIntFields() {
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

	private void testLongFields() {
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

	private void testBooleanFields() {
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

	private void testStringFields() {
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
