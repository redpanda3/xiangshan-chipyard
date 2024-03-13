# xiangshan-chipyard
Integrate xiangshan IP into chipyard. This is only my solution. 

## Introduction

Xiangshan is an open-source high-performance RISC-V processor, chipyard is framework for RISC-V processors.
Here is my approach to integrate xiangshan into chipyard: use xiangshan to emit verilog, create a chisel-wrapper, then integrate it by diplomacy. 

## Usage

Three parts needs to be modified, generator, configurator, simulator. 

### Generator 

copy xiangshan under this repo into chipyard/generators

```
cp -rf xiangshan chipyard/generators/xiangshan
```

go to the xiangshan(https://github.com/OpenXiangShan/XiangShan) project repo, emit the xiangshan baseConfig verilog, copy all the emitted verilog to chipyard/generators/xiangshan/src/main/resources/vsrc/baseConfig

```
cp -rf xiangshan/build/* chipyard/generators/xiangshan/src/main/resources/vsrc/baseConfig/.
```
To avoid the naming issue(since some modules in xiangshan share the same name with the modules in chipyard-generated files) in XSTop.v, run renamer.py

```
python chipyard/generators/xiangshan/renamer.py
```

In the renamer, if further modules has naming conflicts, add those module names into the list within the renamer.

go to chipyard/generate/src/main/resources, generate the xiangshanBaseConfig.prepossessed.v

```
make

```
You will see xiangshanBaseConfig.prepossessed.v


### Configurator

In chipyard/build.sbt add
```
lazy val xiangshan = (project in file("generators/xiangshan"))
  .dependsOn(rocketchip)
  .settings(libraryDependencies ++= rocketLibDeps.value)
  .settings(commonSettings)
```

and in the chipyard dependencies add xiangshan

```
lazy val chipyard = (project in file("generators/chipyard"))
  .dependsOn(testchipip, rocketchip, boom, hwacha, sifive_blocks, sifive_cache, iocell, xiangshan,
    sha3, // On separate line to allow for cleaner tutorial-setup patches
    dsptools, `rocket-dsp-utils`,
    gemmini, icenet, tracegen, cva6, nvdla, sodor, ibex, fft_generator)
  .settings(libraryDependencies ++= rocketLibDeps.value)
  .settings(commonSettings)
```

### Simulation

In chipyard/generators/chipyard/src/main/scala/config, create XiangshanConfigs.scala, add a soc configuration of Xiangshan.


```
package chipyard

import chisel3._

import freechips.rocketchip.config.{Config}

// ---------------------
// Xiangshan Configs
// ---------------------


class XiangshanConfig extends Config(
  new xiangshan.WithNXiangshanCores(1) ++
  new chipyard.config.AbstractConfig)
```

Then go to chipyard/sims/vcs or chipyard/sims/verilator

```
make debug -j16 CONFIG=XiangshanConfig
```

simv-chipyard-XiangshanConfig-debug is generated.

## Notice

This solution is tested on Nanhu xiangshan only. If you want to try the latest xiangshan integration, you will need to make some adjustment.

This solution is for simulation only, since I haven't fully cleaned the warnings.

## Common Problem & My Solutions

1. Lint Error: More checking on the interface on XiangshanBaseBlackBox.scala and XiangshanTile.scala, sometimes width of ports in xiangshan and width of ports in chipyard(depends on which version you are using) are not the consistent.
2. DPI-C in Difftest* Modules, including some import errors: This happens in the latest version of xiangshan. My solution is to generate a xiangshan verilog in nanhu version, and replace the Difftest related modules to the latest version.
3. simv-chipyard-XiangshanConfig-debug cannot successfully simulate the riscv workload: Check on the waveforms.

## Maintenance

I will go back this project every half year, welcome to try.

## Acknowledgement

This is a work started from 2 years ago, due to many reasons, plus only some untested code was built during that time, this work was discontinued. Two month ago, I restarted this work in my spare time. 

And thanks for support from Jiawei Lin and Yungao Bao in Xiangshan team.  