public class Instruction {
    Opcode opcode = Opcode.NOOP;
    Integer Rd = 0; //Destination register
    Integer Rs1 = 0; //Source register 1
    Integer Rs2 = 0; //Source register 2
    Integer Const = 0; //Constant for immediate operations

    // data from source registers
    Integer data1 = null;
    Integer data2 = null;

    // exec result
    Integer result = null;
    Integer memAddress = null; // if this instruction is memory related

    // info for debugging
    int id = 0;
    int insAddress = 0;
    int fetchComplete = 0;
    int decodeComplete = 0;
    int issueComplete = 0;
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

    public Instruction(Instruction ins) {
        this.opcode = ins.opcode;
        this.Rd = ins.Rd;
        this.Rs1 = ins.Rs1;
        this.Rs2 = ins.Rs2;
        this.Const = ins.Const;
        this.data1 = ins.data1;
        this.data2 = ins.data2;
        this.result = ins.result;
        this.memAddress = ins.memAddress;
        this.id = ins.id;
        this.insAddress = ins.insAddress;
        this.fetchComplete = ins.fetchComplete;
        this.decodeComplete = ins.decodeComplete;
        this.issueComplete = ins.issueComplete;
        this.executeComplete = ins.executeComplete;
        this.memoryComplete = ins.memoryComplete;
        this.writeBackComplete = ins.writeBackComplete;
    }

}
