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

import java.util.Vector;

final strictfp class Thread {
	private static final int INSTRUCTIONS_PER_PRIORITY = 20;

	// The upper STATUS_RUNNING status constants need to mean 'running'.
	static final int STATUS_NOT_STARTED = 0;
	static final int STATUS_END = 1;
	static final int STATUS_RUNNING = 2;
	static final int STATUS_JOIN = 3;
	static final int STATUS_SLEEP = 4;
	static final int STATUS_INTERRUPTED = 5;
	static final int STATUS_WAIT_FOR_MONITOR = 6;
	static final int STATUS_WAIT_FOR_NOTIFICATION = 7;

	final VirtualMachine vm;

	private final Vector frames = new Vector();
	private int currentFrame = -1;

	int status = STATUS_NOT_STARTED;
	long wakeUpTime = 0;
	Object monitorToResume;
	private final Vector monitorList = new Vector();

	private int priority = java.lang.Thread.NORM_PRIORITY;
	final String name;
	private Vector joinedThreads = new Vector();

	Thread(final VirtualMachine vm, final String name) {
		this.vm = vm;
		this.name = name;
	}

	Thread(final VirtualMachine vm, final Instance runnable) {
		this(vm, runnable, null);
	}

	Thread(final VirtualMachine vm, final Instance runnable, final String name) {
		this(vm, name);

		Frame frame = pushFrame();
		frame.init(runnable.clazz.getVirtualMethod("run", "()V"));
		frame.intArgument(0, runnable);
	}

	Frame pushFrame() {
		currentFrame++;
		if (frames.size() < currentFrame + 1) {
			frames.addElement(new Frame(this));
		}
		return (Frame)frames.elementAt(currentFrame);
	}

	Frame getCurrentFrame() {
		if (currentFrame < 0) {
			return null;
		}
		return (Frame)frames.elementAt(currentFrame);
	}

	private Frame popFrame() {
		return popFrameByThrowable(null);
	}

	private Frame popFrameByThrowable(final Throwable e) {
		Frame previousFrame = (Frame)frames.elementAt(currentFrame--);
		boolean isChangeThreadFrame = previousFrame.isChangeThreadFrame;
		previousFrame.destroy();
		if (isChangeThreadFrame) {
			throw new ChangeThreadException(e);
		} else if (currentFrame < 0) {
			status = STATUS_END;
			vm.threads.removeElement(this);
			for (int i = 0; i < joinedThreads.size(); i++) {
				Thread thread = (Thread)joinedThreads.elementAt(i);
				thread.status = STATUS_RUNNING;
			}
			joinedThreads.removeAllElements();
			throw new ChangeThreadException(e);
		} else {
			return ((Frame)frames.elementAt(currentFrame));
		}
	}

	strictfp void execute(final boolean endless) throws Throwable {
		int count = INSTRUCTIONS_PER_PRIORITY * priority;

		Frame frame = getCurrentFrame();
		Method method = frame.method;
		int[] lowerCodes = method.lowerCodes;
		int[] upperCodes = method.upperCodes;
		int[] codes = method.codes;

		while (endless || 0 < count--) {
			try {
				switch (lowerCodes[frame.pc]) {
					case 0x00: {
						// nop
						frame.pc++;
					}
					case 0x01: {
						// move vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int source = upperCodes[frame.pc++] >> 4;
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x02: {
						// move/from16 vAA, vBBBB
						int destination = upperCodes[frame.pc++];
						int source = codes[frame.pc++];
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x03: {
						// move/16 vAAAA, vBBBB
						frame.pc++;
						int destination = codes[frame.pc++];
						int source = codes[frame.pc++];
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x04: {
						// move-wide vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int source = upperCodes[frame.pc++] >> 4;
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.intRegisters[destination + 1] = frame.intRegisters[source + 1];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x05: {
						// move-wide/from16 vAA, vBBBB
						int destination = upperCodes[frame.pc++];
						int source = codes[frame.pc++];
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.intRegisters[destination + 1] = frame.intRegisters[source + 1];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x06: {
						// move-wide/16 vAAAA, vBBBB
						frame.pc++;
						int destination = codes[frame.pc++];
						int source = codes[frame.pc++];
						frame.intRegisters[destination] = frame.intRegisters[source];
						frame.intRegisters[destination + 1] = frame.intRegisters[source + 1];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x07: {
						// move-object vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int source = upperCodes[frame.pc++] >> 4;
						frame.objectRegisters[destination] = frame.objectRegisters[source];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x08: {
						// move-object/from16 vAA, vBBBB
						int destination = upperCodes[frame.pc++];
						int source = codes[frame.pc++];
						frame.objectRegisters[destination] = frame.objectRegisters[source];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x09: {
						// move-object/16 vAAAA, vBBBB
						frame.pc++;
						int destination = codes[frame.pc++];
						int source = codes[frame.pc++];
						frame.objectRegisters[destination] = frame.objectRegisters[source];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x0A: {
						//  move-result vAA
						int destination = upperCodes[frame.pc++];
						frame.intRegisters[destination] = frame.singleReturn;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x0B: {
						// move-result-wide vAA
						int destination = upperCodes[frame.pc++];
						Utils.setLong(frame.intRegisters, destination, frame.doubleReturn);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x0C: {
						// move-result-object vAA
						int destination = upperCodes[frame.pc++];
						frame.objectRegisters[destination] = frame.objectReturn;
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x0D: {
						// move-exception vAA
						int destination = upperCodes[frame.pc++];
						frame.objectRegisters[destination] = frame.throwableReturn;
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x0E: {
						// return-void
						frame = popFrame();

						method = frame.method;
						lowerCodes = method.lowerCodes;
						upperCodes = method.upperCodes;
						codes = method.codes;
						break;
					}
					case 0x0F: {
						// return vAA
						int result = frame.intRegisters[upperCodes[frame.pc++]];
						frame = popFrame();
						frame.singleReturn = result;

						method = frame.method;
						lowerCodes = method.lowerCodes;
						upperCodes = method.upperCodes;
						codes = method.codes;
						break;
					}
					case 0x10: {
						// return-wide vAA
						long result = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						frame = popFrame();
						frame.doubleReturn = result;

						method = frame.method;
						lowerCodes = method.lowerCodes;
						upperCodes = method.upperCodes;
						codes = method.codes;
						break;
					}
					case 0x11: {
						// return-object vAA
						Object result = frame.objectRegisters[upperCodes[frame.pc++]];
						frame = popFrame();
						frame.objectReturn = result;

						method = frame.method;
						lowerCodes = method.lowerCodes;
						upperCodes = method.upperCodes;
						codes = method.codes;
						break;
					}
					case 0x12: {
						// const/4 vA, #+B
						int data = upperCodes[frame.pc++];
						int destination = data & 0xF;
						int value = (data << 24) >> 28;
						frame.intRegisters[destination] = value;
						if (value == 0) {
							frame.objectRegisters[destination] = null;
						}
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x13: {
						// const/16 vAA, #+BBBB
						int destination = upperCodes[frame.pc++];
						int value = codes[frame.pc++];
						frame.intRegisters[destination] = (short)value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x14: {
						// const vAA, #+BBBBBBBB
						int destination = upperCodes[frame.pc++];
						int value = codes[frame.pc++];
						value |= codes[frame.pc++] << 16;
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x15: {
						// const/high16 vAA, #+BBBB0000
						int destination = upperCodes[frame.pc++];
						int value = codes[frame.pc++] << 16;
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x16: {
						// const-wide/16 vAA, #+BBBB
						int destination = upperCodes[frame.pc++];
						long value = (short)codes[frame.pc++];
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x17: {
						// const-wide/32 vAA, #+BBBBBBBB
						int destination = upperCodes[frame.pc++];
						long value = codes[frame.pc++];
						value = (int)(value | codes[frame.pc++] << 16);
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x18: {
						// const-wide vAA, #+BBBBBBBBBBBBBBBB
						int destination = upperCodes[frame.pc++];
						long value = codes[frame.pc++];
						value |= codes[frame.pc++] << 16;
						value |= codes[frame.pc++] << 32;
						value |= codes[frame.pc++] << 48;
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x19: {
						// const-wide/high16 vAA, #+BBBB000000000000
						int destination = upperCodes[frame.pc++];
						long value = (long)codes[frame.pc++] << 48;
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x1A: {
						// const-string vAA, string@BBBB
						int destination = upperCodes[frame.pc++];
						frame.objectRegisters[destination] = method.strings[codes[frame.pc++]];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x1B: {
						// const-string/jumbo vAA, string@BBBBBBBB
						int destination = upperCodes[frame.pc++];
						int source = codes[frame.pc++];
						source |= codes[frame.pc++] << 16;
						frame.objectRegisters[destination] = method.strings[source];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x1C: {
						//  const-class vAA, type@BBBB
						int destination = upperCodes[frame.pc++];
						Object value = vm.handleClassGetter(method.types[codes[frame.pc++]]);
						frame.objectRegisters[destination] = value;
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x1D: {
						// monitor-enter vAA
						Object instance = frame.objectRegisters[upperCodes[frame.pc++]];
						if (instance == null) {
							throw new NullPointerException();
						}
						frame.thread.acquireLock(instance, true);
						break;
					}
					case 0x1E: {
						// monitor-exit vAA
						Object instance = frame.objectRegisters[upperCodes[frame.pc++]];
						if (instance == null) {
							throw new NullPointerException();
						}
						frame.thread.releaseLock(instance);
						break;
					}
					case 0x1F: {
						// check-cast vAA, type@BBBB
						Object checked = frame.objectRegisters[upperCodes[frame.pc++]];
						String type = method.types[codes[frame.pc++]];
						if (checked != null && !isInstance(checked, type)) {
							throw new ClassCastException();
						}
						break;
					}
					case 0x20: {
						// instance-of vA, vB, type@CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						Object object = frame.objectRegisters[upperCodes[frame.pc++] >> 4];
						String type = method.types[codes[frame.pc++]];
						frame.intRegisters[destination] = Utils.toInt(isInstance(object, type));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x21: {
						// array-length vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						Object array = frame.objectRegisters[upperCodes[frame.pc++] >> 4];
						int value;
						if (array instanceof boolean[]) {
							value = ((boolean[])array).length;
						} else if (array instanceof byte[]) {
							value = ((byte[])array).length;
						} else if (array instanceof short[]) {
							value = ((short[])array).length;
						} else if (array instanceof int[]) {
							value = ((int[])array).length;
						} else if (array instanceof long[]) {
							value = ((long[])array).length;
						} else {
							value = ((Object[])array).length;
						}
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x22: {
						// new-instance vAA, type@BBBB
						int destination = upperCodes[frame.pc++];
						String type = method.types[codes[frame.pc++]];
						String className = type.substring(1, type.length() - 1);
						Clazz clazz = vm.systemClassLoader.loadClass(className);
						if (clazz != null) {
							frame.objectRegisters[destination] = new Instance(clazz);
						} else {
							frame.objectRegisters[destination] = new String(className); // This instance will be replaced when executing invokespecial
						}
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x23: {
						// new-array vA, vB, type@CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int size = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						String type = method.types[codes[frame.pc++]];

						frame.objectRegisters[destination] = handleNewArray(type, 1, size, -1, -1);
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x24: {
						// filled-new-array {vD, vE, vF, vG, vA}, type@CCCC
						int elements = upperCodes[frame.pc++] << 16;
						String type = method.types[codes[frame.pc++]];
						elements |= codes[frame.pc++];

						if ("[I".equals(type)) {
							int[] value = new int[elements >> 20];
							for (int i = 0, length = value.length; i < length; i++) {
								value[i] = frame.intRegisters[(elements >> (i * 4)) & 0xF];
							}
							frame.objectReturn = value;
						} else {
							throw new VirtualMachineException("not supported array type: " + type);
						}
						break;
					}
					case 0x25: {
						// filled-new-array/range {vCCCC .. vNNNN}, type@BBBB
						int size = upperCodes[frame.pc++] << 16;
						String type = method.types[codes[frame.pc++]];
						int firstRegister = codes[frame.pc++];

						if ("[I".equals(type)) {
							int[] array = new int[size];
							for (int i = 0, length = array.length; i < length; i++) {
								array[i] = frame.intRegisters[firstRegister + i];
							}
							frame.objectReturn = array;
						} else {
							throw new VirtualMachineException("not supported array type: " + type);
						}
						break;
					}
					case 0x26: {
						// fill-array-data vAA, +BBBBBBBB
						Object array = frame.objectRegisters[upperCodes[frame.pc++]];
						int offset = codes[frame.pc++];
						offset |= codes[frame.pc++] << 16;
						int address = frame.pc + offset - 3;
						if (codes[address] != 0x0300) {
							throw new RuntimeException("illegal array data header");
						}
						if (array instanceof int[]) {
							int[] intArray = (int[])array;
							if (codes[address + 1] != 4) {
								throw new RuntimeException("illegal array element size");
							}
							int elementCount = (codes[address + 3] << 16) | codes[address + 2];
							for (int i = 0; i < elementCount; i++) {
								int elementAddress = address + 4 + i * 2;
								intArray[i] = (codes[elementAddress + 1] << 16) | codes[elementAddress];
							}
						} else {
							throw new RuntimeException("not supported array type: " + array.getClass().getName());
						}
						break;
					}
					case 0x27: {
						// throw vAA
						Throwable throwable = (Throwable)frame.objectRegisters[upperCodes[frame.pc++]];
						if (throwable == null) {
							throw new NullPointerException();
						}
						throw throwable;
					}
					case 0x28: {
						// goto +AA
						int offset = (byte)upperCodes[frame.pc++];
						frame.pc += offset - 1;
						break;
					}
					case 0x29: {
						// goto/16 +AAAA
						frame.pc++;
						int offset = (short)codes[frame.pc++];
						frame.pc += offset - 2;
						break;
					}
					case 0x2A: {
						// goto/32 +AAAAAAAA
						frame.pc++;
						int offset = codes[frame.pc++];
						offset |= codes[frame.pc++] << 16;
						frame.pc += offset - 3;
						break;
					}
					case 0x2B: {
						// packed-switch vAA, +BBBBBBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = codes[frame.pc++];
						offset |= codes[frame.pc++] << 16;

						int address = frame.pc - 3 + offset;
						// skip ident
						address += 1;
						int size = codes[address++];
						int firstValue = codes[address] | (codes[address + 1] << 16);
						address += 2;

						if (firstValue <= comparedValue && comparedValue < firstValue + size) {
							int index = (comparedValue - firstValue) * 2;
							frame.pc += -3 + (codes[address + index] | (codes[address + index + 1] << 16));
						}
						break;
					}
					case 0x2C: {
						// sparse-switch vAA, +BBBBBBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = codes[frame.pc++];
						offset |= codes[frame.pc++] << 16;

						int address = frame.pc - 3 + offset;
						// skip ident
						address += 1;
						int size = codes[address++];
						for (int i = 0; i < size; i++) {
							int value = codes[address] | (codes[address + 1] << 16);
							if (value == comparedValue) {
								address += size * 2;
								frame.pc += -3 + (codes[address] | (codes[address + 1] << 16));
								break;
							}
							address += 2;
						}
						break;
					}
					case 0x2D: {
						// cmpl-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						if (Float.isNaN(firstValue) || Float.isNaN(secondValue)) {
							frame.intRegisters[destination] = -1;
						} else if (firstValue == secondValue) {
							frame.intRegisters[destination] = 0;
						} else if (firstValue < secondValue) {
							frame.intRegisters[destination] = -1;
						} else {
							frame.intRegisters[destination] = 1;
						}
						break;
					}
					case 0x2E: {
						// cmpg-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						if (Float.isNaN(firstValue) || Float.isNaN(secondValue)) {
							frame.intRegisters[destination] = 1;
						} else if (firstValue == secondValue) {
							frame.intRegisters[destination] = 0;
						} else if (firstValue < secondValue) {
							frame.intRegisters[destination] = -1;
						} else {
							frame.intRegisters[destination] = 1;
						}
						break;
					}
					case 0x2F: {
						// cmpl-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						if (Double.isNaN(firstValue) || Double.isNaN(secondValue)) {
							frame.intRegisters[destination] = -1;
						} else if (firstValue == secondValue) {
							frame.intRegisters[destination] = 0;
						} else if (firstValue < secondValue) {
							frame.intRegisters[destination] = -1;
						} else {
							frame.intRegisters[destination] = 1;
						}
						break;
					}
					case 0x30: {
						// cmpg-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						if (Double.isNaN(firstValue) || Double.isNaN(secondValue)) {
							frame.intRegisters[destination] = 1;
						} else if (firstValue == secondValue) {
							frame.intRegisters[destination] = 0;
						} else if (firstValue < secondValue) {
							frame.intRegisters[destination] = -1;
						} else {
							frame.intRegisters[destination] = 1;
						}
						break;
					}
					case 0x31: {
						// cmp-long vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						if (firstValue < secondValue) {
							frame.intRegisters[destination] = -1;
						} else if (firstValue == secondValue) {
							frame.intRegisters[destination] = 0;
						} else {
							frame.intRegisters[destination] = 1;
						}
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x32: {
						// if-eq vA, vB, +CCCC
						int firstRegister = upperCodes[frame.pc] & 0xF;
						int secondRegister = upperCodes[frame.pc++] >> 4;
						int offset = (short)codes[frame.pc++];
						boolean result;
						if (frame.isObjectRegister[firstRegister]) {
							if (frame.isObjectRegister[secondRegister]) {
								result = frame.objectRegisters[firstRegister] == frame.objectRegisters[secondRegister];
							} else {
								result = frame.objectRegisters[firstRegister] == toObject(frame.intRegisters[secondRegister]);
							}
						} else {
							if (frame.isObjectRegister[secondRegister]) {
								result = toObject(frame.intRegisters[firstRegister]) == frame.objectRegisters[secondRegister];
							} else {
								result = frame.intRegisters[firstRegister] == frame.intRegisters[secondRegister];
							}
						}
						if (result) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x33: {
						// if-ne vA, vB, +CCCC
						int firstRegister = upperCodes[frame.pc] & 0xF;
						int secondRegister = upperCodes[frame.pc++] >> 4;
						int offset = (short)codes[frame.pc++];
						boolean result;
						if (frame.isObjectRegister[firstRegister]) {
							if (frame.isObjectRegister[secondRegister]) {
								result = frame.objectRegisters[firstRegister] != frame.objectRegisters[secondRegister];
							} else {
								result = frame.objectRegisters[firstRegister] != toObject(frame.intRegisters[secondRegister]);
							}
						} else {
							if (frame.isObjectRegister[secondRegister]) {
								result = toObject(frame.intRegisters[firstRegister]) != frame.objectRegisters[secondRegister];
							} else {
								result = frame.intRegisters[firstRegister] != frame.intRegisters[secondRegister];
							}
						}
						if (result) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x34: {
						// if-lt vA, vB, +CCCC
						int firstValue = frame.intRegisters[upperCodes[frame.pc] & 0xF];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int offset = (short)codes[frame.pc++];
						if (firstValue < secondValue) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x35: {
						// if-ge vA, vB, +CCCC
						int firstValue = frame.intRegisters[upperCodes[frame.pc] & 0xF];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int offset = (short)codes[frame.pc++];
						if (firstValue >= secondValue) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x36: {
						// if-gt vA, vB, +CCCC
						int firstValue = frame.intRegisters[upperCodes[frame.pc] & 0xF];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int offset = (short)codes[frame.pc++];
						if (firstValue > secondValue) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x37: {
						// if-le vA, vB, +CCCC
						int firstValue = frame.intRegisters[upperCodes[frame.pc] & 0xF];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int offset = (short)codes[frame.pc++];
						if (firstValue <= secondValue) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x38: {
						// if-eqz vAA, +BBBB
						int comparedRegister = upperCodes[frame.pc++];
						int offset = (short)codes[frame.pc++];
						boolean result;
						if (frame.isObjectRegister[comparedRegister]) {
							result = frame.objectRegisters[comparedRegister] == null;
						} else {
							result = frame.intRegisters[comparedRegister] == 0;
						}
						if (result) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x39: {
						// if-nez vAA, +BBBB
						int comparedRegister = upperCodes[frame.pc++];
						int offset = (short)codes[frame.pc++];
						boolean result;
						if (frame.isObjectRegister[comparedRegister]) {
							result = frame.objectRegisters[comparedRegister] != null;
						} else {
							result = frame.intRegisters[comparedRegister] != 0;
						}
						if (result) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x3A: {
						// if-ltz vAA, +BBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = (short)codes[frame.pc++];
						if (comparedValue < 0) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x3B: {
						// if-gez vAA, +BBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = (short)codes[frame.pc++];
						if (comparedValue >= 0) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x3C: {
						// if-gtz vAA, +BBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = (short)codes[frame.pc++];
						if (comparedValue > 0) {
							frame.pc += offset - 2;
						}
						break;
					}
					case 0x3D: {
						// if-lez vAA, +BBBB
						int comparedValue = frame.intRegisters[upperCodes[frame.pc++]];
						int offset = (short)codes[frame.pc++];
						if (comparedValue <= 0) {
							frame.pc += offset - 2;
						}
						break;
					}
						// 0x3E ... 0x43 (unused)
					case 0x44: {
						// aget vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						Object array = frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						if (array == null) {
							throw new NullPointerException();
						} else if (array instanceof int[]) {
							int[] intArray = (int[])array;
							frame.intRegisters[destination] = intArray[index];
						} else if (array instanceof float[]) {
							float[] floatArray = (float[])array;
							frame.intRegisters[destination] = Float.floatToIntBits(floatArray[index]);
						} else {
							throw new VirtualMachineException("not supported type:" + array.getClass());
						}
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x45: {
						// aget-wide vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						Object array = frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						if (array == null) {
							throw new NullPointerException();
						} else if (array instanceof long[]) {
							long[] longArray = (long[])array;
							Utils.setLong(frame.intRegisters, destination, longArray[index]);
						} else if (array instanceof double[]) {
							double[] doubleArray = (double[])array;
							Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(doubleArray[index]));
						} else {
							throw new VirtualMachineException("not supported type:" + array.getClass());
						}
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x46: {
						// aget-object vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						Object[] array = (Object[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						frame.objectRegisters[destination] = array[index];
						frame.isObjectRegister[destination] = true;
						break;
					}
					case 0x47: {
						// aget-boolean vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						boolean[] array = (boolean[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = array[index] ? 1 : 0;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x48: {
						// aget-byte vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						byte[] array = (byte[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = array[index];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x49: {
						// aget-char vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						char[] array = (char[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = array[index];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x4A: {
						// aget-short vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						short[] array = (short[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = array[index];
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x4B: {
						// aput vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						Object array = frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						if (array == null) {
							throw new NullPointerException();
						} else if (array instanceof int[]) {
							((int[])array)[index] = frame.intRegisters[source];
						} else if (array instanceof float[]) {
							((float[])array)[index] = Float.intBitsToFloat(frame.intRegisters[source]);
						} else {
							throw new VirtualMachineException("not supported type:" + array.getClass());
						}
						break;
					}
					case 0x4C: {
						// aput-wide vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						Object array = frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						if (array == null) {
							throw new NullPointerException();
						} else if (array instanceof long[]) {
							((long[])array)[index] = Utils.getLong(frame.intRegisters, source);
						} else if (array instanceof double[]) {
							((double[])array)[index] = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, source));
						} else {
							throw new VirtualMachineException("not supported type:" + array.getClass());
						}
						break;
					}
					case 0x4D: {
						// aput-object vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						Object[] array = (Object[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						array[index] = (Object)frame.objectRegisters[source];
						break;
					}
					case 0x4E: {
						// aput-boolean vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						boolean[] array = (boolean[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						array[index] = frame.intRegisters[source] != 0;
						break;
					}
					case 0x4F: {
						// aput-byte vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						byte[] array = (byte[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						array[index] = (byte)frame.intRegisters[source];
						break;
					}
					case 0x50: {
						// aput-char vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						char[] array = (char[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						array[index] = (char)frame.intRegisters[source];
						break;
					}
					case 0x51: {
						// aput-short vAA, vBB, vCC
						int source = upperCodes[frame.pc++];
						short[] array = (short[])frame.objectRegisters[lowerCodes[frame.pc]];
						int index = frame.intRegisters[upperCodes[frame.pc++]];
						array[index] = (short)frame.intRegisters[source];
						break;
					}
					case 0x52:
						// iget vA, vB, field@CCCC
					case 0x53:
						// iget-wide vA, vB, field@CCCC
					case 0x54:
						// iget-object vA, vB, field@CCCC
					case 0x55:
						// iget-boolean vA, vB, field@CCCC
					case 0x56:
						// iget-byte vA, vB, field@CCCC
					case 0x57:
						// iget-char vA, vB, field@CCCC
					case 0x58: {
						// iget-short vA, vB, field@CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int source = upperCodes[frame.pc++] >> 4;
						int fieldIndex = codes[frame.pc++];
						getField(false, frame, source, fieldIndex, destination);
						break;
					}
					case 0x59:
						// iput vA, vB, field@CCCC
					case 0x5A:
						// iput-wide vA, vB, field@CCCC
					case 0x5B:
						// iput-object vA, vB, field@CCCC
					case 0x5C:
						// iput-boolean vA, vB, field@CCCC
					case 0x5D:
						// iput-byte vA, vB, field@CCCC
					case 0x5E:
						// iput-char vA, vB, field@CCCC
					case 0x5F: {
						// iput-short vA, vB, field@CCCC
						int source = upperCodes[frame.pc] & 0xF;
						int destination = upperCodes[frame.pc++] >> 4;
						int fieldIndex = codes[frame.pc++];
						setField(false, frame, source, destination, fieldIndex);
						break;
					}
					case 0x60:
						// sget
					case 0x61:
						// sget-wide
					case 0x62:
						// sget-object
					case 0x63:
						// sget-boolean
					case 0x64:
						// sget-byte
					case 0x65:
						// sget-boolean
					case 0x66: {
						// sget-short
						int destination = upperCodes[frame.pc++];
						int fieldIndex = codes[frame.pc++];
						getField(true, frame, 0, fieldIndex, destination);
						break;
					}
					case 0x67:
						// sput
					case 0x68:
						// sput-wide
					case 0x69:
						// sput-object
					case 0x6A:
						// sput-boolean
					case 0x6B:
						// sput-byte
					case 0x6C:
						// sput-char
					case 0x6D: {
						// sput-short
						int source = upperCodes[frame.pc++];
						int fieldIndex = codes[frame.pc++];
						setField(true, frame, source, 0, fieldIndex);
						break;
					}
					case 0x6E: {
						// invoke-virtual {vD, vE, vF, vG, vA}, meth@CCCC
						int registers = upperCodes[frame.pc++] << 16;
						int methodIndex = codes[frame.pc++];
						registers |= codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, registers);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						if (object instanceof Instance) {
							Instance instance = (Instance)object;
							Method target = instance.clazz.getVirtualMethod(methodName, methodDescriptor);
							if (target != null) {
								frame = callMethod(true, target, frame);

								method = frame.method;
								lowerCodes = method.lowerCodes;
								upperCodes = method.upperCodes;
								codes = method.codes;
								break;
							} else if (clazzName.equals(instance.clazz.name)) {
								clazzName = instance.clazz.superClass;
							}
						}
						if (!vm.handleInstanceMethod(frame, clazzName, methodName, methodDescriptor)) {
							throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
						}
						break;
					}
					case 0x6F:
						// invoke-super {vD, vE, vF, vG, vA}, meth@CCCC
						// fall through
					case 0x70: {
						// invoke-direct {vD, vE, vF, vG, vA}, meth@CCCC
						int registers = upperCodes[frame.pc++] << 16;
						int methodIndex = codes[frame.pc++];
						registers |= codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, registers);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						Clazz clazz = vm.systemClassLoader.loadClass(clazzName);
						if (clazz != null) {
							frame = callMethod(false, clazz.getDirectMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (methodName.equals("<init>")) {
								if (!vm.handleConstructor(frame, clazzName, methodName, methodDescriptor)) {
									throw new VirtualMachineException("not implemented constructor = " + clazzName + " - " + methodDescriptor);
								}
							} else {
								if (!vm.handleInstanceMethod(frame, clazzName, methodName, methodDescriptor)) {
									throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
								}
							}
						}
						break;
					}
					case 0x71: {
						// invoke-static {vD, vE, vF, vG, vA}, meth@CCCC
						int registers = upperCodes[frame.pc++] << 16;
						int methodIndex = codes[frame.pc++];
						registers |= codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(false, frame, methodDescriptor, registers);

						Clazz clazz = vm.systemClassLoader.loadClass(clazzName);
						if (clazz != null) {
							frame = callMethod(false, clazz.getDirectMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (!vm.handleClassMethod(frame, clazzName, methodName, methodDescriptor)) {
								throw new VirtualMachineException("not implemented class method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
							}
						}
						break;
					}
					case 0x72: {
						// invoke-interface {vD, vE, vF, vG, vA}, meth@CCCC
						int registers = upperCodes[frame.pc++] << 16;
						int methodIndex = codes[frame.pc++];
						registers |= codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, registers);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						if (object instanceof Instance) {
							Clazz clazz = ((Instance)object).clazz;
							frame = callMethod(false, clazz.getVirtualMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (!vm.handleInterfaceMethod(frame, clazzName, methodName, methodDescriptor)) {
								throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
							}
						}
						break;
					}
						// 0x73 (unused)
					case 0x74: {
						// invoke-virtual/range {vCCCC .. vNNNN}, meth@BBBB
						int range = upperCodes[frame.pc++];
						int methodIndex = codes[frame.pc++];
						int firstRegister = codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, firstRegister, range);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						if (object instanceof Instance) {
							Instance instance = (Instance)object;
							Method target = instance.clazz.getVirtualMethod(methodName, methodDescriptor);
							if (target != null) {
								frame = callMethod(true, target, frame);

								method = frame.method;
								lowerCodes = method.lowerCodes;
								upperCodes = method.upperCodes;
								codes = method.codes;
								break;
							} else if (clazzName.equals(instance.clazz.name)) {
								clazzName = instance.clazz.superClass;
							}
						}
						if (!vm.handleInstanceMethod(frame, clazzName, methodName, methodDescriptor)) {
							throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
						}
						break;
					}
					case 0x75:
						// invoke-super/range {vCCCC .. vNNNN}, meth@BBBB
						// fall through
					case 0x76: {
						// invoke-direct/range {vCCCC .. vNNNN}, meth@BBBB
						int range = upperCodes[frame.pc++];
						int methodIndex = codes[frame.pc++];
						int firstRegister = codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, firstRegister, range);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						Clazz clazz = vm.systemClassLoader.loadClass(clazzName);
						if (clazz != null) {
							frame = callMethod(false, clazz.getDirectMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (methodName.equals("<init>")) {
								if (!vm.handleConstructor(frame, clazzName, methodName, methodDescriptor)) {
									throw new VirtualMachineException("not implemented constructor = " + clazzName + " - " + methodDescriptor);
								}
							} else {
								if (!vm.handleInstanceMethod(frame, clazzName, methodName, methodDescriptor)) {
									throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
								}
							}
						}
						break;
					}
					case 0x77: {
						// invoke-static/range {vCCCC .. vNNNN}, meth@BBBB
						int range = upperCodes[frame.pc++];
						int methodIndex = codes[frame.pc++];
						int firstRegister = codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(false, frame, methodDescriptor, firstRegister, range);

						Clazz clazz = vm.systemClassLoader.loadClass(clazzName);
						if (clazz != null) {
							frame = callMethod(false, clazz.getDirectMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (!vm.handleClassMethod(frame, clazzName, methodName, methodDescriptor)) {
								throw new VirtualMachineException("not implemented class method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
							}
						}
						break;
					}
					case 0x78: {
						// invoke-interface/range {vCCCC .. vNNNN}, meth@BBBB
						int range = upperCodes[frame.pc++];
						int methodIndex = codes[frame.pc++];
						int firstRegister = codes[frame.pc++];

						String clazzName = method.methodClasses[methodIndex];
						String methodName = method.methodNames[methodIndex];
						String methodDescriptor = method.methodTypes[methodIndex];

						setArguments(true, frame, methodDescriptor, firstRegister, range);

						Object object = frame.objectArguments[0];
						if (object == null) {
							throw new NullPointerException();
						}
						if (object instanceof Instance) {
							Clazz clazz = ((Instance)object).clazz;
							frame = callMethod(false, clazz.getVirtualMethod(methodName, methodDescriptor), frame);

							method = frame.method;
							lowerCodes = method.lowerCodes;
							upperCodes = method.upperCodes;
							codes = method.codes;
						} else {
							if (!vm.handleInterfaceMethod(frame, clazzName, methodName, methodDescriptor)) {
								throw new VirtualMachineException("not implemented instance method = " + clazzName + " - " + methodName + " - " + methodDescriptor);
							}
						}
						break;
					}
						// 0x79 ... 0x7A (unused)
					case 0x7B: {
						// neg-int vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int value = -frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x7C: {
						// not-int vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int value = ~frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x7D: {
						// neg-long vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long value = -Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x7E: {
						// not-long vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long value = ~Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x7F: {
						// neg-float vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float value = -Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x80: {
						// neg-double vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double value = -Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(value));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x81: {
						// int-to-long
						int destination = upperCodes[frame.pc] & 0xF;
						long value = (int)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x82: {
						// int-to-float vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float value = (float)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = Float.floatToIntBits(value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x83: {
						// int-to-double vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double value = (double)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(value));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x84: {
						// long-to-int
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (int)Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x85: {
						// long-to-float
						int destination = upperCodes[frame.pc] & 0xF;
						float value = (float)Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						frame.intRegisters[destination] = Float.floatToIntBits(value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x86: {
						// long-to-double
						int destination = upperCodes[frame.pc] & 0xF;
						double value = (double)Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(value));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x87: {
						// float-to-int vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (int)Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x88: {
						// float-to-long vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long value = (long)Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x89: {
						// float-to-double vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double value = (double)Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(value));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8A: {
						// double-to-int vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (int)Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8B: {
						// double-to-long vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long value = (long)Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8C: {
						// double-to-float vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float value = (float)Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						frame.intRegisters[destination] = Float.floatToIntBits(value);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8D: {
						// int-to-byte
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (byte)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8E: {
						// int-to-char
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (char)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x8F: {
						// int-to-short
						int destination = upperCodes[frame.pc] & 0xF;
						int value = (short)frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = value;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x90: {
						// add-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue + secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x91: {
						// sub-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue - secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x92: {
						// mul-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue * secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x93: {
						// div-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue / secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x94: {
						// rem-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue % secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x95: {
						// and-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue & secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x96: {
						// or-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue | secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x97: {
						// xor-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue ^ secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x98: {
						// shl-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue << secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x99: {
						// shr-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue >> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9A: {
						// ushr-int vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++]];
						frame.intRegisters[destination] = firstValue >>> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9B: {
						// add-long vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue + secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9C: {
						// sub-long vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue - secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9D: {
						// mul-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue * secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9E: {
						// div-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue / secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0x9F: {
						// rem-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue % secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA0: {
						// and-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue & secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA1: {
						// or-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue | secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA2: {
						// xor-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]);
						Utils.setLong(frame.intRegisters, destination, firstValue ^ secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA3: {
						// shl-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = frame.intRegisters[upperCodes[frame.pc++]] & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue << secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA4: {
						// shr-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = frame.intRegisters[upperCodes[frame.pc++]] & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue >> secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA5: {
						// ushr-long
						int destination = upperCodes[frame.pc++];
						long firstValue = Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]);
						long secondValue = frame.intRegisters[upperCodes[frame.pc++]] & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue >>> secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA6: {
						// add-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue + secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA7: {
						// sub-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue - secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA8: {
						// mul-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue * secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xA9: {
						// div-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue / secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAA: {
						// rem-float vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						float firstValue = Float.intBitsToFloat(frame.intRegisters[lowerCodes[frame.pc]]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++]]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue % secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAB: {
						// add-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue + secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAC: {
						// sub-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue - secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAD: {
						// mul-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue * secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAE: {
						// div-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue / secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xAF: {
						// rem-double vAA, vBB, vCC
						int destination = upperCodes[frame.pc++];
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, lowerCodes[frame.pc]));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++]));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue % secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB0: {
						// add-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue + secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB1: {
						// sub-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue - secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB2: {
						// mul-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue * secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB3: {
						// div-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue / secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB4: {
						// rem-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue % secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB5: {
						// and-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue & secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB6: {
						// or-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue | secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB7: {
						// xor-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue ^ secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB8: {
						// shl-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue << secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xB9: {
						// shr-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue >> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBA: {
						// ushr-int/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[destination];
						int secondValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						frame.intRegisters[destination] = firstValue >>> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBB: {
						// add-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue + secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBC: {
						// sub-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue - secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBD: {
						// mul-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue * secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBE: {
						// div-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue / secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xBF: {
						// rem-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue % secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC0: {
						// and-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue & secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC1: {
						// or-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue | secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC2: {
						// xor-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4);
						Utils.setLong(frame.intRegisters, destination, firstValue ^ secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC3: {
						// shl-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4) & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue << secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC4: {
						// shr-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4) & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue >> secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC5: {
						// ushr-long/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						long firstValue = Utils.getLong(frame.intRegisters, destination);
						long secondValue = Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4) & 0x3F;
						Utils.setLong(frame.intRegisters, destination, firstValue >>> secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC6: {
						// add-float/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float firstValue = Float.intBitsToFloat(frame.intRegisters[destination]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue + secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC7: {
						// sub-float/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float firstValue = Float.intBitsToFloat(frame.intRegisters[destination]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue - secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC8: {
						// mul-float/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float firstValue = Float.intBitsToFloat(frame.intRegisters[destination]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue * secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xC9: {
						// div-float/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float firstValue = Float.intBitsToFloat(frame.intRegisters[destination]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue / secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCA: {
						// rem-float/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						float firstValue = Float.intBitsToFloat(frame.intRegisters[destination]);
						float secondValue = Float.intBitsToFloat(frame.intRegisters[upperCodes[frame.pc++] >> 4]);
						frame.intRegisters[destination] = Float.floatToIntBits(firstValue % secondValue);
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCB: {
						// add-double/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, destination));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue + secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCC: {
						// sub-double/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, destination));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue - secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCD: {
						// mul-double/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, destination));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue * secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCE: {
						// div-double/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, destination));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue / secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xCF: {
						// rem-double/2addr vA, vB
						int destination = upperCodes[frame.pc] & 0xF;
						double firstValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, destination));
						double secondValue = Double.longBitsToDouble(Utils.getLong(frame.intRegisters, upperCodes[frame.pc++] >> 4));
						Utils.setLong(frame.intRegisters, destination, Double.doubleToLongBits(firstValue % secondValue));
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD0: {
						// add-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue + secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD1: {
						// rsub-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = secondValue - firstValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD2: {
						// mul-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue * secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD3: {
						// div-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue / secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD4: {
						// rem-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue % secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD5: {
						// and-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue & secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD6: {
						// or-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue | secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD7: {
						// xor-int/lit16 vA, vB, #+CCCC
						int destination = upperCodes[frame.pc] & 0xF;
						int firstValue = frame.intRegisters[upperCodes[frame.pc++] >> 4];
						int secondValue = (short)codes[frame.pc++];
						frame.intRegisters[destination] = firstValue ^ secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD8: {
						// add-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue + secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xD9: {
						// rsub-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = secondValue - firstValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDA: {
						// mul-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue * secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDB: {
						// div-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue / secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDC: {
						// rem-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue % secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDD: {
						// and-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue & secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDE: {
						// or-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue | secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xDF: {
						// xor-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue ^ secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xE0: {
						// shl-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue << secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xE1: {
						// shr-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue >> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
					case 0xE2: {
						// ushr-int/lit8 vAA, vBB, #+CC
						int destination = upperCodes[frame.pc++];
						int firstValue = frame.intRegisters[lowerCodes[frame.pc]];
						int secondValue = (byte)upperCodes[frame.pc++];
						frame.intRegisters[destination] = firstValue >>> secondValue;
						frame.isObjectRegister[destination] = false;
						break;
					}
						// 0xE3 ... 0xFF (unused)
					default:
						throw new RuntimeException("not implemented instruction: 0x" + Integer.toHexString(lowerCodes[frame.pc]));
				}
			} catch (Throwable e) {
				frame = handleThrowable(e, frame);

				method = frame.method;
				lowerCodes = method.lowerCodes;
				upperCodes = method.upperCodes;
				codes = method.codes;
			}
		}
	}

	private static final Object POINTER_OBJECT = new Object();

	private static Object toObject(final int value) {
		return value == 0 ? null : POINTER_OBJECT;
	}

	private void setField(final boolean isStatic, final Frame frame, final int source, final int destination, final int fieldIndex) {
		Method method = frame.method;
		String clazzName = method.fieldClasses[fieldIndex];
		String fieldName = method.fieldNames[fieldIndex];
		String fieldType = method.fieldTypes[fieldIndex];

		Field field = getField(isStatic, frame, clazzName, fieldName, destination);
		if (field != null) {
			switch (fieldType.charAt(0)) {
				case 'C':
				case 'B':
				case 'S':
				case 'I':
				case 'Z':
					field.intValue = frame.intRegisters[source];
					break;
				case 'J':
					field.longValue = Utils.getLong(frame.intRegisters, source);
					break;
				case 'L':
				case '[':
					field.objectValue = frame.objectRegisters[source];
					break;
				default:
					throw new VirtualMachineException("not supported field type");
			}
		} else {
			if (isStatic) {
				if (!vm.handleClassFieldSetter(frame, source, clazzName, fieldName, fieldType)) {
					throw new VirtualMachineException("not implemented class field = " + clazzName + " - " + fieldName + " - " + fieldType);
				}
			} else {
				throw new VirtualMachineException("not implemented instance field = " + clazzName + " - " + fieldName + " - " + fieldType);
			}
		}
	}

	private void getField(final boolean isStatic, final Frame frame, final int source, final int fieldIndex, final int destination) {
		Method method = frame.method;
		String clazzName = method.fieldClasses[fieldIndex];
		String fieldName = method.fieldNames[fieldIndex];
		String fieldType = method.fieldTypes[fieldIndex];

		Field field = getField(isStatic, frame, clazzName, fieldName, source);
		if (field != null) {
			switch (fieldType.charAt(0)) {
				case 'C':
				case 'B':
				case 'S':
				case 'I':
				case 'Z':
					frame.intRegisters[destination] = field.intValue;
					frame.isObjectRegister[destination] = false;
					break;
				case 'J':
					Utils.setLong(frame.intRegisters, destination, field.longValue);
					frame.isObjectRegister[destination] = false;
					break;
				case 'L':
				case '[':
					frame.objectRegisters[destination] = field.objectValue;
					frame.isObjectRegister[destination] = true;
					break;
				default:
					throw new VirtualMachineException("not supported field type");
			}
		} else {
			if (isStatic) {
				if (!vm.handleClassFieldGetter(frame, clazzName, fieldName, fieldType, destination)) {
					throw new VirtualMachineException("not implemented class field = " + clazzName + " - " + fieldName + " - " + fieldType);
				}
			} else {
				//				if (!vm.handleInstanceFieldGetter(frame, clazzName, fieldName, fieldType, register)) {
				throw new VirtualMachineException("not implemented instance field = " + clazzName + " - " + fieldName + " - " + fieldType);
				//				}
			}
		}
	}

	private boolean isInstance(final Object checked, final String type) throws ClassNotFoundException {
		if (checked == null) {
			return false;
		}
		String className = type.startsWith("L") ? type.substring(1, type.length() - 1) : type;
		Clazz vmClass = vm.systemClassLoader.loadClass(className);
		if (vmClass != null) {
			if (checked instanceof Instance) {
				Clazz instanceClazz = ((Instance)checked).clazz;
				while (vmClass != null) {
					if (instanceClazz == vmClass) {
						return true;
					}
					vmClass = vm.systemClassLoader.loadClass(vmClass.superClass);
				}
			}
			return false;
		} else {
			Class nativeClass = Class.forName(className.replace('/', '.'));
			if (checked instanceof Instance) {
				return nativeClass.isInstance(((Instance)checked).parentInstance);
			} else {
				return nativeClass.isInstance(checked);
			}
		}
	}

	private static void setArguments(boolean isVirtual, Frame frame, String descriptor, int firstRegister, int range) {
		int argumentPosition = 0;
		if (isVirtual) {
			frame.setArgument(0, frame.objectRegisters[firstRegister]);
			argumentPosition++;
		}
		for (int i = 1, length = descriptor.indexOf(')'); i < length; i++) {
			int register = firstRegister + argumentPosition;
			switch (descriptor.charAt(i)) {
				case 'C':
				case 'B':
				case 'S':
				case 'I':
				case 'Z':
					frame.setArgument(argumentPosition, frame.intRegisters[register]);
					argumentPosition++;
					break;
				case 'J':
					frame.setArgument(argumentPosition, Utils.getLong(frame.intRegisters, register));
					argumentPosition += 2;
					break;
				case 'F':
					// Copy as int because bits data is important
					frame.setArgument(argumentPosition, frame.intRegisters[register]);
					argumentPosition++;
					break;
				case 'D':
					// Copy as long because bits data is important
					frame.setArgument(argumentPosition, Utils.getLong(frame.intRegisters, register));
					argumentPosition += 2;
					break;
				case 'L': {
					frame.setArgument(argumentPosition, frame.objectRegisters[register]);
					argumentPosition++;
					i = descriptor.indexOf(';', i);
					break;
				}
				case '[': {
					int startIndex = i;
					while (i + 1 < length && descriptor.charAt(i + 1) == '[') {
						i++;
					}
					i++;
					switch (descriptor.charAt(i)) {
						case 'C':
						case 'B':
						case 'S':
						case 'I':
						case 'Z':
						case 'J':
						case 'F':
						case 'D':
							break;
						case 'L':
							i = descriptor.indexOf(';', i);
							break;
						default:
							throw new VirtualMachineException("not implemented type = " + descriptor.substring(startIndex, i + 1));
					}
					frame.setArgument(argumentPosition, frame.objectRegisters[register]);
					argumentPosition++;
					break;
				}
				default:
					throw new VirtualMachineException("not implemented type = " + descriptor.charAt(i));
			}
		}
	}

	private static void setArguments(final boolean isVirtual, final Frame frame, final String descriptor, final int registers) {
		int argumentPosition = 0;
		if (isVirtual) {
			frame.setArgument(0, frame.objectRegisters[registers & 0xF]);
			argumentPosition++;
		}
		for (int i = 1, length = descriptor.indexOf(')'); i < length; i++) {
			int register = (registers >> (argumentPosition * 4)) & 0xF;
			switch (descriptor.charAt(i)) {
				case 'C':
				case 'B':
				case 'S':
				case 'I':
				case 'Z':
					frame.setArgument(argumentPosition, frame.intRegisters[register]);
					argumentPosition++;
					break;
				case 'J':
					frame.setArgument(argumentPosition, Utils.getLong(frame.intRegisters, register));
					argumentPosition += 2;
					break;
				case 'F':
					// Copy as int because bits data is important
					frame.setArgument(argumentPosition, frame.intRegisters[register]);
					argumentPosition++;
					break;
				case 'D':
					// Copy as long because bits data is important
					frame.setArgument(argumentPosition, Utils.getLong(frame.intRegisters, register));
					argumentPosition += 2;
					break;
				case 'L': {
					frame.setArgument(argumentPosition, frame.objectRegisters[register]);
					argumentPosition++;
					i = descriptor.indexOf(';', i);
					break;
				}
				case '[': {
					int startIndex = i;
					while (i + 1 < length && descriptor.charAt(i + 1) == '[') {
						i++;
					}
					i++;
					switch (descriptor.charAt(i)) {
						case 'C':
						case 'B':
						case 'S':
						case 'I':
						case 'Z':
						case 'J':
						case 'F':
						case 'D':
							break;
						case 'L':
							i = descriptor.indexOf(';', i);
							break;
						default:
							throw new VirtualMachineException("not implemented type = " + descriptor.substring(startIndex, i + 1));
					}
					frame.setArgument(argumentPosition, frame.objectRegisters[register]);
					argumentPosition++;
					break;
				}
				default:
					throw new VirtualMachineException("not implemented type = " + descriptor.charAt(i));
			}
		}
	}

	private static Field getField(final boolean isStatic, final Frame frame, final String clazzName, final String fieldName, final int instance) {
		if (isStatic) {
			Clazz clazz = frame.method.clazz.classLoader.loadClass(clazzName);
			if (clazz != null) {
				return clazz.getStaticField(fieldName);
			} else {
				return null;
			}
		} else {
			Object object = frame.objectRegisters[instance];
			if (object instanceof Instance) {
				Field field = ((Instance)object).getField(clazzName, fieldName);
				if (field != null) {
					return field;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	private Frame callMethod(final boolean isVirtual, Method method, final Frame frame) {
		Frame newFrame = pushFrame();

		Object instance = null;
		if (method.isInstance) {
			instance = frame.objectRegisters[0];
			if (isVirtual && instance != null && instance instanceof Instance) {
				// Handle override method
				method = ((Instance)instance).clazz.getVirtualMethod(method.name, method.descriptor);
			}
		}
		newFrame.init(method);

		int argumentCount = method.incomingArgumentCount;
		int destPos = newFrame.registerCount - argumentCount;
		System.arraycopy(frame.intArguments, 0, newFrame.intRegisters, destPos, argumentCount);
		System.arraycopy(frame.objectArguments, 0, newFrame.objectRegisters, destPos, argumentCount);

		if (method.isSynchronized) {
			if (method.isInstance) {
				newFrame.monitor = instance;
			} else {
				newFrame.monitor = method.clazz;
			}
			acquireLock(newFrame.monitor, true);
		}

		return newFrame;
	}

	private Object handleNewArray(final String classDescriptor, final int lengthNumber, final int length1, final int length2, final int length3) {
		int dimension = 0;
		for (int i = 0; i < classDescriptor.length() && classDescriptor.charAt(i) == '['; i++) {
			dimension++;
		}
		switch (classDescriptor.charAt(dimension)) {
			case 'L':
				return vm.handleNewObjectArray(classDescriptor.substring(dimension + 1, classDescriptor.length() - 1), dimension, lengthNumber, length1, length2, length3);
			case 'B':
				switch (dimension) {
					case 1:
						return new byte[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new byte[length1][];
							case 2:
								return new byte[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new byte[length1][][];
							case 2:
								return new byte[length1][length2][];
							case 3:
								return new byte[length1][length2][length3];
						}
						break;
				}
				break;
			case 'C':
				switch (dimension) {
					case 1:
						return new char[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new char[length1][];
							case 2:
								return new char[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new char[length1][][];
							case 2:
								return new char[length1][length2][];
							case 3:
								return new char[length1][length2][length3];
						}
						break;
				}
				break;
			case 'I':
				switch (dimension) {
					case 1:
						return new int[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new int[length1][];
							case 2:
								return new int[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new int[length1][][];
							case 2:
								return new int[length1][length2][];
							case 3:
								return new int[length1][length2][length3];
						}
						break;
				}
				break;
			case 'J':
				switch (dimension) {
					case 1:
						return new long[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new long[length1][];
							case 2:
								return new long[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new long[length1][][];
							case 2:
								return new long[length1][length2][];
							case 3:
								return new long[length1][length2][length3];
						}
						break;
				}
				break;
			case 'F':
				switch (dimension) {
					case 1:
						return new float[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new float[length1][];
							case 2:
								return new float[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new float[length1][][];
							case 2:
								return new float[length1][length2][];
							case 3:
								return new float[length1][length2][length3];
						}
						break;
				}
				break;
			case 'D':
				switch (dimension) {
					case 1:
						return new double[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new double[length1][];
							case 2:
								return new double[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new double[length1][][];
							case 2:
								return new double[length1][length2][];
							case 3:
								return new double[length1][length2][length3];
						}
						break;
				}
				break;
			case 'S':
				switch (dimension) {
					case 1:
						return new short[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new short[length1][];
							case 2:
								return new short[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new short[length1][][];
							case 2:
								return new short[length1][length2][];
							case 3:
								return new short[length1][length2][length3];
						}
						break;
				}
				break;
			case 'Z':
				switch (dimension) {
					case 1:
						return new boolean[length1];
					case 2:
						switch (lengthNumber) {
							case 1:
								return new boolean[length1][];
							case 2:
								return new boolean[length1][length2];
						}
						break;
					case 3:
						switch (lengthNumber) {
							case 1:
								return new boolean[length1][][];
							case 2:
								return new boolean[length1][length2][];
							case 3:
								return new boolean[length1][length2][length3];
						}
						break;
				}
				break;
		}
		throw new VirtualMachineException("not supported array type = " + classDescriptor);
	}

	private Frame handleThrowable(final Throwable e, Frame frame) {
		if (e instanceof ChangeThreadException) {
			throw (ChangeThreadException)e;
		}
		// At the end, #popFrameByThrowable throws a ChangeThreadException exception
		while (true) {
			Method method = frame.method;
			if (method.exceptionStartAddresses != null) {
				int handlerIndex = -1;
				{
					int[] exceptionStartAddresses = method.exceptionStartAddresses;
					int[] exceptionEndAddresses = method.exceptionEndAdresses;
					for (int i = 0, length = exceptionStartAddresses.length; i < length; i++) {
						if (exceptionStartAddresses[i] < frame.pc && frame.pc <= exceptionEndAddresses[i]) {
							handlerIndex = method.exceptionHandlerIndexes[i];
							// Don't exit this loop to search the outer block
						}
					}
				}
				if (handlerIndex != -1) {
					String[] exceptionHandlerTypes = method.exceptionHandlerTypes[handlerIndex];
					int[] exceptionHandlerAddresses = method.exceptionHandlerAddresses[handlerIndex];
					for (int i = 0, length = exceptionHandlerTypes.length; i < length; i++) {
						if (vm.isSubClass(e, exceptionHandlerTypes[i])) {
							frame.throwableReturn = e;
							frame.pc = exceptionHandlerAddresses[i];
							return frame;
						}
					}
				}
			}
			frame = popFrameByThrowable(e);
		}
	}

	static Thread currentThread(final Frame frame) {
		return frame.thread;
	}

	static void yield() {
		throw new ChangeThreadException();
	}

	static void sleep(final Frame frame, final long millis) {
		Thread thread = frame.thread;
		thread.status = STATUS_SLEEP;
		thread.wakeUpTime = System.currentTimeMillis() + millis;
		throw new ChangeThreadException();
	}

	// end code is contained in #popFrame
	void start() {
		if (status != STATUS_NOT_STARTED) {
			throw new IllegalThreadStateException();
		}
		vm.threads.addElement(this);
		status = STATUS_RUNNING;
	}

	void interrupt() {
		switch (status) {
			case STATUS_SLEEP:
				wakeUpTime = 0;
				status = STATUS_INTERRUPTED;
				break;
			case STATUS_JOIN:
				status = STATUS_INTERRUPTED;
				break;
			case STATUS_WAIT_FOR_NOTIFICATION:
				wakeUpTime = 0;
				acquireLock(monitorToResume, false);
				status = STATUS_INTERRUPTED;
				break;
		}
	}

	void handleInterrupted() throws Throwable {
		// need to change the status here because #handleThrowable will throw a exception or not
		status = STATUS_RUNNING;
		handleThrowable(new InterruptedException(), getCurrentFrame());
	}

	boolean isAlive() {
		return STATUS_RUNNING <= status;
	}

	void setPriority(final int newPriority) {
		if (priority < java.lang.Thread.MIN_PRIORITY || java.lang.Thread.MAX_PRIORITY < priority) {
			throw new IllegalArgumentException();
		}
		priority = newPriority;
	}

	int getPriority() {
		return priority;
	}

	static int activeCount(final Frame frame) {
		return frame.thread.vm.threads.size();
	}

	String getName() {
		return name;
	}

	void join(final Thread caller) {
		caller.status = STATUS_JOIN;
		joinedThreads.addElement(caller);
		throw new ChangeThreadException();
	}

	public String toString() {
		return "Thread[" + getName() + "," + getPriority() + "]";
	}

	void acquireLock(final Object instance, final boolean changeThread) {
		if (isLocked(instance)) {
			status = STATUS_WAIT_FOR_MONITOR;
			monitorToResume = instance;
			if (changeThread) {
				throw new ChangeThreadException();
			}
		} else {
			lock(instance);
		}
	}

	private boolean isLocked(final Object instance) {
		for (int i = 0, length = vm.threads.size(); i < length; i++) {
			Thread thread = (Thread)vm.threads.elementAt(i);
			if (thread != this) {
				if (thread.monitorList.contains(instance)) {
					return true;
				}
			}
		}
		return false;
	}

	private void lock(final Object instance) {
		monitorList.addElement(instance);
	}

	void releaseLock(final Object instance) {
		monitorList.removeElementAt(monitorList.size() - 1);
		if (monitorList.contains(instance)) {
			return;
		} else {
			for (int i = 0, length = vm.threads.size(); i < length; i++) {
				Thread thread = (Thread)vm.threads.elementAt(i);
				if (thread != this && thread.status == STATUS_WAIT_FOR_MONITOR && thread.monitorToResume == instance) {
					thread.status = STATUS_RUNNING;
					thread.monitorToResume = null;
					thread.lock(instance);
					break;
				}
			}
		}
	}

	boolean hasLock(final Object instance) {
		return monitorList.contains(instance);
	}
}
