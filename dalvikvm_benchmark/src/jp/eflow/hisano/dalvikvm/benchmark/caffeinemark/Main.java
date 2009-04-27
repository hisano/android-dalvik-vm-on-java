package jp.eflow.hisano.dalvikvm.benchmark.caffeinemark;

import org.apache.commons.io.IOUtils;

public final class Main {
	public static void main(String[] args) throws Exception {
		CaffeineMarkVirtualMachine vm = new CaffeineMarkVirtualMachine();
		vm.load(IOUtils.toByteArray(Main.class.getResourceAsStream("embedded_caffeine_mark.dex")));
		vm.run("CaffeineMarkEmbeddedApp", new String[0]);
	}
}
