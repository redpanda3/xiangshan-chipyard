//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
// Xiangshan Tile Wrapper
// by redpanda3
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
package xiangshan

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, StringParam, RawParam}

import scala.collection.mutable.{ListBuffer}

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.{RocketCrossingParams}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.prci.ClockSinkParameters 


case class XiangshanCoreParams(
  bootFreqHz: BigInt = BigInt(1700000000),

) extends CoreParams{
  val decodeWidth: Int = 6
  val fetchWidth: Int = 8
  val fpu: Option[FPUParams] = Some(FPUParams()) // copied fma latencies from Rocket
  val haveBasicCounters: Boolean = true
  val haveCFlush: Boolean = false
  val haveFSDirty: Boolean = true
  val instBits: Int = 64
  val lrscCycles: Int = 80 // worst case is 14 mispredicted branches + slop or need to check
  val mcontextWidth: Int = 0
  val misaWritable: Boolean = false
  val mtvecInit: Option[BigInt] = Some(BigInt(0))
  val mtvecWritable: Boolean = true
  val mulDiv: Option[freechips.rocketchip.rocket.MulDivParams] = Some(MulDivParams(divEarlyOut=true))
  val nBreakpoints: Int = 0 // TODO Fix with better frontend breakpoint unit
  val nL2TLBEntries: Int = 512
  val nL2TLBWays: Int = 1
  val nLocalInterrupts: Int = 0
  val nPMPs: Int = 8
  val nPTECacheEntries: Int = 8 // TODO: check
  val nPerfCounters: Int = 0
  val pmpGranularity: Int = 4
  val retireWidth: Int = 1
  val scontextWidth: Int = 0
  val useAtomics: Boolean = true
  val useAtomicsOnlyForIO: Boolean = false
  val useBPWatch: Boolean = false
  val useCompressed: Boolean = true
  val useDebug: Boolean = true
  val useHypervisor: Boolean = false
  val useNMI: Boolean = false
  val useRVE: Boolean = false
  val useSCIE: Boolean =false
  val useSupervisor: Boolean = false
  val useUser: Boolean = true //TODO: check
  val useVM: Boolean = true
  val boundaryBuffers: Boolean = false // if synthesized with hierarchical PnR, cut feed-throughs?
}


/**
 * Xiangshan tile parameter class used in configurations
 *
 */
case class XiangshanTileParams(
  name: Option[String] = Some("xiangshan_tile"),
  hartId: Int = 0,
  val core: XiangshanCoreParams = XiangshanCoreParams()
) extends InstantiableTileParams[XiangshanTile]
{
  val beuAddr: Option[BigInt] = None
  val blockerCtrlAddr: Option[BigInt] = None
  val btb: Option[BTBParams] = None
  val boundaryBuffers: Boolean = false
  val dcache: Option[DCacheParams] = None //already implemented
  val icache: Option[ICacheParams] = None //no icache, currently in draft so turning option off
  val clockSinkParams: ClockSinkParameters = ClockSinkParameters()
  def instantiate(crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters): XiangshanTile = {
    new XiangshanTile(this, crossing, lookup)
  }
}

case class XiangshanTileAttachParams(
  tileParams: XiangshanTileParams,
  crossingParams: RocketCrossingParams
) extends CanAttachTile {
  type TileType = XiangshanTile
  val lookup = PriorityMuxHartIdFromSeq(Seq(tileParams))
}

/**
 * Xiangshan tile
 *
 */
class XiangshanTile private(
  val xiangshanParams: XiangshanTileParams,
  crossing: ClockCrossingType,
  lookup: LookupByHartIdImpl,
  q: Parameters)
  extends BaseTile(xiangshanParams, crossing, lookup, q)
  with SinksExternalInterrupts
  with SourcesExternalNotifications
{
  def this(params: XiangshanTileParams, crossing: TileCrossingParamsLike, lookup: LookupByHartIdImpl)(implicit p: Parameters) =
    this(params, crossing.crossingType, lookup, p)

  //TileLink nodes
  val intOutwardNode = IntIdentityNode()
  val masterNode = visibilityNode
  val slaveNode = TLIdentityNode()

  // val tile_master_blocker =
  //   tileParams.blockerCtrlAddr
  //     .map(BasicBusBlockerParams(_, xBytes, masterPortBeatBytes, deadlock = true))
  //     .map(bp => LazyModule(new BasicBusBlocker(bp)))

  // tile_master_blocker.foreach(lm => connectTLSlave(lm.controlNode, xBytes))

  // // TODO: this doesn't block other masters, e.g. RoCCs
  // tlOtherMastersNode := tile_master_blocker.map { _.node := tlMasterXbar.node } getOrElse { tlMasterXbar.node }
  // masterNode :=* tlOtherMastersNode

  tlOtherMastersNode := tlMasterXbar.node
  masterNode :=* tlOtherMastersNode
  DisableMonitors { implicit p => tlSlaveXbar.node :*= slaveNode }

  override lazy val module = new XiangshanTileModuleImp(this)
  
  val node = TLIdentityNode()

  val l2_node = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      clients = Seq(TLMasterParameters.v1(
        name = "xiangshan-l2-port",
        sourceId = IdRange(0, 16))))))

  val mmio_node = TLClientNode(
    Seq(TLMasterPortParameters.v1(
      clients = Seq(TLMasterParameters.v1(
        name = "xiangshan-mmio-port",
        sourceId = IdRange(0, 16))))))

  tlMasterXbar.node := node := TLBuffer() := l2_node 
  tlMasterXbar.node := node := TLBuffer() := mmio_node

  // Required entry of CPU device in the device tree for interrupt purpose
  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("openxiangshan, xiangshan", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping ++
                        cpuProperties ++
                        nextLevelCacheProperty ++
                        tileProperties)
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(hartId))
  }

  def connectXiangshanInterrupts(plic_0: Bool, plic_1: Bool, debug_0: Bool, clint_0: Bool, clint_1: Bool) {
    val (plic_interrupts, _) = intSinkNode.in(0)
    val (debug_interrupts, _) = intSinkNode.in(0)
    val (clint_interrupts, _) = intSinkNode.in(0)

    plic_0 := plic_interrupts(0)
    plic_1 := plic_interrupts(1)

    debug_0 := debug_interrupts(0)

    clint_0 := debug_interrupts(0)
    clint_1 := debug_interrupts(1)
  }
}


/**
 * Xiangshan tile implementation
 *
 * @param outer top level Xiangshan tile
 */
class XiangshanTileModuleImp(outer: XiangshanTile) extends BaseTileModuleImp(outer){
  // annotate the parameters
  Annotated.params(this, outer.xiangshanParams)

  val u_core = Module(new XSTile)

  //connect signals
  u_core.io.clock := clock
  u_core.io.reset := reset
  //hartid
  u_core.io.io_hartId := outer.hartIdSinkNode.bundle

  val (l2, _) = outer.l2_node.out(0)
  //a
  l2.a.valid := u_core.io.auto_misc_memory_port_out_a_valid
  u_core.io.auto_misc_memory_port_out_a_ready := l2.a.ready
  l2.a.bits.opcode := u_core.io.auto_misc_memory_port_out_a_bits_opcode
  l2.a.bits.param := u_core.io.auto_misc_memory_port_out_a_bits_param
  l2.a.bits.size := u_core.io.auto_misc_memory_port_out_a_bits_size
  l2.a.bits.source := u_core.io.auto_misc_memory_port_out_a_bits_source
  l2.a.bits.address := u_core.io.auto_misc_memory_port_out_a_bits_address
  l2.a.bits.mask := u_core.io.auto_misc_memory_port_out_a_bits_mask
  l2.a.bits.data := u_core.io.auto_misc_memory_port_out_a_bits_data
  //b
  u_core.io.auto_misc_memory_port_out_bvalid := l2.b.valid
  l2.b.ready := u_core.io.auto_misc_memory_port_out_bready
  //u_core.io.auto_misc_memory_port_out_b_bits_opcode := l2.b.bits.opcode
  u_core.io.auto_misc_memory_port_out_bparam := l2.b.bits.param
  //u_core.io.auto_misc_memory_port_out_bsize := l2.b.bits.size
  u_core.io.auto_misc_memory_port_out_bsource := l2.b.bits.source
  u_core.io.auto_misc_memory_port_out_baddress := l2.b.bits.address
  //c
  l2.c.valid := u_core.io.auto_misc_memory_port_out_c_valid
  u_core.io.auto_misc_memory_port_out_c_ready := l2.c.ready
  l2.c.bits.opcode := u_core.io.auto_misc_memory_port_out_c_bits_opcode
  l2.c.bits.param := u_core.io.auto_misc_memory_port_out_c_bits_param
  l2.c.bits.size := u_core.io.auto_misc_memory_port_out_c_bits_size
  l2.c.bits.source := u_core.io.auto_misc_memory_port_out_c_bits_source
  l2.c.bits.address := u_core.io.auto_misc_memory_port_out_c_bits_address
  l2.c.bits.data := u_core.io.auto_misc_memory_port_out_c_bits_data
  //d
  u_core.io.auto_misc_memory_port_out_d_valid := l2.d.valid
  l2.d.ready := u_core.io.auto_misc_memory_port_out_d_ready
  u_core.io.auto_misc_memory_port_out_d_bits_opcode := l2.d.bits.opcode
  u_core.io.auto_misc_memory_port_out_d_bits_param := l2.d.bits.param
  u_core.io.auto_misc_memory_port_out_d_bits_size := l2.d.bits.size
  u_core.io.auto_misc_memory_port_out_d_bits_source := l2.d.bits.source
  u_core.io.auto_misc_memory_port_out_d_bits_sink := l2.d.bits.sink
  u_core.io.auto_misc_memory_port_out_d_bits_denied := l2.d.bits.denied
  u_core.io.auto_misc_memory_port_out_d_bits_data := l2.d.bits.data
  //e
  l2.e.valid := u_core.io.auto_misc_memory_port_out_e_valid
  u_core.io.auto_misc_memory_port_out_e_ready := l2.e.ready
  l2.e.bits.sink := u_core.io.auto_misc_memory_port_out_e_bits_sink

  val (mmio, _) = outer.mmio_node.out(0)
  //a
  mmio.a.valid := u_core.io.auto_misc_mmio_port_out_a_valid
  u_core.io.auto_misc_mmio_port_out_a_ready := mmio.a.ready
  mmio.a.bits.opcode := u_core.io.auto_misc_mmio_port_out_a_bits_opcode
  mmio.a.bits.size := u_core.io.auto_misc_mmio_port_out_a_bits_size
  mmio.a.bits.source := u_core.io.auto_misc_mmio_port_out_a_bits_source
  mmio.a.bits.address := u_core.io.auto_misc_mmio_port_out_a_bits_address
  mmio.a.bits.mask := u_core.io.auto_misc_mmio_port_out_a_bits_mask
  mmio.a.bits.data := u_core.io.auto_misc_mmio_port_out_a_bits_data
  //d
  u_core.io.auto_misc_mmio_port_out_d_valid := mmio.d.valid
  mmio.d.ready := u_core.io.auto_misc_mmio_port_out_d_ready
  u_core.io.auto_misc_mmio_port_out_d_bits_opcode := mmio.d.bits.opcode
  u_core.io.auto_misc_mmio_port_out_d_bits_size := mmio.d.bits.size
  u_core.io.auto_misc_mmio_port_out_d_bits_source := mmio.d.bits.source
  u_core.io.auto_misc_mmio_port_out_d_bits_data := mmio.d.bits.data

  
  outer.connectXiangshanInterrupts(u_core.io.auto_core_plic_int_sink_in_1_0, u_core.io.auto_core_plic_int_sink_in_0_0,
  u_core.io.auto_core_debug_int_sink_in_0, u_core.io.auto_core_clint_int_sink_in_0, u_core.io.auto_core_clint_int_sink_in_1)
}