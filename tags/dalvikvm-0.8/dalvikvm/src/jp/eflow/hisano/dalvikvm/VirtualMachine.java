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

import java.io.*;
import java.lang.ref.*;
import java.util.*;

public class VirtualMachine {
	final ClassLoader systemClassLoader;

	private volatile boolean isEnd = true;
	private volatile boolean stopRequested = false;
	private Object stopWait = new Object();

	Vector threads = new Vector();

	private final Vector waitSets = new Vector();

	public VirtualMachine() {
		systemClassLoader = new ClassLoader(this);
	}

	public final void load(final byte[] dexFileContent) {
		systemClassLoader.loadClasses(dexFileContent);
	}

	public final void run(final String mainClass, final String[] args) {
		Clazz clazz = systemClassLoader.loadClass(mainClass.replace('.', '/'));
		if (clazz == null) {
			throw new IllegalArgumentException("no such class: " + mainClass);
		}

		isEnd = false;

		Thread main = new Thread(this, "main");
		try {
			Frame frame = main.pushFrame();
			frame.init(clazz.getDirectMethod("main", "([Ljava/lang/String;)V"));
			frame.intArgument(0, args);

			main.start();
			do {
				for (int i = 0; i < threads.size(); i++) {
					try {
						Thread thread = (Thread)threads.elementAt(i);
						switch (thread.status) {
							case Thread.STATUS_RUNNING:
								thread.execute(false);
								break;
							case Thread.STATUS_JOIN:
								break;
							case Thread.STATUS_SLEEP:
								if (thread.wakeUpTime != 0 && thread.wakeUpTime <= System.currentTimeMillis()) {
									thread.wakeUpTime = 0;
									thread.status = Thread.STATUS_RUNNING;
								}
								break;
							case Thread.STATUS_INTERRUPTED:
								thread.handleInterrupted();
								// execute here for good response after calling #interrupt
								if (thread.status == Thread.STATUS_RUNNING) {
									thread.execute(false);
								}
								break;
							case Thread.STATUS_WAIT_FOR_MONITOR:
								break;
							case Thread.STATUS_WAIT_FOR_NOTIFICATION:
								if (thread.wakeUpTime != 0 && thread.wakeUpTime <= System.currentTimeMillis()) {
									thread.wakeUpTime = 0;
									thread.acquireLock(thread.monitorToResume, false);
									thread.status = Thread.STATUS_RUNNING;
								}
								break;
							default:
								throw new VirtualMachineException(thread.name + " thread status is illegal (=" + thread.status + ").");
						}
					} catch (ChangeThreadException e) {
						Throwable throwable = e.getCause();
						if (throwable != null) {
							error(throwable);
						}
					}
					synchronized (stopWait) {
						if (stopRequested) {
							stopRequested = false;
							stopWait.notify();
							isEnd = true;
							return;
						}
					}
				}
			} while (threads.size() != 0);
		} catch (Throwable e) {
			if (e instanceof VirtualMachineException) {
				throw (VirtualMachineException)e;
			}
			error(e);
			isEnd = true;
		}
	}

	public final void stop() {
		synchronized (stopWait) {
			stopRequested = true;
			try {
				stopWait.wait();
			} catch (InterruptedException e) {
				java.lang.Thread.currentThread().interrupt();
			}
		}
	}

	public final boolean isEnd() {
		return isEnd;
	}

	protected void error(final Throwable e) {
		error(e.getClass().getName() + ":" + e.getMessage());
	}

	protected void error(final String message) {
		System.out.println(message);
	}

	boolean isSubClass(final Throwable checked, final String targetClassName) {
		if (targetClassName == null) {
			return true;
		}
		try {
			return java.lang.Class.forName(targetClassName.replace('/', '.')).isAssignableFrom(checked.getClass());
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	Object handleNewObjectArray(final String absoluteClassName, final int dimension, final int lengthNumber, final int length1, final int length2, final int length3) {
		// NEW OBJECT ARRAY SECTION {
		// }
		switch (dimension) {
			case 1:
				return new Object[length1];
			case 2:
				switch (lengthNumber) {
					case 1:
						return new Object[length1][];
					case 2:
						return new Object[length1][length2];
				}
				break;
			case 3:
				switch (lengthNumber) {
					case 1:
						return new Object[length1][][];
					case 2:
						return new Object[length1][length2][];
					case 3:
						return new Object[length1][length2][length3];
				}
				break;
		}
		throw new VirtualMachineException("not supported array type = " + absoluteClassName + toDimesionString(dimension));
	}

	private String toDimesionString(final int dimension) {
		StringBuffer returned = new StringBuffer();
		for (int i = 0; i < dimension; i++) {
			returned.append("[]");
		}
		return returned.toString();
	}

	protected boolean handleInterfaceMethod(final Frame frame, final String absoluteClassName, final String methodName, final String methodDescriptor) throws Exception {
		// INTERFACE METHOD SECTION {
		String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
		String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
		if ("java/util".equals(packageName)) {
			if ("Enumeration".equals(className)) {
				if ("nextElement".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Enumeration)toTargetInstance(frame.objectArguments[0])).nextElement();
					return true;
				} else if ("hasMoreElements".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Enumeration)toTargetInstance(frame.objectArguments[0])).hasMoreElements() ? 1 : 0;
					return true;
				} else {
					return false;
				}
			}
		} else if ("java/lang".equals(packageName)) {
			if ("Runnable".equals(className) && "run".equals(methodName)) {
				((Runnable)toTargetInstance(frame.objectArguments[0])).run();
				return true;
			}
		} else if ("java/io".equals(packageName)) {
			if ("DataInput".equals(className)) {
				if ("readInt".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readInt();
					return true;
				} else if ("readUTF".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readUTF();
					return true;
				} else if ("readByte".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readByte();
					return true;
				} else if ("readChar".equals(methodName) && "()C".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readChar();
					return true;
				} else if ("readLong".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readLong();
					return true;
				} else if ("readFully".equals(methodName) && "([B)V".equals(methodDescriptor)) {
					((DataInput)toTargetInstance(frame.objectArguments[0])).readFully((byte[])frame.objectArguments[1]);
					return true;
				} else if ("readFully".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((DataInput)toTargetInstance(frame.objectArguments[0])).readFully((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skipBytes".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).skipBytes(frame.intArguments[1]);
					return true;
				} else if ("readShort".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readShort();
					return true;
				} else if ("readFloat".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((DataInput)toTargetInstance(frame.objectArguments[0])).readFloat());
					return true;
				} else if ("readDouble".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((DataInput)toTargetInstance(frame.objectArguments[0])).readDouble());
					return true;
				} else if ("readBoolean".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readBoolean() ? 1 : 0;
					return true;
				} else if ("readUnsignedByte".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readUnsignedByte();
					return true;
				} else if ("readUnsignedShort".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInput)toTargetInstance(frame.objectArguments[0])).readUnsignedShort();
					return true;
				} else {
					return false;
				}
			} else if ("DataOutput".equals(className)) {
				if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([B)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("writeInt".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeInt(frame.intArguments[1]);
					return true;
				} else if ("writeUTF".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeUTF((String)frame.objectArguments[1]);
					return true;
				} else if ("writeByte".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeByte(frame.intArguments[1]);
					return true;
				} else if ("writeChar".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeChar(frame.intArguments[1]);
					return true;
				} else if ("writeLong".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeLong(getLong(frame.intArguments, 1));
					return true;
				} else if ("writeShort".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeShort(frame.intArguments[1]);
					return true;
				} else if ("writeFloat".equals(methodName) && "(F)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeFloat(Float.intBitsToFloat(frame.intArguments[1]));
					return true;
				} else if ("writeChars".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeChars((String)frame.objectArguments[1]);
					return true;
				} else if ("writeDouble".equals(methodName) && "(D)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeDouble(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1)));
					return true;
				} else if ("writeBoolean".equals(methodName) && "(Z)V".equals(methodDescriptor)) {
					((DataOutput)toTargetInstance(frame.objectArguments[0])).writeBoolean(frame.intArguments[1] != 0);
					return true;
				} else {
					return false;
				}
			}
		}
		// }
		return false;
	}

	protected boolean handleClassFieldGetter(final Frame frame, final String absoluteClassName, final String fieldName, final String fieldDescriptor, final int destination) {
		// CLASS FIELD SECTION {
		String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
		String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
		if ("java/lang".equals(packageName)) {
			if ("Boolean".equals(className)) {
				if ("TRUE".equals(fieldName)) {
					frame.objectRegisters[destination] = Boolean.TRUE;
					frame.isObjectRegister[destination] = true;
					return true;
				} else if ("FALSE".equals(fieldName)) {
					frame.objectRegisters[destination] = Boolean.FALSE;
					frame.isObjectRegister[destination] = true;
					return true;
				}
			} else if ("System".equals(className)) {
				if ("out".equals(fieldName)) {
					frame.objectRegisters[destination] = System.out;
					frame.isObjectRegister[destination] = true;
					return true;
				} else if ("err".equals(fieldName)) {
					frame.objectRegisters[destination] = System.err;
					frame.isObjectRegister[destination] = true;
					return true;
				}
			}
		}
		// }
		// not CLDC classes but used in dex file
		if ("java/lang".equals(packageName)) {
			if ("Boolean".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Boolean.class;
					return true;
				}
			} else if ("Byte".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Byte.class;
					return true;
				}
			} else if ("Short".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Short.class;
					return true;
				}
			} else if ("Integer".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Integer.class;
					return true;
				}
			} else if ("Long".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Long.class;
					return true;
				}
			} else if ("Float".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Float.class;
					return true;
				}
			} else if ("Double".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Double.class;
					return true;
				}
			} else if ("Character".equals(className)) {
				if ("TYPE".equals(fieldName)) {
					frame.objectRegisters[destination] = Character.class;
					return true;
				}
			}
		}
		return false;
	}

	public boolean handleClassFieldSetter(final Frame frame, final int source, final String absoluteClassName, final String fieldName, final String fieldDescriptor) {
		return false;
	}

	protected boolean handleInstanceFieldGetter(final Frame frame, final String absoluteClassName, final String fieldName, final String fieldDescriptor, final int register) {
		// INSTANCE FIELD SECTION {
		// }
		return false;
	}

	protected boolean handleClassMethod(final Frame frame, final String absoluteClassName, final String methodName, final String methodDescriptor) throws Exception {
		// CLASS METHOD SECTION {
		String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
		String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
		if ("java/util".equals(packageName)) {
			if ("Calendar".equals(className)) {
				if ("getInstance".equals(methodName) && "()Ljava/util/Calendar;".equals(methodDescriptor)) {
					frame.objectReturn = Calendar.getInstance();
					return true;
				} else if ("getInstance".equals(methodName) && "(Ljava/util/TimeZone;)Ljava/util/Calendar;".equals(methodDescriptor)) {
					frame.objectReturn = Calendar.getInstance((TimeZone)frame.objectArguments[0]);
					return true;
				} else {
					return false;
				}
			} else if ("TimeZone".equals(className)) {
				if ("getDefault".equals(methodName) && "()Ljava/util/TimeZone;".equals(methodDescriptor)) {
					frame.objectReturn = TimeZone.getDefault();
					return true;
				} else if ("getTimeZone".equals(methodName) && "(Ljava/lang/String;)Ljava/util/TimeZone;".equals(methodDescriptor)) {
					frame.objectReturn = TimeZone.getTimeZone((String)frame.objectArguments[0]);
					return true;
				} else if ("getAvailableIDs".equals(methodName) && "()[Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = TimeZone.getAvailableIDs();
					return true;
				} else {
					return false;
				}
			}
		} else if ("java/lang".equals(packageName)) {
			if ("Byte".equals(className)) {
				if ("parseByte".equals(methodName) && "(Ljava/lang/String;)B".equals(methodDescriptor)) {
					frame.singleReturn = Byte.parseByte((String)frame.objectArguments[0]);
					return true;
				} else if ("parseByte".equals(methodName) && "(Ljava/lang/String;I)B".equals(methodDescriptor)) {
					frame.singleReturn = Byte.parseByte((String)frame.objectArguments[0], frame.intArguments[1]);
					return true;
				} else {
					return false;
				}
			} else if ("Character".equals(className)) {
				if ("digit".equals(methodName) && "(CI)I".equals(methodDescriptor)) {
					frame.singleReturn = Character.digit((char)frame.intArguments[0], frame.intArguments[1]);
					return true;
				} else if ("isDigit".equals(methodName) && "(C)Z".equals(methodDescriptor)) {
					frame.singleReturn = Character.isDigit((char)frame.intArguments[0]) ? 1 : 0;
					return true;
				} else if ("isLowerCase".equals(methodName) && "(C)Z".equals(methodDescriptor)) {
					frame.singleReturn = Character.isLowerCase((char)frame.intArguments[0]) ? 1 : 0;
					return true;
				} else if ("isUpperCase".equals(methodName) && "(C)Z".equals(methodDescriptor)) {
					frame.singleReturn = Character.isUpperCase((char)frame.intArguments[0]) ? 1 : 0;
					return true;
				} else if ("toLowerCase".equals(methodName) && "(C)C".equals(methodDescriptor)) {
					frame.singleReturn = Character.toLowerCase((char)frame.intArguments[0]);
					return true;
				} else if ("toUpperCase".equals(methodName) && "(C)C".equals(methodDescriptor)) {
					frame.singleReturn = Character.toUpperCase((char)frame.intArguments[0]);
					return true;
				} else {
					return false;
				}
			} else if ("Class".equals(className) && "forName".equals(methodName)) {
				frame.objectReturn = Class.forName((String)frame.objectArguments[0]);
				return true;
			} else if ("Double".equals(className)) {
				if ("isNaN".equals(methodName) && "(D)Z".equals(methodDescriptor)) {
					frame.singleReturn = Double.isNaN(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))) ? 1 : 0;
					return true;
				} else if ("valueOf".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/Double;".equals(methodDescriptor)) {
					frame.objectReturn = Double.valueOf((String)frame.objectArguments[0]);
					return true;
				} else if ("toString".equals(methodName) && "(D)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Double.toString(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0)));
					return true;
				} else if ("isInfinite".equals(methodName) && "(D)Z".equals(methodDescriptor)) {
					frame.singleReturn = Double.isInfinite(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))) ? 1 : 0;
					return true;
				} else if ("parseDouble".equals(methodName) && "(Ljava/lang/String;)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Double.parseDouble((String)frame.objectArguments[0]));
					return true;
				} else if ("doubleToLongBits".equals(methodName) && "(D)J".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0)));
					return true;
				} else if ("longBitsToDouble".equals(methodName) && "(J)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Double.longBitsToDouble(getLong(frame.intArguments, 0)));
					return true;
				} else {
					return false;
				}
			} else if ("Float".equals(className)) {
				if ("isNaN".equals(methodName) && "(F)Z".equals(methodDescriptor)) {
					frame.singleReturn = Float.isNaN(Float.intBitsToFloat(frame.intArguments[0])) ? 1 : 0;
					return true;
				} else if ("valueOf".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/Float;".equals(methodDescriptor)) {
					frame.objectReturn = Float.valueOf((String)frame.objectArguments[0]);
					return true;
				} else if ("toString".equals(methodName) && "(F)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Float.toString(Float.intBitsToFloat(frame.intArguments[0]));
					return true;
				} else if ("parseFloat".equals(methodName) && "(Ljava/lang/String;)F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Float.parseFloat((String)frame.objectArguments[0]));
					return true;
				} else if ("isInfinite".equals(methodName) && "(F)Z".equals(methodDescriptor)) {
					frame.singleReturn = Float.isInfinite(Float.intBitsToFloat(frame.intArguments[0])) ? 1 : 0;
					return true;
				} else if ("floatToIntBits".equals(methodName) && "(F)I".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Float.intBitsToFloat(frame.intArguments[0]));
					return true;
				} else if ("intBitsToFloat".equals(methodName) && "(I)F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Float.intBitsToFloat(frame.intArguments[0]));
					return true;
				} else {
					return false;
				}
			} else if ("Integer".equals(className)) {
				if ("valueOf".equals(methodName) && "(Ljava/lang/String;I)Ljava/lang/Integer;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.valueOf((String)frame.objectArguments[0], frame.intArguments[1]);
					return true;
				} else if ("valueOf".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/Integer;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.valueOf((String)frame.objectArguments[0]);
					return true;
				} else if ("toString".equals(methodName) && "(II)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.toString(frame.intArguments[0], frame.intArguments[1]);
					return true;
				} else if ("toString".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.toString(frame.intArguments[0]);
					return true;
				} else if ("parseInt".equals(methodName) && "(Ljava/lang/String;I)I".equals(methodDescriptor)) {
					frame.singleReturn = Integer.parseInt((String)frame.objectArguments[0], frame.intArguments[1]);
					return true;
				} else if ("parseInt".equals(methodName) && "(Ljava/lang/String;)I".equals(methodDescriptor)) {
					frame.singleReturn = Integer.parseInt((String)frame.objectArguments[0]);
					return true;
				} else if ("toHexString".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.toHexString(frame.intArguments[0]);
					return true;
				} else if ("toOctalString".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.toOctalString(frame.intArguments[0]);
					return true;
				} else if ("toBinaryString".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Integer.toBinaryString(frame.intArguments[0]);
					return true;
				} else {
					return false;
				}
			} else if ("Long".equals(className)) {
				if ("toString".equals(methodName) && "(JI)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Long.toString(getLong(frame.intArguments, 0), frame.intArguments[2]);
					return true;
				} else if ("toString".equals(methodName) && "(J)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = Long.toString(getLong(frame.intArguments, 0));
					return true;
				} else if ("parseLong".equals(methodName) && "(Ljava/lang/String;I)J".equals(methodDescriptor)) {
					frame.doubleReturn = Long.parseLong((String)frame.objectArguments[0], frame.intArguments[1]);
					return true;
				} else if ("parseLong".equals(methodName) && "(Ljava/lang/String;)J".equals(methodDescriptor)) {
					frame.doubleReturn = Long.parseLong((String)frame.objectArguments[0]);
					return true;
				} else {
					return false;
				}
			} else if ("Math".equals(className)) {
				if ("sin".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.sin(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("cos".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.cos(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("tan".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.tan(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("abs".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = Math.abs(frame.intArguments[0]);
					return true;
				} else if ("abs".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = Math.abs(getLong(frame.intArguments, 0));
					return true;
				} else if ("abs".equals(methodName) && "(F)F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Math.abs(Float.intBitsToFloat(frame.intArguments[0])));
					return true;
				} else if ("abs".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.abs(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("max".equals(methodName) && "(II)I".equals(methodDescriptor)) {
					frame.singleReturn = Math.max(frame.intArguments[0], frame.intArguments[1]);
					return true;
				} else if ("max".equals(methodName) && "(JJ)J".equals(methodDescriptor)) {
					frame.doubleReturn = Math.max(getLong(frame.intArguments, 0), getLong(frame.intArguments, 2));
					return true;
				} else if ("max".equals(methodName) && "(FF)F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Math.max(Float.intBitsToFloat(frame.intArguments[0]), Float.intBitsToFloat(frame.intArguments[1])));
					return true;
				} else if ("max".equals(methodName) && "(DD)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.max(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0)), Double.longBitsToDouble(Utils.getLong(frame.intArguments, 2))));
					return true;
				} else if ("min".equals(methodName) && "(II)I".equals(methodDescriptor)) {
					frame.singleReturn = Math.min(frame.intArguments[0], frame.intArguments[1]);
					return true;
				} else if ("min".equals(methodName) && "(JJ)J".equals(methodDescriptor)) {
					frame.doubleReturn = Math.min(getLong(frame.intArguments, 0), getLong(frame.intArguments, 2));
					return true;
				} else if ("min".equals(methodName) && "(FF)F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(Math.min(Float.intBitsToFloat(frame.intArguments[0]), Float.intBitsToFloat(frame.intArguments[1])));
					return true;
				} else if ("min".equals(methodName) && "(DD)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.min(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0)), Double.longBitsToDouble(Utils.getLong(frame.intArguments, 2))));
					return true;
				} else if ("sqrt".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.sqrt(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("ceil".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.ceil(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("floor".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.floor(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("toRadians".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.toRadians(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else if ("toDegrees".equals(methodName) && "(D)D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(Math.toDegrees(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0))));
					return true;
				} else {
					return false;
				}
			} else if ("Runtime".equals(className) && "getRuntime".equals(methodName)) {
				frame.objectReturn = Runtime.getRuntime();
				return true;
			} else if ("Short".equals(className)) {
				if ("parseShort".equals(methodName) && "(Ljava/lang/String;)S".equals(methodDescriptor)) {
					frame.singleReturn = Short.parseShort((String)frame.objectArguments[0]);
					return true;
				} else if ("parseShort".equals(methodName) && "(Ljava/lang/String;I)S".equals(methodDescriptor)) {
					frame.singleReturn = Short.parseShort((String)frame.objectArguments[0], frame.intArguments[1]);
					return true;
				} else {
					return false;
				}
			} else if ("String".equals(className)) {
				if ("valueOf".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf((Object)frame.objectArguments[0]);
					return true;
				} else if ("valueOf".equals(methodName) && "([C)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf((char[])frame.objectArguments[0]);
					return true;
				} else if ("valueOf".equals(methodName) && "([CII)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf((char[])frame.objectArguments[0], frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("valueOf".equals(methodName) && "(Z)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf(frame.intArguments[0] != 0);
					return true;
				} else if ("valueOf".equals(methodName) && "(C)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf((char)frame.intArguments[0]);
					return true;
				} else if ("valueOf".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf(frame.intArguments[0]);
					return true;
				} else if ("valueOf".equals(methodName) && "(J)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf(getLong(frame.intArguments, 0));
					return true;
				} else if ("valueOf".equals(methodName) && "(F)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf(Float.intBitsToFloat(frame.intArguments[0]));
					return true;
				} else if ("valueOf".equals(methodName) && "(D)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = String.valueOf(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 0)));
					return true;
				} else {
					return false;
				}
			} else if ("System".equals(className)) {
				if ("gc".equals(methodName) && "()V".equals(methodDescriptor)) {
					System.gc();
					return true;
				} else if ("exit".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					System.exit(frame.intArguments[0]);
					return true;
				} else if ("arraycopy".equals(methodName) && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(methodDescriptor)) {
					System.arraycopy((Object)frame.objectArguments[0], frame.intArguments[1], (Object)frame.objectArguments[2], frame.intArguments[3], frame.intArguments[4]);
					return true;
				} else if ("getProperty".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = System.getProperty((String)frame.objectArguments[0]);
					return true;
				} else if ("identityHashCode".equals(methodName) && "(Ljava/lang/Object;)I".equals(methodDescriptor)) {
					frame.singleReturn = System.identityHashCode((Object)frame.objectArguments[0]);
					return true;
				} else if ("currentTimeMillis".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = System.currentTimeMillis();
					return true;
				} else {
					return false;
				}
			}
		} else if ("java/io".equals(packageName)) {
			if ("DataInputStream".equals(className) && "readUTF".equals(methodName)) {
				frame.objectReturn = DataInputStream.readUTF((DataInput)frame.objectArguments[0]);
				return true;
			}
		}
		// }
		// replace existing classes to add special code
		if ("java/lang".equals(packageName)) {
			if ("Thread".equals(className)) {
				if ("currentThread".equals(methodName) && "()Ljava/lang/Thread;".equals(methodDescriptor)) {
					frame.objectReturn = Thread.currentThread(frame);
					return true;
				} else if ("yield".equals(methodName) && "()V".equals(methodDescriptor)) {
					Thread.yield();
					return true;
				} else if ("sleep".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					Thread.sleep(frame, getLong(frame.intArguments, 0));
					return true;
				} else if ("activeCount".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = Thread.activeCount(frame);
					return true;
				}
			}
		} else if ("java/lang/reflect".equals(packageName)) {
			if ("Array".equals(className)) {
				if ("newInstance".equals(methodName) && "(Ljava/lang/Class;[I)Ljava/lang/Object;".equals(methodDescriptor)) {
					Class componentType = (Class)frame.objectArguments[0];
					int[] dimensions = (int[])frame.objectArguments[1];
					frame.objectReturn = multiNewArray(componentType, dimensions);
					return true;
				}
			}
		}
		return false;
	}

	protected Object multiNewArray(final Class componentType, final int[] dimensions) {
		try {
			if (componentType == Boolean.class) {
				switch (dimensions.length) {
					case 1:
						return new boolean[dimensions[0]];
					case 2:
						return new boolean[dimensions[0]][dimensions[1]];
					case 3:
						return new boolean[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[Z")) {
				switch (dimensions.length) {
					case 1:
						return new boolean[dimensions[0]][];
					case 2:
						return new boolean[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Byte.class) {
				switch (dimensions.length) {
					case 1:
						return new byte[dimensions[0]];
					case 2:
						return new byte[dimensions[0]][dimensions[1]];
					case 3:
						return new byte[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[B")) {
				switch (dimensions.length) {
					case 1:
						return new byte[dimensions[0]][];
					case 2:
						return new byte[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Short.class) {
				switch (dimensions.length) {
					case 1:
						return new short[dimensions[0]];
					case 2:
						return new short[dimensions[0]][dimensions[1]];
					case 3:
						return new short[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[S")) {
				switch (dimensions.length) {
					case 1:
						return new short[dimensions[0]][];
					case 2:
						return new short[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Integer.class) {
				switch (dimensions.length) {
					case 1:
						return new int[dimensions[0]];
					case 2:
						return new int[dimensions[0]][dimensions[1]];
					case 3:
						return new int[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[I")) {
				switch (dimensions.length) {
					case 1:
						return new int[dimensions[0]][];
					case 2:
						return new int[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Long.class) {
				switch (dimensions.length) {
					case 1:
						return new long[dimensions[0]];
					case 2:
						return new long[dimensions[0]][dimensions[1]];
					case 3:
						return new long[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[J")) {
				switch (dimensions.length) {
					case 1:
						return new long[dimensions[0]][];
					case 2:
						return new long[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Float.class) {
				switch (dimensions.length) {
					case 1:
						return new float[dimensions[0]];
					case 2:
						return new float[dimensions[0]][dimensions[1]];
					case 3:
						return new float[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[F")) {
				switch (dimensions.length) {
					case 1:
						return new float[dimensions[0]][];
					case 2:
						return new float[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Double.class) {
				switch (dimensions.length) {
					case 1:
						return new double[dimensions[0]];
					case 2:
						return new double[dimensions[0]][dimensions[1]];
					case 3:
						return new double[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[D")) {
				switch (dimensions.length) {
					case 1:
						return new double[dimensions[0]][];
					case 2:
						return new double[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Character.class) {
				switch (dimensions.length) {
					case 1:
						return new char[dimensions[0]];
					case 2:
						return new char[dimensions[0]][dimensions[1]];
					case 3:
						return new char[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Class.forName("[C")) {
				switch (dimensions.length) {
					case 1:
						return new char[dimensions[0]][];
					case 2:
						return new char[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Object.class) {
				switch (dimensions.length) {
					case 1:
						return new Object[dimensions[0]];
					case 2:
						return new Object[dimensions[0]][dimensions[1]];
					case 3:
						return new Object[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Object[].class) {
				switch (dimensions.length) {
					case 1:
						return new Object[dimensions[0]][];
					case 2:
						return new Object[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == String.class) {
				switch (dimensions.length) {
					case 1:
						return new String[dimensions[0]];
					case 2:
						return new String[dimensions[0]][dimensions[1]];
					case 3:
						return new String[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == String[].class) {
				switch (dimensions.length) {
					case 1:
						return new String[dimensions[0]][];
					case 2:
						return new String[dimensions[0]][dimensions[1]][];
				}
			} else if (componentType == Vector.class) {
				switch (dimensions.length) {
					case 1:
						return new Vector[dimensions[0]];
					case 2:
						return new Vector[dimensions[0]][dimensions[1]];
					case 3:
						return new Vector[dimensions[0]][dimensions[1]][dimensions[2]];
				}
			} else if (componentType == Vector[].class) {
				switch (dimensions.length) {
					case 1:
						return new Vector[dimensions[0]][];
					case 2:
						return new Vector[dimensions[0]][dimensions[1]][];
				}
			}
		} catch (ClassNotFoundException e) {
		}
		// TODO Add types
		throw new IllegalArgumentException("not supported array type: " + componentType.getName());
	}

	protected boolean handleInstanceMethod(final Frame frame, final String absoluteClassName, final String methodName, final String methodDescriptor) throws Exception {
		// INSTANCE METHOD SECTION {
		String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
		String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
		if ("java/util".equals(packageName)) {
			if ("Calendar".equals(className)) {
				if ("get".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).get(frame.intArguments[1]);
					return true;
				} else if ("set".equals(methodName) && "(II)V".equals(methodDescriptor)) {
					((Calendar)toTargetInstance(frame.objectArguments[0])).set(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("after".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).after((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("before".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).before((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("getTime".equals(methodName) && "()Ljava/util/Date;".equals(methodDescriptor)) {
					frame.objectReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).getTime();
					return true;
				} else if ("setTime".equals(methodName) && "(Ljava/util/Date;)V".equals(methodDescriptor)) {
					((Calendar)toTargetInstance(frame.objectArguments[0])).setTime((Date)frame.objectArguments[1]);
					return true;
				} else if ("setTimeZone".equals(methodName) && "(Ljava/util/TimeZone;)V".equals(methodDescriptor)) {
					((Calendar)toTargetInstance(frame.objectArguments[0])).setTimeZone((TimeZone)frame.objectArguments[1]);
					return true;
				} else if ("getTimeZone".equals(methodName) && "()Ljava/util/TimeZone;".equals(methodDescriptor)) {
					frame.objectReturn = ((Calendar)toTargetInstance(frame.objectArguments[0])).getTimeZone();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Date".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Date)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("getTime".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Date)toTargetInstance(frame.objectArguments[0])).getTime();
					return true;
				} else if ("setTime".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((Date)toTargetInstance(frame.objectArguments[0])).setTime(getLong(frame.intArguments, 1));
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Date)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Date)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("EmptyStackException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("Hashtable".equals(className)) {
				if ("get".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).get((Object)frame.objectArguments[1]);
					return true;
				} else if ("put".equals(methodName) && "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).put((Object)frame.objectArguments[1], (Object)frame.objectArguments[2]);
					return true;
				} else if ("size".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).size();
					return true;
				} else if ("keys".equals(methodName) && "()Ljava/util/Enumeration;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).keys();
					return true;
				} else if ("clear".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Hashtable)toTargetInstance(frame.objectArguments[0])).clear();
					return true;
				} else if ("remove".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).remove((Object)frame.objectArguments[1]);
					return true;
				} else if ("isEmpty".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).isEmpty() ? 1 : 0;
					return true;
				} else if ("elements".equals(methodName) && "()Ljava/util/Enumeration;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).elements();
					return true;
				} else if ("contains".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).contains((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("containsKey".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Hashtable)toTargetInstance(frame.objectArguments[0])).containsKey((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("NoSuchElementException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("Random".equals(className)) {
				if ("setSeed".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((Random)toTargetInstance(frame.objectArguments[0])).setSeed(getLong(frame.intArguments, 1));
					return true;
				} else if ("nextInt".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Random)toTargetInstance(frame.objectArguments[0])).nextInt();
					return true;
				} else if ("nextInt".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Random)toTargetInstance(frame.objectArguments[0])).nextInt(frame.intArguments[1]);
					return true;
				} else if ("nextLong".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Random)toTargetInstance(frame.objectArguments[0])).nextLong();
					return true;
				} else if ("nextFloat".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((Random)toTargetInstance(frame.objectArguments[0])).nextFloat());
					return true;
				} else if ("nextDouble".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((Random)toTargetInstance(frame.objectArguments[0])).nextDouble());
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Stack".equals(className)) {
				if ("pop".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Stack)toTargetInstance(frame.objectArguments[0])).pop();
					return true;
				} else if ("push".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Stack)toTargetInstance(frame.objectArguments[0])).push((Object)frame.objectArguments[1]);
					return true;
				} else if ("peek".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Stack)toTargetInstance(frame.objectArguments[0])).peek();
					return true;
				} else if ("empty".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Stack)toTargetInstance(frame.objectArguments[0])).empty() ? 1 : 0;
					return true;
				} else if ("search".equals(methodName) && "(Ljava/lang/Object;)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Stack)toTargetInstance(frame.objectArguments[0])).search((Object)frame.objectArguments[1]);
					return true;
				} else {
					return handleInstanceMethod(frame, "java/util/Vector", methodName, methodDescriptor);
				}
			} else if ("TimeZone".equals(className)) {
				if ("getID".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((TimeZone)toTargetInstance(frame.objectArguments[0])).getID();
					return true;
				} else if ("getOffset".equals(methodName) && "(IIIIII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((TimeZone)toTargetInstance(frame.objectArguments[0])).getOffset(frame.intArguments[1], frame.intArguments[2], frame.intArguments[3], frame.intArguments[4], frame.intArguments[5], frame.intArguments[6]);
					return true;
				} else if ("getRawOffset".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((TimeZone)toTargetInstance(frame.objectArguments[0])).getRawOffset();
					return true;
				} else if ("useDaylightTime".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((TimeZone)toTargetInstance(frame.objectArguments[0])).useDaylightTime() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Vector".equals(className)) {
				if ("size".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).size();
					return true;
				} else if ("setSize".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).setSize(frame.intArguments[1]);
					return true;
				} else if ("isEmpty".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).isEmpty() ? 1 : 0;
					return true;
				} else if ("indexOf".equals(methodName) && "(Ljava/lang/Object;)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).indexOf((Object)frame.objectArguments[1]);
					return true;
				} else if ("indexOf".equals(methodName) && "(Ljava/lang/Object;I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).indexOf((Object)frame.objectArguments[1], frame.intArguments[2]);
					return true;
				} else if ("copyInto".equals(methodName) && "([Ljava/lang/Object;)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).copyInto((Object[])frame.objectArguments[1]);
					return true;
				} else if ("capacity".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).capacity();
					return true;
				} else if ("elements".equals(methodName) && "()Ljava/util/Enumeration;".equals(methodDescriptor)) {
					frame.objectReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).elements();
					return true;
				} else if ("contains".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).contains((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("elementAt".equals(methodName) && "(I)Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).elementAt(frame.intArguments[1]);
					return true;
				} else if ("trimToSize".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).trimToSize();
					return true;
				} else if ("addElement".equals(methodName) && "(Ljava/lang/Object;)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).addElement((Object)frame.objectArguments[1]);
					return true;
				} else if ("lastIndexOf".equals(methodName) && "(Ljava/lang/Object;)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).lastIndexOf((Object)frame.objectArguments[1]);
					return true;
				} else if ("lastIndexOf".equals(methodName) && "(Ljava/lang/Object;I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).lastIndexOf((Object)frame.objectArguments[1], frame.intArguments[2]);
					return true;
				} else if ("lastElement".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).lastElement();
					return true;
				} else if ("firstElement".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).firstElement();
					return true;
				} else if ("setElementAt".equals(methodName) && "(Ljava/lang/Object;I)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).setElementAt((Object)frame.objectArguments[1], frame.intArguments[2]);
					return true;
				} else if ("removeElement".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Vector)toTargetInstance(frame.objectArguments[0])).removeElement((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("ensureCapacity".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).ensureCapacity(frame.intArguments[1]);
					return true;
				} else if ("removeElementAt".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).removeElementAt(frame.intArguments[1]);
					return true;
				} else if ("insertElementAt".equals(methodName) && "(Ljava/lang/Object;I)V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).insertElementAt((Object)frame.objectArguments[1], frame.intArguments[2]);
					return true;
				} else if ("removeAllElements".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Vector)toTargetInstance(frame.objectArguments[0])).removeAllElements();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			}
		} else if ("java/lang/ref".equals(packageName)) {
			if ("Reference".equals(className)) {
				if ("get".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Reference)toTargetInstance(frame.objectArguments[0])).get();
					return true;
				} else if ("clear".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Reference)toTargetInstance(frame.objectArguments[0])).clear();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("WeakReference".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/ref/Reference", methodName, methodDescriptor);
			}
		} else if ("java/lang".equals(packageName)) {
			if ("ArithmeticException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("ArrayIndexOutOfBoundsException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/IndexOutOfBoundsException", methodName, methodDescriptor);
			} else if ("ArrayStoreException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("Boolean".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Boolean)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Boolean)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Boolean)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("booleanValue".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Boolean)toTargetInstance(frame.objectArguments[0])).booleanValue() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Byte".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Byte)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Byte)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Byte)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("byteValue".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((Byte)toTargetInstance(frame.objectArguments[0])).byteValue();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Character".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Character)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Character)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Character)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("charValue".equals(methodName) && "()C".equals(methodDescriptor)) {
					frame.singleReturn = ((Character)toTargetInstance(frame.objectArguments[0])).charValue();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Class".equals(className)) {
				if ("isArray".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Class)toTargetInstance(frame.objectArguments[0])).isArray() ? 1 : 0;
					return true;
				} else if ("getName".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Class)toTargetInstance(frame.objectArguments[0])).getName();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Class)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("isInstance".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Class)toTargetInstance(frame.objectArguments[0])).isInstance((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("newInstance".equals(methodName) && "()Ljava/lang/Object;".equals(methodDescriptor)) {
					frame.objectReturn = ((Class)toTargetInstance(frame.objectArguments[0])).newInstance();
					return true;
				} else if ("isInterface".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Class)toTargetInstance(frame.objectArguments[0])).isInterface() ? 1 : 0;
					return true;
				} else if ("isAssignableFrom".equals(methodName) && "(Ljava/lang/Class;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Class)toTargetInstance(frame.objectArguments[0])).isAssignableFrom((Class)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("getResourceAsStream".equals(methodName) && "(Ljava/lang/String;)Ljava/io/InputStream;".equals(methodDescriptor)) {
					frame.objectReturn = ((Class)toTargetInstance(frame.objectArguments[0])).getResourceAsStream((String)frame.objectArguments[1]);
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("ClassCastException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("ClassNotFoundException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("Double".equals(className)) {
				if ("isNaN".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).isNaN() ? 1 : 0;
					return true;
				} else if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Double)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("intValue".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).intValue();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("byteValue".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).byteValue();
					return true;
				} else if ("longValue".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).longValue();
					return true;
				} else if ("isInfinite".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).isInfinite() ? 1 : 0;
					return true;
				} else if ("shortValue".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((Double)toTargetInstance(frame.objectArguments[0])).shortValue();
					return true;
				} else if ("floatValue".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((Double)toTargetInstance(frame.objectArguments[0])).floatValue());
					return true;
				} else if ("doubleValue".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((Double)toTargetInstance(frame.objectArguments[0])).doubleValue());
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Error".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Throwable", methodName, methodDescriptor);
			} else if ("Exception".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Throwable", methodName, methodDescriptor);
			} else if ("Float".equals(className)) {
				if ("isNaN".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).isNaN() ? 1 : 0;
					return true;
				} else if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Float)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("intValue".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).intValue();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("byteValue".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).byteValue();
					return true;
				} else if ("longValue".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).longValue();
					return true;
				} else if ("isInfinite".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).isInfinite() ? 1 : 0;
					return true;
				} else if ("shortValue".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((Float)toTargetInstance(frame.objectArguments[0])).shortValue();
					return true;
				} else if ("floatValue".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((Float)toTargetInstance(frame.objectArguments[0])).floatValue());
					return true;
				} else if ("doubleValue".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((Float)toTargetInstance(frame.objectArguments[0])).doubleValue());
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("IllegalAccessException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("IllegalArgumentException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("IllegalMonitorStateException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("IllegalThreadStateException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/IllegalArgumentException", methodName, methodDescriptor);
			} else if ("IndexOutOfBoundsException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("InstantiationException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("Integer".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("intValue".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).intValue();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("byteValue".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).byteValue();
					return true;
				} else if ("longValue".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).longValue();
					return true;
				} else if ("shortValue".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((Integer)toTargetInstance(frame.objectArguments[0])).shortValue();
					return true;
				} else if ("floatValue".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((Integer)toTargetInstance(frame.objectArguments[0])).floatValue());
					return true;
				} else if ("doubleValue".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((Integer)toTargetInstance(frame.objectArguments[0])).doubleValue());
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("InterruptedException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("Long".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Long)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Long)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Long)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("longValue".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Long)toTargetInstance(frame.objectArguments[0])).longValue();
					return true;
				} else if ("floatValue".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((Long)toTargetInstance(frame.objectArguments[0])).floatValue());
					return true;
				} else if ("doubleValue".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((Long)toTargetInstance(frame.objectArguments[0])).doubleValue());
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("Math".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
			} else if ("NegativeArraySizeException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("NoClassDefFoundError".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Error", methodName, methodDescriptor);
			} else if ("NullPointerException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("NumberFormatException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/IllegalArgumentException", methodName, methodDescriptor);
			} else if ("OutOfMemoryError".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/VirtualMachineError", methodName, methodDescriptor);
			} else if ("Runtime".equals(className)) {
				if ("gc".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Runtime)toTargetInstance(frame.objectArguments[0])).gc();
					return true;
				} else if ("exit".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Runtime)toTargetInstance(frame.objectArguments[0])).exit(frame.intArguments[1]);
					return true;
				} else if ("freeMemory".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Runtime)toTargetInstance(frame.objectArguments[0])).freeMemory();
					return true;
				} else if ("totalMemory".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Runtime)toTargetInstance(frame.objectArguments[0])).totalMemory();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("RuntimeException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("SecurityException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/RuntimeException", methodName, methodDescriptor);
			} else if ("Short".equals(className)) {
				if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Short)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Short)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Short)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("shortValue".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((Short)toTargetInstance(frame.objectArguments[0])).shortValue();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("String".equals(className)) {
				if ("trim".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).trim();
					return true;
				} else if ("length".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).length();
					return true;
				} else if ("charAt".equals(methodName) && "(I)C".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).charAt(frame.intArguments[1]);
					return true;
				} else if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).equals((Object)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("concat".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).concat((String)frame.objectArguments[1]);
					return true;
				} else if ("intern".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).intern();
					return true;
				} else if ("indexOf".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).indexOf(frame.intArguments[1]);
					return true;
				} else if ("indexOf".equals(methodName) && "(II)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).indexOf(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("indexOf".equals(methodName) && "(Ljava/lang/String;)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).indexOf((String)frame.objectArguments[1]);
					return true;
				} else if ("indexOf".equals(methodName) && "(Ljava/lang/String;I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).indexOf((String)frame.objectArguments[1], frame.intArguments[2]);
					return true;
				} else if ("replace".equals(methodName) && "(CC)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).replace((char)frame.intArguments[1], (char)frame.intArguments[2]);
					return true;
				} else if ("getChars".equals(methodName) && "(II[CI)V".equals(methodDescriptor)) {
					((String)toTargetInstance(frame.objectArguments[0])).getChars(frame.intArguments[1], frame.intArguments[2], (char[])frame.objectArguments[3], frame.intArguments[4]);
					return true;
				} else if ("getBytes".equals(methodName) && "(Ljava/lang/String;)[B".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).getBytes((String)frame.objectArguments[1]);
					return true;
				} else if ("getBytes".equals(methodName) && "()[B".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).getBytes();
					return true;
				} else if ("endsWith".equals(methodName) && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).endsWith((String)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).hashCode();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("compareTo".equals(methodName) && "(Ljava/lang/String;)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).compareTo((String)frame.objectArguments[1]);
					return true;
				} else if ("substring".equals(methodName) && "(I)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).substring(frame.intArguments[1]);
					return true;
				} else if ("substring".equals(methodName) && "(II)Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).substring(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("startsWith".equals(methodName) && "(Ljava/lang/String;I)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).startsWith((String)frame.objectArguments[1], frame.intArguments[2]) ? 1 : 0;
					return true;
				} else if ("startsWith".equals(methodName) && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).startsWith((String)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else if ("lastIndexOf".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).lastIndexOf(frame.intArguments[1]);
					return true;
				} else if ("lastIndexOf".equals(methodName) && "(II)I".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).lastIndexOf(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("toLowerCase".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).toLowerCase();
					return true;
				} else if ("toUpperCase".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).toUpperCase();
					return true;
				} else if ("toCharArray".equals(methodName) && "()[C".equals(methodDescriptor)) {
					frame.objectReturn = ((String)toTargetInstance(frame.objectArguments[0])).toCharArray();
					return true;
				} else if ("regionMatches".equals(methodName) && "(ZILjava/lang/String;II)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).regionMatches(frame.intArguments[1] != 0, frame.intArguments[2], (String)frame.objectArguments[3], frame.intArguments[4], frame.intArguments[5]) ? 1 : 0;
					return true;
				} else if ("equalsIgnoreCase".equals(methodName) && "(Ljava/lang/String;)Z".equals(methodDescriptor)) {
					frame.singleReturn = ((String)toTargetInstance(frame.objectArguments[0])).equalsIgnoreCase((String)frame.objectArguments[1]) ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("StringBuffer".equals(className)) {
				if ("length".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).length();
					return true;
				} else if ("charAt".equals(methodName) && "(I)C".equals(methodDescriptor)) {
					frame.singleReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).charAt(frame.intArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append((Object)frame.objectArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "(Ljava/lang/String;)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append((String)frame.objectArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "([C)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append((char[])frame.objectArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "([CII)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("append".equals(methodName) && "(Z)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append(frame.intArguments[1] != 0);
					return true;
				} else if ("append".equals(methodName) && "(C)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append((char)frame.intArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "(I)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append(frame.intArguments[1]);
					return true;
				} else if ("append".equals(methodName) && "(J)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append(getLong(frame.intArguments, 1));
					return true;
				} else if ("append".equals(methodName) && "(F)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append(Float.intBitsToFloat(frame.intArguments[1]));
					return true;
				} else if ("append".equals(methodName) && "(D)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).append(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1)));
					return true;
				} else if ("delete".equals(methodName) && "(II)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).delete(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(ILjava/lang/Object;)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], (Object)frame.objectArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(ILjava/lang/String;)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], (String)frame.objectArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(I[C)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], (char[])frame.objectArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(IZ)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], frame.intArguments[2] != 0);
					return true;
				} else if ("insert".equals(methodName) && "(IC)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], (char)frame.intArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(II)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], frame.intArguments[2]);
					return true;
				} else if ("insert".equals(methodName) && "(IJ)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], getLong(frame.intArguments, 2));
					return true;
				} else if ("insert".equals(methodName) && "(IF)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], Float.intBitsToFloat(frame.intArguments[2]));
					return true;
				} else if ("insert".equals(methodName) && "(ID)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).insert(frame.intArguments[1], Double.longBitsToDouble(Utils.getLong(frame.intArguments, 2)));
					return true;
				} else if ("reverse".equals(methodName) && "()Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).reverse();
					return true;
				} else if ("capacity".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).capacity();
					return true;
				} else if ("getChars".equals(methodName) && "(II[CI)V".equals(methodDescriptor)) {
					((StringBuffer)toTargetInstance(frame.objectArguments[0])).getChars(frame.intArguments[1], frame.intArguments[2], (char[])frame.objectArguments[3], frame.intArguments[4]);
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("setLength".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((StringBuffer)toTargetInstance(frame.objectArguments[0])).setLength(frame.intArguments[1]);
					return true;
				} else if ("setCharAt".equals(methodName) && "(IC)V".equals(methodDescriptor)) {
					((StringBuffer)toTargetInstance(frame.objectArguments[0])).setCharAt(frame.intArguments[1], (char)frame.intArguments[2]);
					return true;
				} else if ("deleteCharAt".equals(methodName) && "(I)Ljava/lang/StringBuffer;".equals(methodDescriptor)) {
					frame.objectReturn = ((StringBuffer)toTargetInstance(frame.objectArguments[0])).deleteCharAt(frame.intArguments[1]);
					return true;
				} else if ("ensureCapacity".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((StringBuffer)toTargetInstance(frame.objectArguments[0])).ensureCapacity(frame.intArguments[1]);
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("StringIndexOutOfBoundsException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/IndexOutOfBoundsException", methodName, methodDescriptor);
			} else if ("System".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
			} else if ("Throwable".equals(className)) {
				if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Throwable)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("getMessage".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Throwable)toTargetInstance(frame.objectArguments[0])).getMessage();
					return true;
				} else if ("printStackTrace".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Throwable)toTargetInstance(frame.objectArguments[0])).printStackTrace();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("VirtualMachineError".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Error", methodName, methodDescriptor);
			}
		} else if ("java/io".equals(packageName)) {
			if ("ByteArrayInputStream".equals(className)) {
				if ("read".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).read();
					return true;
				} else if ("read".equals(methodName) && "([BII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).read((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skip".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = ((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).skip(getLong(frame.intArguments, 1));
					return true;
				} else if ("mark".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).mark(frame.intArguments[1]);
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("available".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).available();
					return true;
				} else if ("markSupported".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((ByteArrayInputStream)toTargetInstance(frame.objectArguments[0])).markSupported() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/InputStream", methodName, methodDescriptor);
				}
			} else if ("ByteArrayOutputStream".equals(className)) {
				if ("size".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).size();
					return true;
				} else if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).toString();
					return true;
				} else if ("toByteArray".equals(methodName) && "()[B".equals(methodDescriptor)) {
					frame.objectReturn = ((ByteArrayOutputStream)toTargetInstance(frame.objectArguments[0])).toByteArray();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/OutputStream", methodName, methodDescriptor);
				}
			} else if ("DataInputStream".equals(className)) {
				if ("read".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).read();
					return true;
				} else if ("read".equals(methodName) && "([B)I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).read((byte[])frame.objectArguments[1]);
					return true;
				} else if ("read".equals(methodName) && "([BII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).read((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skip".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).skip(getLong(frame.intArguments, 1));
					return true;
				} else if ("mark".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataInputStream)toTargetInstance(frame.objectArguments[0])).mark(frame.intArguments[1]);
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((DataInputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((DataInputStream)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("readInt".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readInt();
					return true;
				} else if ("readUTF".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readUTF();
					return true;
				} else if ("readByte".equals(methodName) && "()B".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readByte();
					return true;
				} else if ("readChar".equals(methodName) && "()C".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readChar();
					return true;
				} else if ("readLong".equals(methodName) && "()J".equals(methodDescriptor)) {
					frame.doubleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readLong();
					return true;
				} else if ("readFully".equals(methodName) && "([B)V".equals(methodDescriptor)) {
					((DataInputStream)toTargetInstance(frame.objectArguments[0])).readFully((byte[])frame.objectArguments[1]);
					return true;
				} else if ("readFully".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((DataInputStream)toTargetInstance(frame.objectArguments[0])).readFully((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skipBytes".equals(methodName) && "(I)I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).skipBytes(frame.intArguments[1]);
					return true;
				} else if ("readShort".equals(methodName) && "()S".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readShort();
					return true;
				} else if ("readFloat".equals(methodName) && "()F".equals(methodDescriptor)) {
					frame.singleReturn = Float.floatToIntBits(((DataInputStream)toTargetInstance(frame.objectArguments[0])).readFloat());
					return true;
				} else if ("available".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).available();
					return true;
				} else if ("readDouble".equals(methodName) && "()D".equals(methodDescriptor)) {
					frame.doubleReturn = Double.doubleToLongBits(((DataInputStream)toTargetInstance(frame.objectArguments[0])).readDouble());
					return true;
				} else if ("readBoolean".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readBoolean() ? 1 : 0;
					return true;
				} else if ("markSupported".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).markSupported() ? 1 : 0;
					return true;
				} else if ("readUnsignedByte".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readUnsignedByte();
					return true;
				} else if ("readUnsignedShort".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((DataInputStream)toTargetInstance(frame.objectArguments[0])).readUnsignedShort();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/InputStream", methodName, methodDescriptor);
				}
			} else if ("DataOutputStream".equals(className)) {
				if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("flush".equals(methodName) && "()V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).flush();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("writeInt".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeInt(frame.intArguments[1]);
					return true;
				} else if ("writeUTF".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeUTF((String)frame.objectArguments[1]);
					return true;
				} else if ("writeByte".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeByte(frame.intArguments[1]);
					return true;
				} else if ("writeChar".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeChar(frame.intArguments[1]);
					return true;
				} else if ("writeLong".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeLong(getLong(frame.intArguments, 1));
					return true;
				} else if ("writeShort".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeShort(frame.intArguments[1]);
					return true;
				} else if ("writeFloat".equals(methodName) && "(F)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeFloat(Float.intBitsToFloat(frame.intArguments[1]));
					return true;
				} else if ("writeChars".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeChars((String)frame.objectArguments[1]);
					return true;
				} else if ("writeDouble".equals(methodName) && "(D)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeDouble(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1)));
					return true;
				} else if ("writeBoolean".equals(methodName) && "(Z)V".equals(methodDescriptor)) {
					((DataOutputStream)toTargetInstance(frame.objectArguments[0])).writeBoolean(frame.intArguments[1] != 0);
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/OutputStream", methodName, methodDescriptor);
				}
			} else if ("EOFException".equals(className)) {
				return handleInstanceMethod(frame, "java/io/IOException", methodName, methodDescriptor);
			} else if ("InputStream".equals(className)) {
				if ("read".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).read();
					return true;
				} else if ("read".equals(methodName) && "([B)I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).read((byte[])frame.objectArguments[1]);
					return true;
				} else if ("read".equals(methodName) && "([BII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).read((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skip".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).skip(getLong(frame.intArguments, 1));
					return true;
				} else if ("mark".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((InputStream)toTargetInstance(frame.objectArguments[0])).mark(frame.intArguments[1]);
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((InputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((InputStream)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("available".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).available();
					return true;
				} else if ("markSupported".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStream)toTargetInstance(frame.objectArguments[0])).markSupported() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("InputStreamReader".equals(className)) {
				if ("read".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStreamReader)toTargetInstance(frame.objectArguments[0])).read();
					return true;
				} else if ("read".equals(methodName) && "([CII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStreamReader)toTargetInstance(frame.objectArguments[0])).read((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skip".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = ((InputStreamReader)toTargetInstance(frame.objectArguments[0])).skip(getLong(frame.intArguments, 1));
					return true;
				} else if ("mark".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((InputStreamReader)toTargetInstance(frame.objectArguments[0])).mark(frame.intArguments[1]);
					return true;
				} else if ("ready".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStreamReader)toTargetInstance(frame.objectArguments[0])).ready() ? 1 : 0;
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((InputStreamReader)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((InputStreamReader)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("markSupported".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((InputStreamReader)toTargetInstance(frame.objectArguments[0])).markSupported() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/Reader", methodName, methodDescriptor);
				}
			} else if ("InterruptedIOException".equals(className)) {
				return handleInstanceMethod(frame, "java/io/IOException", methodName, methodDescriptor);
			} else if ("IOException".equals(className)) {
				return handleInstanceMethod(frame, "java/lang/Exception", methodName, methodDescriptor);
			} else if ("OutputStream".equals(className)) {
				if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((OutputStream)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([B)V".equals(methodDescriptor)) {
					((OutputStream)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((OutputStream)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("flush".equals(methodName) && "()V".equals(methodDescriptor)) {
					((OutputStream)toTargetInstance(frame.objectArguments[0])).flush();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((OutputStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("OutputStreamWriter".equals(className)) {
				if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((OutputStreamWriter)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([CII)V".equals(methodDescriptor)) {
					((OutputStreamWriter)toTargetInstance(frame.objectArguments[0])).write((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("write".equals(methodName) && "(Ljava/lang/String;II)V".equals(methodDescriptor)) {
					((OutputStreamWriter)toTargetInstance(frame.objectArguments[0])).write((String)frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("flush".equals(methodName) && "()V".equals(methodDescriptor)) {
					((OutputStreamWriter)toTargetInstance(frame.objectArguments[0])).flush();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((OutputStreamWriter)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/Writer", methodName, methodDescriptor);
				}
			} else if ("PrintStream".equals(className)) {
				if ("flush".equals(methodName) && "()V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).flush();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([BII)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).write((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("print".equals(methodName) && "(Z)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print(frame.intArguments[1] != 0);
					return true;
				} else if ("print".equals(methodName) && "(C)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print((char)frame.intArguments[1]);
					return true;
				} else if ("print".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print(frame.intArguments[1]);
					return true;
				} else if ("print".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print(getLong(frame.intArguments, 1));
					return true;
				} else if ("print".equals(methodName) && "(F)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print(Float.intBitsToFloat(frame.intArguments[1]));
					return true;
				} else if ("print".equals(methodName) && "(D)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1)));
					return true;
				} else if ("print".equals(methodName) && "([C)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print((char[])frame.objectArguments[1]);
					return true;
				} else if ("print".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print((String)frame.objectArguments[1]);
					return true;
				} else if ("print".equals(methodName) && "(Ljava/lang/Object;)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).print((Object)frame.objectArguments[1]);
					return true;
				} else if ("println".equals(methodName) && "()V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println();
					return true;
				} else if ("println".equals(methodName) && "(Z)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println(frame.intArguments[1] != 0);
					return true;
				} else if ("println".equals(methodName) && "(C)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println((char)frame.intArguments[1]);
					return true;
				} else if ("println".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println(frame.intArguments[1]);
					return true;
				} else if ("println".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println(getLong(frame.intArguments, 1));
					return true;
				} else if ("println".equals(methodName) && "(F)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println(Float.intBitsToFloat(frame.intArguments[1]));
					return true;
				} else if ("println".equals(methodName) && "(D)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1)));
					return true;
				} else if ("println".equals(methodName) && "([C)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println((char[])frame.objectArguments[1]);
					return true;
				} else if ("println".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println((String)frame.objectArguments[1]);
					return true;
				} else if ("println".equals(methodName) && "(Ljava/lang/Object;)V".equals(methodDescriptor)) {
					((PrintStream)toTargetInstance(frame.objectArguments[0])).println((Object)frame.objectArguments[1]);
					return true;
				} else if ("checkError".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((PrintStream)toTargetInstance(frame.objectArguments[0])).checkError() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/io/OutputStream", methodName, methodDescriptor);
				}
			} else if ("Reader".equals(className)) {
				if ("read".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).read();
					return true;
				} else if ("read".equals(methodName) && "([C)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).read((char[])frame.objectArguments[1]);
					return true;
				} else if ("read".equals(methodName) && "([CII)I".equals(methodDescriptor)) {
					frame.singleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).read((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("skip".equals(methodName) && "(J)J".equals(methodDescriptor)) {
					frame.doubleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).skip(getLong(frame.intArguments, 1));
					return true;
				} else if ("mark".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Reader)toTargetInstance(frame.objectArguments[0])).mark(frame.intArguments[1]);
					return true;
				} else if ("ready".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).ready() ? 1 : 0;
					return true;
				} else if ("reset".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Reader)toTargetInstance(frame.objectArguments[0])).reset();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Reader)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else if ("markSupported".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = ((Reader)toTargetInstance(frame.objectArguments[0])).markSupported() ? 1 : 0;
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			} else if ("UnsupportedEncodingException".equals(className)) {
				return handleInstanceMethod(frame, "java/io/IOException", methodName, methodDescriptor);
			} else if ("UTFDataFormatException".equals(className)) {
				return handleInstanceMethod(frame, "java/io/IOException", methodName, methodDescriptor);
			} else if ("Writer".equals(className)) {
				if ("write".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).write(frame.intArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([C)V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).write((char[])frame.objectArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "([CII)V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).write((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("write".equals(methodName) && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).write((String)frame.objectArguments[1]);
					return true;
				} else if ("write".equals(methodName) && "(Ljava/lang/String;II)V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).write((String)frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]);
					return true;
				} else if ("flush".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).flush();
					return true;
				} else if ("close".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Writer)toTargetInstance(frame.objectArguments[0])).close();
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			}
		}
		// }
		// replace existing classes to add special code
		if ("java/lang".equals(packageName)) {
			if ("Object".equals(className)) {
				if ("wait".equals(methodName) && "(J)V".equals(methodDescriptor)) {
					long timeout = getLong(frame.intArguments, 1);
					waitForNotification(frame, timeout, 0);
					return true;
				} else if ("wait".equals(methodName) && "(JI)V".equals(methodDescriptor)) {
					long timeout = getLong(frame.intArguments, 1);
					int nanos = frame.intArguments[3];
					waitForNotification(frame, timeout, nanos);
					return true;
				} else if ("wait".equals(methodName) && "()V".equals(methodDescriptor)) {
					waitForNotification(frame, 0, 0);
					return true;
				} else if ("notify".equals(methodName) && "()V".equals(methodDescriptor)) {
					notifyToThreads(frame, false);
					return true;
				} else if ("notifyAll".equals(methodName) && "()V".equals(methodDescriptor)) {
					notifyToThreads(frame, true);
					return true;
				} else if ("equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodDescriptor)) {
					frame.singleReturn = Utils.toInt(toTargetInstance(frame.objectArguments[0]).equals(frame.objectArguments[1]));
					return true;
				} else if ("getClass".equals(methodName) && "()Ljava/lang/Class;".equals(methodDescriptor)) {
					frame.objectReturn = toTargetInstance(frame.objectArguments[0]).getClass();
					return true;
				} else if ("hashCode".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = toTargetInstance(frame.objectArguments[0]).hashCode();
					return true;
				} else if ("toString".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = toTargetInstance(frame.objectArguments[0]).toString();
					return true;
				}
			} else if ("Thread".equals(className)) {
				if ("start".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Thread)frame.objectArguments[0]).start();
					return true;
				} else if ("interrupt".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Thread)frame.objectArguments[0]).interrupt();
					return true;
				} else if ("isAlive".equals(methodName) && "()Z".equals(methodDescriptor)) {
					frame.singleReturn = Utils.toInt(((Thread)frame.objectArguments[0]).isAlive());
					return true;
				} else if ("setPriority".equals(methodName) && "(I)V".equals(methodDescriptor)) {
					((Thread)frame.objectArguments[0]).setPriority(frame.intArguments[1]);
					return true;
				} else if ("getPriority".equals(methodName) && "()I".equals(methodDescriptor)) {
					frame.singleReturn = ((Thread)frame.objectArguments[0]).getPriority();
					return true;
				} else if ("getName".equals(methodName) && "()Ljava/lang/String;".equals(methodDescriptor)) {
					frame.objectReturn = ((Thread)frame.objectArguments[0]).getName();
					return true;
				} else if ("join".equals(methodName) && "()V".equals(methodDescriptor)) {
					((Thread)frame.objectArguments[0]).join(frame.thread);
					return true;
				} else {
					return handleInstanceMethod(frame, "java/lang/Object", methodName, methodDescriptor);
				}
			}
		}
		return false;
	}

	protected final Object toTargetInstance(final Object object) {
		if (object instanceof Instance) {
			return ((Instance)object).parentInstance;
		} else {
			return object;
		}
	}

	private void waitForNotification(final Frame frame, long timeout, int nanos) {
		Object instance = frame.objectArguments[0];

		Thread thread = frame.thread;
		if (!thread.hasLock(instance)) {
			throw new IllegalMonitorStateException();
		}
		if (timeout < 0 || nanos < 0 || 999999 < nanos) {
			throw new IllegalArgumentException();
		}

		thread.releaseLock(instance);
		thread.status = Thread.STATUS_WAIT_FOR_NOTIFICATION;
		if (timeout != 0 && nanos != 0) {
			if (500000 <= nanos) {
				timeout += 1;
			}
			thread.wakeUpTime = System.currentTimeMillis() + timeout;
		} else {
			thread.wakeUpTime = 0;
		}
		thread.vm.addToWaitSet(instance, thread);
		throw new ChangeThreadException();
	}

	private void notifyToThreads(final Frame frame, final boolean toAllThreads) {
		Object instance = frame.objectArguments[0];
		if (!frame.thread.hasLock(instance)) {
			throw new IllegalMonitorStateException();
		}
		WaitSet waitSet = getWaitSet(instance);
		if (waitSet != null) {
			if (toAllThreads) {
				Thread current = waitSet.getFirstThreadAndRemove();
				while (current != null) {
					current.acquireLock(instance, false);
					current = waitSet.getFirstThreadAndRemove();
				}
			} else {
				waitSet.getFirstThreadAndRemove().acquireLock(instance, false);
			}
		}
	}

	protected boolean handleConstructor(final Frame frame, final String absoluteClassName, final String methodName, final String methodDescriptor) throws Exception {
		// CONSTRUCTOR SECTION {
		String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
		String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
		if ("java/util".equals(packageName)) {
			if ("Date".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Date());
					return true;
				} else if ("(J)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Date(getLong(frame.intArguments, 1)));
					return true;
				} else {
					return false;
				}
			} else if ("EmptyStackException".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new EmptyStackException());
				return true;
			} else if ("Hashtable".equals(className)) {
				if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Hashtable(frame.intArguments[1]));
					return true;
				} else if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Hashtable());
					return true;
				} else {
					return false;
				}
			} else if ("NoSuchElementException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NoSuchElementException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NoSuchElementException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Random".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Random());
					return true;
				} else if ("(J)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Random(getLong(frame.intArguments, 1)));
					return true;
				} else {
					return false;
				}
			} else if ("Stack".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Stack());
				return true;
			} else if ("Vector".equals(className)) {
				if ("(II)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Vector(frame.intArguments[1], frame.intArguments[2]));
					return true;
				} else if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Vector(frame.intArguments[1]));
					return true;
				} else if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Vector());
					return true;
				} else {
					return false;
				}
			}
		} else if ("java/lang/ref".equals(packageName)) {
			if ("WeakReference".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new WeakReference((Object)frame.objectArguments[1]));
				return true;
			}
		} else if ("java/lang".equals(packageName)) {
			if ("ArithmeticException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArithmeticException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArithmeticException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("ArrayIndexOutOfBoundsException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArrayIndexOutOfBoundsException());
					return true;
				} else if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArrayIndexOutOfBoundsException(frame.intArguments[1]));
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArrayIndexOutOfBoundsException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("ArrayStoreException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArrayStoreException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ArrayStoreException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Boolean".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Boolean(frame.intArguments[1] != 0));
				return true;
			} else if ("Byte".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Byte((byte)frame.intArguments[1]));
				return true;
			} else if ("Character".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Character((char)frame.intArguments[1]));
				return true;
			} else if ("ClassCastException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ClassCastException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ClassCastException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("ClassNotFoundException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ClassNotFoundException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ClassNotFoundException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Double".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Double(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1))));
				return true;
			} else if ("Error".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Error());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Error((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Exception".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Exception());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Exception((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Float".equals(className)) {
				if ("(F)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Float(Float.intBitsToFloat(frame.intArguments[1])));
					return true;
				} else if ("(D)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Float(Double.longBitsToDouble(Utils.getLong(frame.intArguments, 1))));
					return true;
				} else {
					return false;
				}
			} else if ("IllegalAccessException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalAccessException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalAccessException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("IllegalArgumentException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalArgumentException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalArgumentException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("IllegalMonitorStateException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalMonitorStateException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalMonitorStateException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("IllegalThreadStateException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalThreadStateException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IllegalThreadStateException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("IndexOutOfBoundsException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IndexOutOfBoundsException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IndexOutOfBoundsException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("InstantiationException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InstantiationException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InstantiationException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Integer".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Integer(frame.intArguments[1]));
				return true;
			} else if ("InterruptedException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InterruptedException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InterruptedException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Long".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Long(getLong(frame.intArguments, 1)));
				return true;
			} else if ("NegativeArraySizeException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NegativeArraySizeException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NegativeArraySizeException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("NoClassDefFoundError".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NoClassDefFoundError());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NoClassDefFoundError((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("NullPointerException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NullPointerException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NullPointerException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("NumberFormatException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NumberFormatException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new NumberFormatException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("OutOfMemoryError".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new OutOfMemoryError());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new OutOfMemoryError((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("RuntimeException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new RuntimeException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new RuntimeException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("SecurityException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new SecurityException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new SecurityException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Short".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new Short((short)frame.intArguments[1]));
				return true;
			} else if ("String".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((String)frame.objectArguments[1]));
					return true;
				} else if ("([C)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((char[])frame.objectArguments[1]));
					return true;
				} else if ("([CII)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((char[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]));
					return true;
				} else if ("([BIILjava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3], (String)frame.objectArguments[4]));
					return true;
				} else if ("([BLjava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((byte[])frame.objectArguments[1], (String)frame.objectArguments[2]));
					return true;
				} else if ("([BII)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]));
					return true;
				} else if ("([B)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((byte[])frame.objectArguments[1]));
					return true;
				} else if ("(Ljava/lang/StringBuffer;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new String((StringBuffer)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("StringBuffer".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringBuffer());
					return true;
				} else if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringBuffer(frame.intArguments[1]));
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringBuffer((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("StringIndexOutOfBoundsException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringIndexOutOfBoundsException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringIndexOutOfBoundsException((String)frame.objectArguments[1]));
					return true;
				} else if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new StringIndexOutOfBoundsException(frame.intArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("Throwable".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Throwable());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Throwable((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			}
		} else if ("java/io".equals(packageName)) {
			if ("ByteArrayInputStream".equals(className)) {
				if ("([B)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ByteArrayInputStream((byte[])frame.objectArguments[1]));
					return true;
				} else if ("([BII)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ByteArrayInputStream((byte[])frame.objectArguments[1], frame.intArguments[2], frame.intArguments[3]));
					return true;
				} else {
					return false;
				}
			} else if ("ByteArrayOutputStream".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ByteArrayOutputStream());
					return true;
				} else if ("(I)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new ByteArrayOutputStream(frame.intArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("DataInputStream".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new DataInputStream((InputStream)frame.objectArguments[1]));
				return true;
			} else if ("DataOutputStream".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new DataOutputStream((OutputStream)frame.objectArguments[1]));
				return true;
			} else if ("EOFException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new EOFException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new EOFException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("InputStreamReader".equals(className)) {
				if ("(Ljava/io/InputStream;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InputStreamReader((InputStream)frame.objectArguments[1]));
					return true;
				} else if ("(Ljava/io/InputStream;Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InputStreamReader((InputStream)frame.objectArguments[1], (String)frame.objectArguments[2]));
					return true;
				} else {
					return false;
				}
			} else if ("InterruptedIOException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InterruptedIOException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new InterruptedIOException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("IOException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IOException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new IOException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("OutputStreamWriter".equals(className)) {
				if ("(Ljava/io/OutputStream;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new OutputStreamWriter((OutputStream)frame.objectArguments[1]));
					return true;
				} else if ("(Ljava/io/OutputStream;Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new OutputStreamWriter((OutputStream)frame.objectArguments[1], (String)frame.objectArguments[2]));
					return true;
				} else {
					return false;
				}
			} else if ("PrintStream".equals(className) && "<init>".equals(methodName)) {
				replaceObjects(frame, frame.objectArguments[0], new PrintStream((OutputStream)frame.objectArguments[1]));
				return true;
			} else if ("UnsupportedEncodingException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new UnsupportedEncodingException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new UnsupportedEncodingException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			} else if ("UTFDataFormatException".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new UTFDataFormatException());
					return true;
				} else if ("(Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new UTFDataFormatException((String)frame.objectArguments[1]));
					return true;
				} else {
					return false;
				}
			}
		}
		// }
		// replace existing classes to add special code
		if ("java/lang".equals(packageName)) {
			if ("Object".equals(className)) {
				replaceObjects(frame, frame.objectArguments[0], new Object());
				return true;
			} else if ("Thread".equals(className)) {
				if ("()V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Thread(frame.thread.vm, "thread"));
					return true;
				} else if ("(Ljava/lang/Runnable;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Thread(frame.thread.vm, (Instance)frame.objectArguments[1]));
					return true;
				} else if ("(Ljava/lang/Runnable;Ljava/lang/String;)V".equals(methodDescriptor)) {
					replaceObjects(frame, frame.objectArguments[0], new Thread(frame.thread.vm, (Instance)frame.objectArguments[1], (String)frame.objectArguments[2]));
					return true;
				}
			}
		}
		throw new VirtualMachineException("not implemented");
		//		return false;
	}

	protected long getLong(final int[] ints, final int offset) {
		return Utils.getLong(ints, offset);
	}

	protected void setLong(final int[] ints, final int offset, final long value) {
		Utils.setLong(ints, offset, value);
	}

	protected void replaceObjects(final Frame frame, final Object previousObject, final Object newObject) {
		if (previousObject instanceof Instance) {
			((Instance)previousObject).parentInstance = newObject;
		} else {
			for (int i = 0, length = frame.registerCount; i < length; i++) {
				if (frame.objectRegisters[i] == previousObject) {
					frame.objectRegisters[i] = newObject;
				}
			}
			for (int i = 0, length = frame.argumentCount; i < length; i++) {
				if (frame.objectArguments[i] == previousObject) {
					frame.objectArguments[i] = newObject;
				}
			}
		}
	}

	private void addToWaitSet(final Object instance, final Thread thread) {
		WaitSet waitSet = getWaitSet(instance);
		if (waitSet != null) {
			waitSet.threads.addElement(thread);
			return;
		}

		waitSet = findEmptyWaitSet(instance);
		if (waitSet == null) {
			waitSet = new WaitSet();
			waitSets.addElement(waitSet);
		}
		waitSet.instance = instance;
		waitSet.threads.addElement(thread);
	}

	private WaitSet findEmptyWaitSet(final Object instance) {
		for (int i = 0, length = waitSets.size(); i < length; i++) {
			WaitSet waitSet = (WaitSet)waitSets.elementAt(i);
			if (waitSet.instance == null) {
				return waitSet;
			}
		}
		return null;
	}

	private WaitSet getWaitSet(final Object instance) {
		for (int i = 0, length = waitSets.size(); i < length; i++) {
			WaitSet waitSet = (WaitSet)waitSets.elementAt(i);
			if (waitSet.instance == instance) {
				return waitSet;
			}
		}
		return null;
	}

	// TODO Add types
	protected Class handleClassGetter(String type) throws ClassNotFoundException {
		if (type.startsWith("L")) {
			type = type.substring(1, type.length() - 1).replace('/', '.');
			return Class.forName(type);
		} else if (type.startsWith("[L")) {
			type = type.substring(2, type.length() - 1);
			String packageName = type.substring(0, type.lastIndexOf('/'));
			String className = type.substring(type.lastIndexOf('/') + 1);
			if ("java/lang".equals(packageName)) {
				if ("Object".equals(className)) {
					return Object[].class;
				} else if ("String".equals(className)) {
					return String[].class;
				}
			} else if ("java/util".endsWith(packageName)) {
				if ("Vector".equals(className)) {
					return Vector[].class;
				}
			}
			throw new ClassNotFoundException();
		} else {
			return Class.forName(type);
		}
	}
}
