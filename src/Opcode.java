public enum Opcode {
    NOOP, // no operation
    ADD, // rf[Rd] <- rf[Rs1] + rf[Rs2]
    ADDI, // rf[Rd] <- rf[Rs1] + Const
    SUB, // rf[Rd] <- rf[Rs1] - rf[Rs2]
    MUL, // rf[Rd] <- rf[Rs1] * rf[Rs2]
    MULI, // rf[Rd] <- rf[Rs1] * Const
    DIV, // rf[Rd] <- rf[Rs1] / rf[Rs2]
    DIVI, // rf[Rd] <- rf[Rs1] / Const
    SHL, // rf[Rd] <- rf[Rs1] << rf[Rs2]
    SHR, // rf[Rd] <- rf[Rs1] >> rf[Rs2]
    NOT, // rf[Rd] <- ~rf[Rs1]
    AND, // rf[Rd] <- rf[Rs1] & rf[Rs2]
    OR, // rf[Rd] <- rf[Rs1] | rf[Rs2]
    MOVC, // rf[Rd] <- Const
    LD, // rf[Rd] <- mem[ rf[Rs1] + rf[Rs2] ]
    LDO, // rf[Rd] <- mem[ rf[Rs1] + Const ]
    ST, // mem[ rf[Rs1] + rf[Rs2] ] <- rf[Rd]
    STO, // mem[ rf[Rs1] + Const ] <- rf[Rd]
    MOV, // rf[Rd] <- rf[Rs1]
    BR, // pc <- Const
    JMP, // pc <- pc + Const
    JR, // pc <- rf[Rs1] + Const
    BEQ, // pc <- Const if (rf[Rs1] == rf[Rs2]) with CMP redundant
    BLT, // pc <- Const if (rf[Rs1] < rf[Rs2])  with CMP redundant
    CMP, // rf[Rd] <- -1 if(rf[Rs1] < rf[Rs2]), 0 if(rf[Rs1] == rf[Rs2]), 1 if(rf[Rs1] > rf[Rs2])
    BZ, // pc <- Const if (rf[Rs1] == 0)
    BN, // pc <- Const if (rf[Rs1] < 0)
    BP, // pc <- Const if (rf[Rs1] > 0)
    HALT // Terminate processor
}
