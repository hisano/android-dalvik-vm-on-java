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

public class ArrayTest {
	public static void main(String[] args) {
		boolean[] za = new boolean[1];
		za[0] = true;
		if (za[0]) {
			System.out.println("‚½‚¾‚µ‚¢");
		}
		
		byte[] ba = new byte[1];
		ba[0] = 1;
		System.out.println("<NUM VAL=" + ba[0] + ">");

		char[] ca = new char[1];
		ca[0] = 2;
		System.out.println("<NUM VAL=" + (int)ca[0] + ">");

		short[] sa = new short[1];
		sa[0] = 3;
		System.out.println("<NUM VAL=" + sa[0] + ">");

		int[] ia = new int[1];
		ia[0] = 4;
		System.out.println("<NUM VAL=" + ia[0] + ">");

		long[] la = new long[1];
		la[0] = 5;
		System.out.println("<NUM VAL=" + la[0] + ">");
		
		String[] aa = new String[1];
		aa[0] = "‚Ä‚·‚Æ";
		System.out.println(aa[0]);

		boolean[][] zaa = new boolean[1][1];
		zaa[0][0] = true;
		if (zaa[0][0]) {
			System.out.println("‚½‚¾‚µ‚¢");
		}

		byte[][] baa = new byte[1][1];
		baa[0][0] = 1;
		System.out.println("<NUM VAL=" + baa[0][0] + ">");

		char[][] caa = new char[1][1];
		caa[0][0] = 2;
		System.out.println("<NUM VAL=" + (int)caa[0][0] + ">");

		short[][] saa = new short[1][1];
		saa[0][0] = 3;
		System.out.println("<NUM VAL=" + saa[0][0] + ">");

		int[][] iaa = new int[1][1];
		iaa[0][0] = 4;
		System.out.println("<NUM VAL=" + iaa[0][0] + ">");

		long[][] laa = new long[1][1];
		laa[0][0] = 5;
		System.out.println("<NUM VAL=" + laa[0][0] + ">");
		
		String[][] aaa = new String[1][1];
		aaa[0][0] = "‚Ä‚·‚Æ";
		System.out.println(aaa[0][0]);

		boolean[][] zaa2 = new boolean[1][];
		zaa2[0] = new boolean[1];
		zaa2[0][0] = true;
		if (zaa2[0][0]) {
			System.out.println("‚½‚¾‚µ‚¢");
		}

		byte[][] baa2 = new byte[1][];
		baa2[0] = new byte[1];
		baa2[0][0] = 1;
		System.out.println("<NUM VAL=" + baa2[0][0] + ">");

		char[][] caa2 = new char[1][];
		caa2[0] = new char[1];
		caa2[0][0] = 2;
		System.out.println("<NUM VAL=" + (int)caa2[0][0] + ">");

		short[][] saa2 = new short[1][];
		saa2[0] = new short[1];
		saa2[0][0] = 3;
		System.out.println("<NUM VAL=" + saa2[0][0] + ">");

		int[][] iaa2 = new int[1][];
		iaa2[0] = new int[1];
		iaa2[0][0] = 4;
		System.out.println("<NUM VAL=" + iaa2[0][0] + ">");

		long[][] laa2 = new long[1][];
		laa2[0] = new long[1];
		laa2[0][0] = 5;
		System.out.println("<NUM VAL=" + laa2[0][0] + ">");
		
		String[][] aaa2 = new String[1][];
		aaa2[0] = new String[1];
		aaa2[0][0] = "‚Ä‚·‚Æ";
		System.out.println(aaa2[0][0]);
	}
}
