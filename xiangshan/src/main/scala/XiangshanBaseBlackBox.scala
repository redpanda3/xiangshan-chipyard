//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
// Xiangshan Blackbox Wrapper
// by redpanda3
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
package xiangshan

import sys.process._

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, StringParam, RawParam}

import scala.collection.mutable.{ListBuffer}

class XSTile extends BlackBox with HasBlackBoxPath
{

  val io = IO(new Bundle {
    
    val clock = Input(Clock())
    val reset = Input(Bool())

    val auto_misc_beu_int_out_0 = Output(Bool())
    val auto_misc_memory_port_out_a_ready = Input(Bool())
    val auto_misc_memory_port_out_a_valid = Output(Bool())
    val auto_misc_memory_port_out_a_bits_opcode = Output(UInt(3.W))
    val auto_misc_memory_port_out_a_bits_param = Output(UInt(3.W))
    val auto_misc_memory_port_out_a_bits_size = Output(UInt(3.W))
    val auto_misc_memory_port_out_a_bits_source = Output(UInt(7.W))
    val auto_misc_memory_port_out_a_bits_address = Output(UInt(36.W))
    val auto_misc_memory_port_out_a_bits_mask = Output(UInt(32.W))
    val auto_misc_memory_port_out_a_bits_data = Output(UInt(256.W))
    val auto_misc_memory_port_out_bready = Output(Bool())
    val auto_misc_memory_port_out_bvalid = Input(Bool())
    //val auto_misc_memory_port_out_bopcode = Input(UInt(3.W))
    val auto_misc_memory_port_out_bparam = Input(UInt(2.W))
    //val auto_misc_memory_port_out_bsize = Input(UInt(3.W))
    val auto_misc_memory_port_out_bsource = Input(UInt(7.W))
    val auto_misc_memory_port_out_baddress = Input(UInt(36.W))
    val auto_misc_memory_port_out_c_ready = Input(Bool())
    val auto_misc_memory_port_out_c_valid = Output(Bool())
    val auto_misc_memory_port_out_c_bits_opcode = Output(UInt(3.W))
    val auto_misc_memory_port_out_c_bits_param = Output(UInt(3.W))
    val auto_misc_memory_port_out_c_bits_size = Output(UInt(3.W))
    val auto_misc_memory_port_out_c_bits_source = Output(UInt(7.W))
    val auto_misc_memory_port_out_c_bits_address = Output(UInt(36.W))
    val auto_misc_memory_port_out_c_bits_data = Output(UInt(256.W))
    val auto_misc_memory_port_out_d_ready = Output(Bool())
    val auto_misc_memory_port_out_d_valid = Input(Bool())
    val auto_misc_memory_port_out_d_bits_opcode = Input(UInt(3.W))
    val auto_misc_memory_port_out_d_bits_param = Input(UInt(2.W))
    val auto_misc_memory_port_out_d_bits_size = Input(UInt(3.W))
    val auto_misc_memory_port_out_d_bits_source = Input(UInt(7.W))
    val auto_misc_memory_port_out_d_bits_sink = Input(UInt(6.W))
    val auto_misc_memory_port_out_d_bits_denied = Input(Bool())
    val auto_misc_memory_port_out_d_bits_data = Input(UInt(256.W))
    val auto_misc_memory_port_out_e_ready = Input(Bool())
    val auto_misc_memory_port_out_e_valid = Output(Bool())
    val auto_misc_memory_port_out_e_bits_sink = Output(UInt(6.W))
    val auto_misc_mmio_port_out_a_ready = Input(Bool())
    val auto_misc_mmio_port_out_a_valid = Output(Bool())
    val auto_misc_mmio_port_out_a_bits_opcode = Output(UInt(3.W))
    val auto_misc_mmio_port_out_a_bits_size = Output(UInt(3.W))
    val auto_misc_mmio_port_out_a_bits_source = Output(Bool())
    val auto_misc_mmio_port_out_a_bits_address = Output(UInt(36.W))
    val auto_misc_mmio_port_out_a_bits_mask = Output(UInt(8.W))
    val auto_misc_mmio_port_out_a_bits_data = Output(UInt(64.W))
    val auto_misc_mmio_port_out_d_ready = Output(Bool())
    val auto_misc_mmio_port_out_d_valid = Input(Bool())
    val auto_misc_mmio_port_out_d_bits_opcode = Input(UInt(3.W))
    val auto_misc_mmio_port_out_d_bits_size = Input(UInt(3.W))
    val auto_misc_mmio_port_out_d_bits_source = Input(Bool())
    val auto_misc_mmio_port_out_d_bits_data = Input(UInt(64.W))
    val auto_core_plic_int_sink_in_1_0 = Input(Bool())
    val auto_core_plic_int_sink_in_0_0 = Input(Bool())
    val auto_core_debug_int_sink_in_0 = Input(Bool())
    val auto_core_clint_int_sink_in_0 = Input(Bool())
    val auto_core_clint_int_sink_in_1 = Input(Bool())
    val io_hartId = Input(UInt(64.W))
  })

  val chipyardDir = System.getProperty("user.dir")
  val xiangshanVsrcDir = s"$chipyardDir/generators/xiangshan/src/main/resources"

  val proc = s"make -C $xiangshanVsrcDir"
  require(proc.! == 0, "Failed to run pre-processing step")

  addPath(s"$xiangshanVsrcDir/xiangshanBaseConfig.preprocessed.v")
}
