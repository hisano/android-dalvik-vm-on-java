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

import java.util.Hashtable;

final class ClassLoader {
	private static final int ACC_private = 0x1;
	private static final int ACC_PRIVATE = 0x2;
	private static final int ACC_PROTECTED = 0x4;
	private static final int ACC_STATIC = 0x8;
	private static final int ACC_FINAL = 0x10;
	private static final int ACC_SYNCHRONIZED = 0x20;
	private static final int ACC_VOLATILE = 0x40;
	private static final int ACC_BRIDGE = 0x40;
	private static final int ACC_TRANSIENT = 0x80;
	private static final int ACC_VARARGS = 0x80;
	private static final int ACC_NATIVE = 0x100;
	private static final int ACC_INTERFACE = 0x200;
	private static final int ACC_ABSTRACT = 0x400;
	private static final int ACC_STRICT = 0x800;
	private static final int ACC_SYNTHETIC = 0x1000;
	private static final int ACC_ANNOTATION = 0x2000;
	private static final int ACC_ENUM = 0x4000;
	private static final int ACC_UNUSED = 0x8000;
	private static final int ACC_CONSTRUCTOR = 0x10000;
	private static final int ACC_DECLARED_SYNCHRONIZED = 0x2000;

	private static final int VALUE_BYTE = 0x00;
	private static final int VALUE_SHORT = 0x02;
	private static final int VALUE_CHAR = 0x03;
	private static final int VALUE_INT = 0x04;
	private static final int VALUE_LONG = 0x06;
	private static final int VALUE_FLOAT = 0x10;
	private static final int VALUE_DOUBLE = 0x11;
	private static final int VALUE_STRING = 0x17;
	private static final int VALUE_TYPE = 0x18;
	private static final int VALUE_FIELD = 0x19;
	private static final int VALUE_METHOD = 0x1a;
	private static final int VALUE_ENUM = 0x1b;
	private static final int VALUE_ARRAY = 0x1c;
	private static final int VALUE_ANNOTATION = 0x1d;
	private static final int VALUE_NULL = 0x1e;
	private static final int VALUE_BOOLEAN = 0x1f;

	private final VirtualMachine vm;
	private final Thread loadThread;
	private final Hashtable classes = new Hashtable();

	ClassLoader(final VirtualMachine vm) {
		this.vm = vm;
		loadThread = new Thread(vm, "Class Loader");
	}

	Clazz loadClass(final String name) {
		Clazz clazz;
		if (classes.containsKey(name)) {
			clazz = (Clazz)classes.get(name);
		} else {
			clazz = findClass(name);
			if (clazz == null) {
				return null;
			}
			classes.put(name, clazz);
		}
		if (!clazz.binded) {
			clazz.binded = true;
			Method clinit = clazz.getDirectMethod("<clinit>", "()V");
			if (clinit != null) {
				Frame frame = loadThread.pushFrame();
				frame.init(clinit);
				try {
					loadThread.execute(true);
				} catch (ChangeThreadException e) {
					// TODO Implement here by checking the behavior of the class loading in The Java Virtual Machine Specification
				} catch (Throwable e) {
					vm.error(e);
				}
			}
		}
		return clazz;
	}

	protected Clazz findClass(final String name) {
		return null;
	}

	private Object loadClassesMutex = new Object();
	private byte[] dexFileContent;
	private int offset;
	private int[] oldOffset = new int[5];
	private int oldOffsetIndex = 0;

	private String[] strings;
	private String[] types;

	private String[] descriptors;

	private String[] fieldClasses;
	private String[] fieldTypes;
	private String[] fieldNames;

	private String[] methodClasses;
	private String[] methodTypes;
	private String[] methodNames;

	void loadClasses(final byte[] dexFileContent) {
		synchronized (loadClassesMutex) {
			this.dexFileContent = dexFileContent;
			offset = 0;

			checkData("magic number", "6465780A30333500");

			skip("checksum", 4);
			skip("SHA-1 signature", 20);

			checkUInt("file size", dexFileContent.length);
			checkUInt("header size", 0x70);
			checkUInt("endian", 0x12345678);

			checkUInt("link size", 0);
			checkUInt("link offset", 0);

			readMap();
			readStrings();
			readTypes();
			readDescriptors();
			readFields();
			readMethods();
			readClassContents();
		}
	}

	private void readClassContents() {
		int count = readUInt();
		int offset = readUInt();
		if (offset != 0) {
			pushOffset(offset);
			for (int i = 0; i < count; i++) {
				Clazz clazz = new Clazz(this);

				clazz.name = fromTypeToClassName(types[readUInt()]);

				clazz.flag = readUInt();
				clazz.isInterface = ((clazz.flag & ACC_INTERFACE) != 0) | ((clazz.flag & ACC_ABSTRACT) != 0);

				int superClassIndex = readUInt();
				if (hasNoValue(superClassIndex)) {
					clazz.superClass = "java/lang/Object";
				} else {
					clazz.superClass = fromTypeToClassName(types[superClassIndex]);
				}

				int interfacesOffset = readUInt();
				if (interfacesOffset != 0) {
					pushOffset(interfacesOffset);

					int interfaceCount = readUInt();
					String[] interfaces = clazz.interfaces = new String[interfaceCount];
					for (int j = 0; j < interfaceCount; j++) {
						interfaces[j] = fromTypeToClassName(types[readUShort()]);
					}

					popOffset();
				}
				skip("source file index", 4);
				skip("anotations offset", 4);

				int classDataOffset = readUInt();
				if (classDataOffset != 0) {
					pushOffset(classDataOffset);

					Field[] staticFields = new Field[readULEB128()];
					Field[] instanceFields = new Field[readULEB128()];
					Method[] directMethods = new Method[readULEB128()];
					Method[] virtualMethods = new Method[readULEB128()];

					readFields(clazz, staticFields, false);
					readFields(clazz, instanceFields, true);
					readMethodContents(clazz, directMethods);
					readMethodContents(clazz, virtualMethods);

					clazz.staticFields = staticFields;
					clazz.staticFieldMap = new Hashtable();
					for (int j = 0; j < staticFields.length; j++) {
						Field field = staticFields[j];
						clazz.staticFieldMap.put(field.name, field);
					}
					clazz.instanceFields = instanceFields;
					clazz.directMethods = directMethods;
					clazz.virtualMethods = virtualMethods;

					popOffset();
				}

				int staticValuesOffset = readUInt();
				if (staticValuesOffset != 0) {
					pushOffset(staticValuesOffset);

					int length = readULEB128();
					for (int j = 0; j < length; j++) {
						Field staticField = clazz.staticFields[j];

						int data = readUByte();
						int valueType = data & 0x1F;
						int valueArgument = data >> 5;
						switch (valueType) {
							case VALUE_BYTE:
								staticField.intValue = readByte();
								break;
							case VALUE_CHAR:
								staticField.intValue = (char)readValueByTypeArgument(valueArgument);
								break;
							case VALUE_SHORT:
								staticField.intValue = (short)readValueByTypeArgument(valueArgument);
								break;
							case VALUE_INT:
								staticField.intValue = (int)readValueByTypeArgument(valueArgument);
								break;
							case VALUE_LONG:
								staticField.longValue = (long)readValueByTypeArgument(valueArgument);
								break;
							case VALUE_STRING:
								staticField.objectValue = strings[(int)readValueByTypeArgument(valueArgument)];
								break;
							case VALUE_NULL:
								staticField.objectValue = null;
								break;
							case VALUE_BOOLEAN:
								staticField.intValue = valueArgument;
								break;
							default:
								throw new VirtualMachineException("not supported value type: 0x" + Integer.toHexString(valueType));
						}
					}
					popOffset();
				}

				classes.put(clazz.name, clazz);
			}
			popOffset();
		}
	}

	private static String fromTypeToClassName(final String type) {
		return type.substring(1, type.length() - 1);
	}

	private long readValueByTypeArgument(final int typeArgument) {
		return readSigned(typeArgument + 1);
	}

	private static boolean hasNoValue(final int value) {
		return value == -1;
	}

	private void readMethodContents(final Clazz clazz, final Method[] methods) {
		int methodIndex = 0;
		for (int i = 0, length = methods.length; i < length; i++) {
			if (i == 0) {
				methodIndex = readULEB128();
			} else {
				methodIndex += readULEB128();
			}
			Method method = new Method(clazz);

			method.strings = strings;
			method.types = types;

			method.descriptors = descriptors;

			method.fieldClasses = fieldClasses;
			method.fieldTypes = fieldTypes;
			method.fieldNames = fieldNames;

			method.methodClasses = methodClasses;
			method.methodTypes = methodTypes;
			method.methodNames = methodNames;

			method.flag = readULEB128();
			method.isInstance = (method.flag & ACC_STATIC) == 0;
			method.isSynchronized = (method.flag & ACC_SYNCHRONIZED) != 0;

			method.name = methodNames[methodIndex];
			method.descriptor = methodTypes[methodIndex];

			int codeOffset = readULEB128();
			if (codeOffset != 0) {
				pushOffset(codeOffset);

				method.registerCount = readUShort();
				method.incomingArgumentCount = readUShort();
				method.outgoingArgumentCount = readUShort();

				int tryItemCount = readUShort();
				int debugInfoOffset = readUInt();

				int codeLength = readUInt();
				int[] lowerCodes = method.lowerCodes = new int[codeLength];
				int[] upperCodes = method.upperCodes = new int[codeLength];
				int[] codes = method.codes = new int[codeLength];
				for (int j = 0; j < codeLength; j++) {
					int data = readUShort();
					lowerCodes[j] = data & 0xFF;
					upperCodes[j] = data >> 8;
					codes[j] = data;
				}
				if (codeLength % 2 != 0 && tryItemCount != 0) {
					skip("padding", 2);
				}

				int[] exceptionStartAddresses = method.exceptionStartAddresses = new int[tryItemCount];
				int[] exceptionEndAddresses = method.exceptionEndAdresses = new int[tryItemCount];
				int[] exceptionHandlerIndex = method.exceptionHandlerIndexes = new int[tryItemCount];
				if (tryItemCount != 0) {
					for (int j = 0; j < tryItemCount; j++) {
						int startAddress = exceptionStartAddresses[j] = readUInt();
						exceptionEndAddresses[j] = startAddress + readUShort();
						exceptionHandlerIndex[j] = readUShort();
					}

					int baseOffset = offset;
					int listCount = readULEB128();
					String[][] exceptionHandlerTypes = method.exceptionHandlerTypes = new String[listCount][];
					int[][] exceptionHandlerAddresses = method.exceptionHandlerAddresses = new int[listCount][];
					for (int j = 0; j < listCount; j++) {
						int comaredOffset = offset - baseOffset;
						for (int k = 0, k_length = exceptionStartAddresses.length; k < k_length; k++) {
							if (exceptionHandlerIndex[k] == comaredOffset) {
								exceptionHandlerIndex[k] = j;
							}
						}
						int handlerCount = readSLEB128();
						if (handlerCount <= 0) {
							exceptionHandlerTypes[j] = new String[-handlerCount + 1];
							exceptionHandlerAddresses[j] = new int[-handlerCount + 1];
						} else {
							exceptionHandlerTypes[j] = new String[handlerCount];
							exceptionHandlerAddresses[j] = new int[handlerCount];
						}
						for (int k = 0, k_length = Math.abs(handlerCount); k < k_length; k++) {
							exceptionHandlerTypes[j][k] = toDotSeparatorClassName(types[readULEB128()]);
							exceptionHandlerAddresses[j][k] = readULEB128();
						}
						if (handlerCount <= 0) {
							exceptionHandlerTypes[j][-handlerCount] = "java.lang.Throwable";
							exceptionHandlerAddresses[j][-handlerCount] = readULEB128();
						}
					}
				}

				popOffset();
			}

			methods[i] = method;
		}
	}

	private static String toDotSeparatorClassName(final String slashSeparatorClassName) {
		return slashSeparatorClassName.substring(1, slashSeparatorClassName.length() - 1).replace('/', '.');
	}

	private void readFields(final Clazz clazz, final Field[] fields, final boolean isInstance) {
		int fieldIndex = 0;
		for (int i = 0, length = fields.length; i < length; i++) {
			if (i == 0) {
				fieldIndex = readULEB128();
			} else {
				fieldIndex += readULEB128();
			}
			Field field = new Field(clazz);

			field.flag = readULEB128();
			field.isInstance = isInstance;

			field.name = fieldNames[fieldIndex];
			field.type = fieldTypes[fieldIndex];

			fields[i] = field;
		}
	}

	private void readMethods() {
		int count = readUInt();
		methodClasses = new String[count];
		methodTypes = new String[count];
		methodNames = new String[count];
		int offset = readUInt();
		if (offset != 0) {
			pushOffset(offset);
			for (int i = 0; i < count; i++) {
				String classType = types[readUShort()];
				methodClasses[i] = classType.substring(1, classType.length() - 1);
				methodTypes[i] = descriptors[readUShort()];
				methodNames[i] = strings[readUInt()];
			}
			popOffset();
		}
	}

	private void readFields() {
		int count = readUInt();
		fieldClasses = new String[count];
		fieldTypes = new String[count];
		fieldNames = new String[count];
		int offset = readUInt();
		if (offset != 0) {
			pushOffset(offset);
			for (int i = 0; i < count; i++) {
				String classType = types[readUShort()];
				fieldClasses[i] = classType.substring(1, classType.length() - 1);
				fieldTypes[i] = types[readUShort()];
				fieldNames[i] = strings[readUInt()];
			}
			popOffset();
		}
	}

	private void readDescriptors() {
		int count = readUInt();
		descriptors = new String[count];
		pushOffset(readUInt());
		for (int i = 0; i < count; i++) {
			StringBuffer buffer = new StringBuffer();
			skip("shorty index", 4);
			String returnType = types[readUInt()];

			int offset = readUInt();
			if (offset == 0) {
				buffer.append("()");
			} else {
				pushOffset(offset);

				buffer.append("(");
				int typeCount = readUInt();
				for (int j = 0; j < typeCount; j++) {
					buffer.append(types[readUShort()]);
				}
				buffer.append(")");
				popOffset();
			}

			buffer.append(returnType);
			descriptors[i] = buffer.toString();
		}
		popOffset();
	}

	private void readTypes() {
		int count = readUInt();
		types = new String[count];
		pushOffset(readUInt());
		for (int i = 0; i < count; i++) {
			types[i] = strings[readUInt()];
		}
		popOffset();
	}

	private void readStrings() {
		int count = readUInt();
		strings = new String[count];
		pushOffset(readUInt());
		for (int i = 0; i < count; i++) {
			pushOffset(readUInt());

			int stringLength = readULEB128();
			char[] chars = new char[stringLength];
			for (int j = 0, j_length = chars.length; j < j_length; j++) {
				int data = readUByte();
				switch (data >> 4) {
					case 0:
					case 1:
					case 2:
					case 3:
					case 4:
					case 5:
					case 6:
					case 7:
						chars[j] = (char)data;
						break;
					case 12:
					case 13:
						chars[j] = (char)(((data & 0x1F) << 6) | (readUByte() & 0x3F));
						break;
					case 14:
						chars[j] = (char)(((data & 0x0F) << 12) | ((readUByte() & 0x3F) << 6) | (readUByte() & 0x3F));
						break;
					default:
						throw new NoClassDefFoundError("illegal modified utf-8");
				}
			}
			strings[i] = convertStringBuilderToStringBuffer(new String(chars));

			popOffset();
		}
		popOffset();
	}

	private static String convertStringBuilderToStringBuffer(final String value) {
		int start = value.indexOf("java/lang/StringBuilder");
		if (start == -1) {
			return value;
		}
		StringBuffer returned = new StringBuffer();
		int end = 0;
		while (start != -1) {
			returned.append(value.substring(end, start));
			returned.append("java/lang/StringBuffer");
			end = start + "java/lang/StringBuilder".length();
			start = value.indexOf("java/lang/StringBuilder", end);
		}
		returned.append(value.substring(end));
		return returned.toString();
	}

	private int readSLEB128() {
		int value = 0;
		int shiftCount = 0;
		boolean hasNext = true;
		while (hasNext) {
			int data = readUByte();
			value |= (data & 0x7F) << shiftCount;
			shiftCount += 7;
			hasNext = (data & 0x80) != 0;
		}
		return (value << (32 - shiftCount)) >> (32 - shiftCount);
	}

	private int readULEB128() {
		int value = 0;
		int shiftCount = 0;
		boolean hasNext = true;
		while (hasNext) {
			int data = readUByte();
			value |= (data & 0x7F) << shiftCount;
			shiftCount += 7;
			hasNext = (data & 0x80) != 0;
		}
		return value;
	}

	private void readMap() {
		pushOffset(readUInt());

		int count = readUInt();
		for (int i = 0; i < count; i++) {
			int type = readUShort();
			skip("unused", 2);
			int itemSize = readUInt();
			int itemOffset = readUInt();
		}

		popOffset();
	}

	private void popOffset() {
		offset = oldOffset[--oldOffsetIndex];
	}

	private void pushOffset(final int offset) {
		oldOffset[oldOffsetIndex++] = this.offset;
		this.offset = offset;
	}

	private void checkUInt(final String type, final int valueToCheck) {
		if (readUInt() != valueToCheck) {
			throw new NoClassDefFoundError("illegal " + type);
		}
	}

	private void skip(final String type, final int count) {
		offset += count;
	}

	// TODO Change the return value from int to long
	private int readUInt() {
		return readUByte() | (readUByte() << 8) | (readUByte() << 16) | (readUByte() << 24);
	}

	private int readUShort() {
		return readUByte() | (readUByte() << 8);
	}

	private int readByte() {
		return dexFileContent[offset++];
	}

	private int readUByte() {
		return dexFileContent[offset++] & 0xFF;
	}

	private long readSigned(final int byteLength) {
		long value = 0;
		for (int i = 0; i < byteLength; i++) {
			value |= (long)readUByte() << (8 * i);
		}
		int shift = 8 * byteLength;
		return (value << shift) >> shift;
	}

	private void checkData(final String type, final String valueToCheck) {
		for (int i = 0, length = valueToCheck.length() / 2; i < length; i++) {
			if (readUByte() != Integer.parseInt(valueToCheck.substring(i * 2, i * 2 + 2), 16)) {
				throw new NoClassDefFoundError("illegal " + type);
			}
		}
	}
}
