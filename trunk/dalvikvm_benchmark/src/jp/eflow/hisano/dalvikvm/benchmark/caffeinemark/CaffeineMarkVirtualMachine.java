package jp.eflow.hisano.dalvikvm.benchmark.caffeinemark;

import henson.midp.Float11;
import jp.eflow.hisano.dalvikvm.*;

public class CaffeineMarkVirtualMachine extends VirtualMachine {
	protected void error(Throwable e) {
		e.printStackTrace();
	}

	protected boolean handleClassMethod(Frame frame, String absoluteClassName, String methodName, String methodDescriptor) throws Exception {
		if (super.handleClassMethod(frame, absoluteClassName, methodName, methodDescriptor)) {
			return true;
		} else {
			String packageName = absoluteClassName.substring(0, absoluteClassName.lastIndexOf('/'));
			String className = absoluteClassName.substring(absoluteClassName.lastIndexOf('/') + 1);
			if ("java/lang".equals(packageName)) {
				if ("Math".equals(className)) {
					if ("log".equals(methodName) && "(D)D".equals(methodDescriptor)) {
						frame.doubleReturn = Double.doubleToLongBits(Float11.log(Double.longBitsToDouble(getLong(frame.intArguments, 0))));
						return true;
					} else if ("exp".equals(methodName) && "(D)D".equals(methodDescriptor)) {
						frame.doubleReturn = Double.doubleToLongBits(Float11.exp(Double.longBitsToDouble(getLong(frame.intArguments, 0))));
						return true;
					} else {
						return false;
					}
				}
			}
			return false;
		}
	}
}
