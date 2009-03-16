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

import java.util.Vector;

public class ArrayLengthTest {
	public static void main(String[] args) {
		System.out.println("new boolean[0].length = " + new boolean[0].length);
		System.out.println("new boolean[0][].length = " + new boolean[0][].length);
		System.out.println("new boolean[0][0].length = " + new boolean[0][0].length);
		System.out.println("new boolean[0][][].length = " + new boolean[0][][].length);
		System.out.println("new boolean[0][0][].length = " + new boolean[0][0][].length);
		System.out.println("new boolean[0][0][0].length = " + new boolean[0][0][0].length);
		System.out.println("new byte[1].length = " + new byte[1].length);
		System.out.println("new byte[1][].length = " + new byte[1][].length);
		System.out.println("new byte[1][1].length = " + new byte[1][1].length);
		System.out.println("new byte[1][][].length = " + new byte[1][][].length);
		System.out.println("new byte[1][1][].length = " + new byte[1][1][].length);
		System.out.println("new byte[1][1][1].length = " + new byte[1][1][1].length);
		System.out.println("new short[2].length = " + new short[2].length);
		System.out.println("new short[2][].length = " + new short[2][].length);
		System.out.println("new short[2][2].length = " + new short[2][2].length);
		System.out.println("new short[2][][].length = " + new short[2][][].length);
		System.out.println("new short[2][2][].length = " + new short[2][2][].length);
		System.out.println("new short[2][2][2].length = " + new short[2][2][2].length);
		System.out.println("new int[3].length = " + new int[3].length);
		System.out.println("new int[3][].length = " + new int[3][].length);
		System.out.println("new int[3][3].length = " + new int[3][3].length);
		System.out.println("new int[3][][].length = " + new int[3][][].length);
		System.out.println("new int[3][3][].length = " + new int[3][3][].length);
		System.out.println("new int[3][3][3].length = " + new int[3][3][3].length);
		System.out.println("new long[4].length = " + new long[4].length);
		System.out.println("new long[4][].length = " + new long[4][].length);
		System.out.println("new long[4][4].length = " + new long[4][4].length);
		System.out.println("new long[4][][].length = " + new long[4][][].length);
		System.out.println("new long[4][4][].length = " + new long[4][4][].length);
		System.out.println("new long[4][4][4].length = " + new long[4][4][4].length);
		System.out.println("new Object[5].length = " + new Object[5].length);
		System.out.println("new Object[5][].length = " + new Object[5][].length);
		System.out.println("new Object[5][5].length = " + new Object[5][5].length);
		System.out.println("new Object[5][][].length = " + new Object[5][][].length);
		System.out.println("new Object[5][5][].length = " + new Object[5][5][].length);
		System.out.println("new Object[5][5][5].length = " + new Object[5][5][5].length);
		System.out.println("new String[6].length = " + new String[6].length);
		System.out.println("new String[6][].length = " + new String[6][].length);
		System.out.println("new String[6][6].length = " + new String[6][6].length);
		System.out.println("new String[6][][].length = " + new String[6][][].length);
		System.out.println("new String[6][6][].length = " + new String[6][6][].length);
		System.out.println("new String[6][6][6].length = " + new String[6][6][6].length);
		System.out.println("new Vector[7].length = " + new Vector[7].length);
		System.out.println("new Vector[7][].length = " + new Vector[7][].length);
		System.out.println("new Vector[7][7].length = " + new Vector[7][7].length);
		System.out.println("new Vector[7][][].length = " + new Vector[7][][].length);
		System.out.println("new Vector[7][7][].length = " + new Vector[7][7][].length);
		System.out.println("new Vector[7][7][7].length = " + new Vector[7][7][7].length);
	}
}
