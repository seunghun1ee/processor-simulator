public class Instruction {
    Opcode opcode = Opcode.NOOP;
    Integer Rd = 0; //Destination register
    Integer Rs1 = 0; //Source register 1
    Integer Rs2 = 0; //Source register 2
    Integer Const = 0; //Constant for immediate operations

    // data from source registers
    Integer data1 = null;
    Integer data2 = null;

    // info for debugging
    int id = 0;
    int fetchComplete = 0;
    int decodeComplete = 0;
    int issueComplete = 0;
    int dispatchComplete = 0;
    int executeComplete = 0;
    int memoryComplete = 0;
    int writeBackComplete = 0;

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
