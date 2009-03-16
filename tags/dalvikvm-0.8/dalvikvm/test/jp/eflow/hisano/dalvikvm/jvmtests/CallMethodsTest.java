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

public class CallMethodsTest {
	public static void main(String[] args) {
		test();
		System.out.println("" + test(false));
		System.out.println("" + test(true));
		System.out.println("" + test('a'));
		System.out.println("" + test((byte)1));
		System.out.println("" + test((short)2));
		System.out.println("" + test(3));
		System.out.println("" + test(4L));
		System.out.println("" + test("テスト"));
		test(false, 'a', (byte)1, (short)2, 3, 4L, "テスト");
		test("テスト", 4L, 3, (short)2, (byte)1, 'a', false);
		test(new int[] { 1, 2, 3 });
	}

	private static void test(int[] ia) {
		System.out.println(ia[0] + "," + ia[1] + "," + ia[2]);
	}

	private static void test(String string, long l, int i, short s, byte b, char c, boolean d) {
		System.out.println(string + "," + l + "," + i + "," + s + "," + b + "," + c + "," + d);
	}

	private static void test(boolean b, char c, byte d, short s, int i, long l, String str) {
		System.out.println(b + "," + c + "," + d + "," + s + "," + i + "," + l + "," + str);
	}

	private static String test(String s) {
		System.out.println(s);
		return s + s;
	}

	private static long test(long l) {
		System.out.println("" + l);
		return l + 1;
	}

	private static int test(int i) {
		return test0(i);
	}

	private static int test0(int i) {
		System.out.println("" + i);
		return i + 1;
	}

	private static short test(short s) {
		System.out.println("" + s);
		return (short)(s + 1);
	}

	private static byte test(byte b) {
		System.out.println("" + b);
		return (byte)(b + 1);
	}

	private static char test(char c) {
		System.out.println("" + c);
		return (char)(c + 1);
	}

	private static boolean test(boolean b) {
		System.out.println("" + b);
		return !b;
	}

	private static void test() {
		System.out.println("引数なし");
	}
}
