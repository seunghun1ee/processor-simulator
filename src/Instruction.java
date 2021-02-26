public class Instruction {
    Opcode opcode = null;
    Integer Rd = null; //Destination register
    Integer Rs1 = null; //Source register 1
    Integer Rs2 = null; //Source register 2
    Integer Const = null; //Constant for immediate operations
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
