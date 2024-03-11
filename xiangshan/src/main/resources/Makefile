#########################################################################################
# pre-process xiangshan into a single blackbox file
#########################################################################################

xiangshan_blocks_dir := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))

# name of output pre-processed verilog file
PREPROC_VERILOG = $(xiangshan_blocks_dir)/xiangshanBaseConfig.preprocessed.v

.PHONY: default $(PREPROC_VERILOG)
default: $(PREPROC_VERILOG)

#########################################################################################
# includes and vsrcs
#########################################################################################

lookup_srcs = $(shell find -L $(1)/ -name target -prune -o -iname "*.$(2)" -print 2> /dev/null)

VLOG_DIR = $(xiangshan_blocks_dir)/vsrc/$(NVDLA_TYPE)

include $(xiangshan_blocks_dir)/vsrc.mk

ALL_VSRCS = \
	$(xiangshan_baseConfig_vsrcs) 

INC_DIRS = $(sort $(dir $(call lookup_srcs,$(VLOG_DIR)/vmod,vh)))

#########################################################################################
# pre-process using custom script to replace the includes (but leave rest unaffected)
#########################################################################################

PREPROC_SCRIPT = $(xiangshan_blocks_dir)/../../../../../scripts/insert-includes.py
#RENAMING_SCRIPT = $(xiangshan_blocks_dir)/../../../renamer.py

$(PREPROC_VERILOG): $(ALL_VSRCS)

	mkdir -p $(dir $(PREPROC_VERILOG))
	cat $(ALL_VSRCS) > combined.v
	$(PREPROC_SCRIPT) combined.v $@ $(INC_DIRS)
	rm -rf combined.v

clean:
	rm -rf $(xiangshan_blocks_dir)/*.preprocessed.v
