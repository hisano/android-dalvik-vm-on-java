# Introduction #

This tutorial describes how to run a simple program on this vm step by step.

# Details #

1. Compile the below code and package it as App.jar
```
package tutorial1;

public final class HelloWorld {
	public static void main(final String[] args) {
		System.out.println("Hello, World!");
	}
}
```

2. Convert App.jar to App.dex which is a Dalvik Executable file
```
dx --dex --output=<absolute file path to tutorial1/App.dex> <absolute file path to tutorial1/App.jar>
```

3. Write a launcher code and run it
```
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
```