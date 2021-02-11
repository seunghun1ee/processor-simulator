public class Instruction {
    Opcode opcode = null;
    Integer Rd = null; //Destination register
    Integer Rs1 = null; //Source register 1
    Integer Rs2 = null; //Source register 2
    Integer Const = null; //Constant for immediate operations

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
