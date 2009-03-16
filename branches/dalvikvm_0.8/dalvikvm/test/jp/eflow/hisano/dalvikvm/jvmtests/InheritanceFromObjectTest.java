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

public class InheritanceFromObjectTest {
	public static class Wait {
		public String toString() {
			return "toString";
		}
	}

	public static void main(String[] args) {
		final java.lang.Thread mainThread = java.lang.Thread.currentThread();

		final Object wait = new Wait();
		System.out.println(wait.toString());

		java.lang.Thread thread = new java.lang.Thread(new Runnable() {
			public void run() {
				synchronized (wait) {
					wait.notify();
				}
				java.lang.Thread.yield();
				synchronized (wait) {
					mainThread.interrupt();
				}
			}
		});
		synchronized (wait) {
			thread.start();
			try {
				wait.wait();
			} catch (InterruptedException e) {
				System.out.println("interrupted");
			}
		}
		System.out.println("notified");
		synchronized (wait) {
			try {
				wait.wait();
			} catch (InterruptedException e) {
				System.out.println("interrupted");
			}
		}
	}
}
