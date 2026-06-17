#!/bin/bash

enip_get_attribute -S "@22/1/6=10,1,2,65,66,67,69,1,0,0,0,255,255,255,255,6,66,246,230,102,5,65,66,67,68,69,0,0,0,0"
# IMPORTANT:
# Data has to include all bytes (ie. 30)
# Pay attention to return message:
# "@0x0016/1/6 == True" <= indicates success
# "@0x0016/1/6 == None" <= indicates error
# Expected: Mon Jun  2 14:32:34 2025:   0: Single S_A_S      @0x0016/1/6 == True

enip_client -p "STRING=(STRING)STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890STRING_01234567890"
enip_client -p "TEXT=(SSTRING)SHORT_STRING_SHORT_STRING_SHORT_STRING"
