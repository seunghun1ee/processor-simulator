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
    LDI, // rf[Rd] <- mem[ rf[Rs1] + Const ]
    ST, // mem[ rf[Rs1] + rf[Rs2] ] <- rf[Rd]
    STI, // mem[ rf[Rs1] + Const ] <- rf[Rd]
    MOV, // rf[Rd] <- rf[Rs1]
    JMP, // pc <- pc + Const
    BR, // pc <- rf[Rs1] + Const
    CMP, // rf[Rd] <- -1 if(rf[Rs1] < rf[Rs2]), 0 if(rf[Rs1] == rf[Rs2]), 1 if(rf[Rs1] > rf[Rs2])
    BRZ, // pc <- Const if (rf[Rs1] == 0)
    BRN, // pc <- Const if (rf[Rs1] < 0)
    HALT // Terminate processor
}
