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

import java.io.*;

import jp.eflow.hisano.dalvikvm.VirtualMachine;

public final class RunHelloWorld {
	public static void main(final String[] args) {
		final File dexFile = new File("tutorial1", "App.dex");
		final String absoluteMainClassName = "tutorial1.HelloWorld";

		final VirtualMachine vm = new VirtualMachine();
		vm.load(toBytes(dexFile));
		vm.run(absoluteMainClassName, new String[0]);
	}

	private static byte[] toBytes(final File dexFile) {
		final byte[] bytes = new byte[(int)dexFile.length()];
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(dexFile)));
			in.readFully(bytes);
		} catch (IOException e) {
			System.err.println("The specified dex file path is invalid: " + dexFile.getName());
			System.exit(-1);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return bytes;
	}
}
