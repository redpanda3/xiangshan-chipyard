# list of srcs for large and small configurations of Xiangshan
# note: bulk includes all files in vsrc/*/ doesn't work since some of the files have syntax errors

xiangshan_baseConfig_vsrcs := \
	$(xiangshan_blocks_dir)/vsrc/baseConfig/array_ext.v \
	$(xiangshan_blocks_dir)/vsrc/baseConfig/array_*_ext.v \
	$(xiangshan_blocks_dir)/vsrc/baseConfig/Difftest*.v \
	$(xiangshan_blocks_dir)/vsrc/baseConfig/XSTop*.v 

