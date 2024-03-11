#!/usr/bin/python

# for integrations of xiangshan-verilog and chipyard, to rename some modules

import sys
import re
import os

inVlog = "../xiangshan/src/main/resources/vsrc/baseConfig/XSTop.v"
print("[INFO] modules from: " + str(inVlog))

replace_list = ["TLXbar", "TLFIFOFixer", "TLWidthWidget", "TLWidthWidget_1", "TLWidthWidget_2", "TLXbar_1", "TLXbar_2", "Queue", "Queue_1", "TLBuffer", "Repeater", "TLBuffer_2", "TLXbar_3", "TLXbar_4", "TLBuffer_5", "TLError", "TLBuffer_7", "Queue_16", "TLXbar_6", "TLBuffer_13", "QueueCompatibility", "AXI4UserYanker", "AXI4IdIndexer", "TLToAXI4", "SourceA", "SourceC", "SourceD", "SourceE", "Queue_24", "SinkA", "Queue_25", "SinkD", "Directory", "MSHR", "Scheduler", "TLBuffer_15", "TLBuffer_16", "IDPool", "TLCacheCork", "BankBinder", "TLXbar_7", "Queue_32", "LevelGateway", "PLICFanIn", "TLPLIC", "CLINT", "DMIToTL", "TLDebugModuleOuter", "TLBusBypassBar", "TLError_1", "TLBusBypass", "AsyncResetSynchronizerPrimitiveShiftReg_d3_i0", "AsyncResetSynchronizerShiftReg_w1_d3_i0", "AsyncResetSynchronizerShiftReg_w1_d3_i0_1", "JtagStateMachine", "CaptureUpdateChain_2", "JtagTapController", "JtagBypassChain", "DebugTransportModuleJTAG", "AsyncValidSync", "AsyncQueueSource", "ClockCrossingReg_w43", "AsyncQueueSink", "TLAsyncCrossingSource", "AsyncQueueSource_1", "TLDebugModuleOuterAsync", "TLDebugModuleInner", "ClockCrossingReg_w55", "AsyncQueueSink_1", "AsyncQueueSource_2", "TLAsyncCrossingSink", "ClockCrossingReg_w15", "AsyncQueueSink_2", "TLDebugModuleInnerAsync", "TLDebugModule", "CaptureUpdateChain", "CaptureUpdateChain_1", "CaptureChain"]


try:
    for item in replace_list:
        with open(inVlog, 'r') as file:
            filedata = file.read()
        # Use regular expressions to replace the module declaration
        # This pattern assumes that the module declaration may have leading spaces, 
        # and captures the entire line containing the module declaration.
        module_declaration_pattern = r'(\bmodule\s+)' + re.escape(item) + r'(\s*\(|\s+;|\s+//|\s+/\*)'
        module_reference_pattern = r'(?<!\w)' + re.escape(item) + r'(?!\w)'

        print("processing pattern:" + item)

        # Replace the module declaration and references
        replaced_data = re.sub(module_declaration_pattern, r'\1' + "xs_" + item + r'\2', filedata)
        replaced_data = re.sub(module_reference_pattern, "xs_"+item, replaced_data)
        # Write the file out again
        with open(inVlog, 'w') as file:
            file.write(replaced_data)

except IOError as e:
    print(f"An error occurred while trying to read or write the file: {e}")
