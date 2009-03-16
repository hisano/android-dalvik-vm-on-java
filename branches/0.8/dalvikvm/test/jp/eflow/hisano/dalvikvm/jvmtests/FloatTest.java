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

public class FloatTest {
	public static void main(String[] args) {
		testMethod();
		testCompare();
		testConvert();
		testCalc();
		testArray();
		testStoreAndLoad();
		testConstant();
	}

	private static void testMethod() {
		System.out.println(add("1f + 2f = '", 1f, 2f, "'"));
	}

	private static String add(String header, float value1, float value2, String footer) {
		return header + add(value1, value2) + footer;
	}

	private static float add(float value1, float value2) {
		return value1 + value2;
	}

	private static void testCompare() {
		float value1 = Float.NaN;
		float value2 = Float.NaN;
		if (!(value1 < value2)) {
			System.out.println("Values are NaN.");
		}
		if (!(value1 > value2)) {
			System.out.println("Values are NaN.");
		}

		value1 = 0f;
		value2 = 0f;
		if (value1 <= value2) {
			System.out.println("0f <= 0f");
		}
		if (value1 >= value2) {
			System.out.println("0f >= 0f");
		}

		value1 = 0f;
		value2 = 1f;
		if (value1 < value2) {
			System.out.println("0f < 1f");
		}
		if (value1 > value2) {
			System.out.println("0f > 1f");
		}

		value1 = 1f;
		value2 = 0f;
		if (value1 < value2) {
			System.out.println("1f < 0f");
		}
		if (value1 > value2) {
			System.out.println("1f > 0f");
		}
	}

	private static void testConvert() {
		float f = 1.5f;
		System.out.println("(int)1.5f = " + (int)f);
		System.out.println("(long)1.5f = " + (long)f);
		int i = 1;
		System.out.println("(float)1 = " + (float)i);
		long l = 1L;
		System.out.println("(float)1L = " + (float)l);
	}

	private static void testCalc() {
		float f1 = 4f, f2 = 2f;
		System.out.println("4.0 + 2.0 = " + (f1 + f2));
		System.out.println("4.0 - 2.0 = " + (f1 - f2));
		System.out.println("4.0 * 2.0 = " + (f1 * f2));
		System.out.println("4.0 / 2.0 = " + (f1 / f2));
		System.out.println("4.0 % 2.0 = " + (f1 % f2));
		System.out.println("-(4.0) = " + (-f1));
	}

	private static void testArray() {
		float[] fa = null;
		try {
			System.out.println(fa[0]);
			System.out.println("A execution must not reach here because fa is null when loading.");
		} catch (NullPointerException e) {
		}
		try {
			fa[0] = 0F;
			System.out.println("A execution must not reach here because fa is null when storeing.");
		} catch (NullPointerException e) {
		}

		fa = new float[0];
		try {
			System.out.println(fa[1]);
			System.out.println("A execution must not reach here because fa[1] is out of bounds when loading.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			fa[1] = 1F;
			System.out.println("A execution must not reach here because fa[1] is out of bounds when storing.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		fa = new float[1];
		fa[0] = 2f;
		System.out.println("fa[0] is '" + fa[0] + "'.");
	}

	private static void testStoreAndLoad() {
		// fstore_0, fload_0
		float f0 = 0;
		System.out.println("f0 = " + f0);

		// fstore_1, fload_1
		float f1 = 1;
		System.out.println("f1 = " + f1);

		// fstore_2, fload_2
		float f2 = 2;
		System.out.println("f2 = " + f2);

		// fstore_3, fload_3
		float f3 = 3;
		System.out.println("f3 = " + f3);

		// fstore, fload
		float f4 = 4;
		System.out.println("f4 = " + f4);
	}

	private static void testConstant() {
		System.out.println(0f);
		System.out.println(1f);
		System.out.println(2f);
		System.out.println(3f);
	}
}
