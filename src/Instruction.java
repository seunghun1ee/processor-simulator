public class Instruction {
    Opcode opcode = Opcode.NOOP;
    Integer Rd = 0; //Destination register
    Integer Rs1 = 0; //Source register 1
    Integer Rs2 = 0; //Source register 2
    Integer Const = 0; //Constant for immediate operations

    public Instruction() {

    }

    public Instruction(Opcode opcode, Integer Rd, Integer Rs1, Integer Rs2, Integer Const) {
        this.opcode = opcode;
        this.Rd = Rd;
        this.Rs1 = Rs1;
        this.Rs2 = Rs2;
        this.Const = Const;
    }

}
