This project's objective is to develop a pure Java implementation of the Android's Dalvik virtual machine.

To study the concept and architecture of Dalvik VM, [Koji Hisano](mailto:koji.hisano@eflow.jp) is developing this for J2ME CLDC environments as on-going research at "[eflow Inc.](http://www.eflow.jp/)"

## Currently supported functions ##
  * Dalvik Execution file format (.dex)
  * Complete Dalvik instruction set
  * J2ME CLDC API
  * Multi-thread (include synchronized block, wait and notify)

## Unsupported functions ##
  * Android frameworks (as implemented on CLDC)
  * Verifier
  * Debugger
  * Some array types
  * Annotation (as implemented on CLDC)
  * Enum (as implemented on CLDC)

## Documents ##
  * [Change history of this project](ChangeHistory.md)
  * [Tutorial 1: How to run a program](Tutorial1.md)