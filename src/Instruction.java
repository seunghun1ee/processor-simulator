public class Instruction {
    Opcode opcode = Opcode.NOOP;
    Integer Rd = 0; //Destination register
    Integer Rs1 = 0; //Source register 1
    Integer Rs2 = 0; //Source register 2
    Integer Const = 0; //Constant for immediate operations
    Integer numCycles = 1;

    public Instruction() {

    }

    public Instruction(Opcode opcode, Integer Rd, Integer Rs1, Integer Rs2, Integer Const) {
        this.opcode = opcode;
        this.Rd = Rd;
        this.Rs1 = Rs1;
        this.Rs2 = Rs2;
        this.Const = Const;
        this.numCycles = getInstCycle();
    }

    private int getInstCycle() {
        int cycle = 1;
        switch (this.opcode) {
            case ADD:
            case ADDI:
            case SUB:
                cycle = 2;
                break;
            case MUL:
            case MULI:
                cycle = 3;
                break;
            case DIV:
            case DIVI:
                cycle = 4;
                break;
            default:
                break;
        }
        return cycle;
    }
}
