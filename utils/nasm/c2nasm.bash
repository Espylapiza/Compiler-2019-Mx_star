#!/bin/bash

########################################################################
#
# Usage: $bash c2nasm.bash FILENAME.c
# The bash will generate the objective file FILENAME.asm.
#
# https://gist.github.com/abcdabcd987/acb76b101094edac57537ab54ef1c4ef
#
########################################################################

# thanks to http://stackoverflow.com/a/20743090
# thanks to https://github.com/diogovk/c2nasm
# install objconv: https://github.com/vertis/objconv
#
# $1: source code

set -e
C_FILE="$1"
BASE_NAME="${C_FILE%.*}"
O_FILE="$BASE_NAME.o"
NASM_FILE="$BASE_NAME.asm"
gcc -Werror=implicit-function-declaration -fno-asynchronous-unwind-tables -O2 -c -o "$O_FILE" "$C_FILE"
./utils/nasm/objconv/objconv -fnasm "$O_FILE" "$NASM_FILE"
sed -i 's/st(0)/st0  /g' "$NASM_FILE"
sed -i 's/.LC/_LC/g' "$NASM_FILE"
sed -i 's/noexecute/         /g' "$NASM_FILE"
sed -i 's/execute/       /g' "$NASM_FILE"
sed -i 's/: function//g' "$NASM_FILE"
sed -i 's/?_/L_/g' "$NASM_FILE"
sed -i -n '/SECTION .eh_frame/q;p' "$NASM_FILE"
sed -i 's/;.*//g' "$NASM_FILE"
sed -i 's/^M//g' "$NASM_FILE"
sed -i 's/\s\+$//g' "$NASM_FILE"
sed -i 's/align=1[0-9]*//g' "$NASM_FILE"
rm "$O_FILE"
cat $NASM_FILE
