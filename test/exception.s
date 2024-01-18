.globl main
.text
main:
 	la t0,handler
 	csrrw zero, 0x305, t0 # set mtvec
 	csrrsi zero, 0x300, 1 # set interrupt enable in mstatus
 	lw zero, 0(zero)        # trigger trap
failure:
	li a0, 0
	li a7, 93
	ecall
success:
 	li a0, 42
 	li a7, 93
 	ecall
handler:
 	# move epc to success and return
	la t0, success 	
	csrrw zero, 0x341, t0
	mret
	
