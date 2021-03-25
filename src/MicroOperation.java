public class MicroOperation { // Processor4
    public Opcode opcode = Opcode.NOOP;
    public int Rd = 0;
    public int input1 = 0;
    public int input2 = 0;
    public int input3 = 0;

    public MicroOperation() {
    }

    public MicroOperation(Opcode opcode, int rd, int input1, int input2) {
        this.opcode = opcode;
        Rd = rd;
        this.input1 = input1;
        this.input2 = input2;
    }

    public MicroOperation(Opcode opcode, int rd, int input1, int input2, int input3) {
        this.opcode = opcode;
        Rd = rd;
        this.input1 = input1;
        this.input2 = input2;
        this.input3 = input3;
    }
}
