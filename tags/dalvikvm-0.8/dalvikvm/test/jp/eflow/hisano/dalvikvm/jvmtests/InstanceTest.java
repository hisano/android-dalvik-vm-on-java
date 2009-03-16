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

public class InstanceTest {
	public static void main(String[] args) {
		InstanceTest instance = new InstanceTest("get_field");
		instance.publicMethod();
		instance.protectedMethod();
		instance.packageMethod();
		instance.privateMethod();
		System.out.println(instance.field);
		instance.field = "set_field";
		System.out.println(instance.field);
	}

	private String field = "default";

	private InstanceTest() {
	}

	private InstanceTest(String field) {
		this.field = field;
	}

	public void publicMethod() {
		System.out.println("public_method");
	}

	protected void protectedMethod() {
		System.out.println("protected_method");
	}

	void packageMethod() {
		System.out.println("package_method");
	}

	private void privateMethod() {
		System.out.println("private_method");
	}
}
