public class LSU {

    private final ALU agu;

    public LSU() {
        this.agu = new ALU();
    }

    public Integer evaluate(Opcode op, int input1, int input2) {
        switch (op) {
            case LD:
            case LDO:
            case ST:
            case STO:
                return agu.evaluate(Opcode.ADD,input1,input2);
            case LDI:
            case STI:
                return agu.evaluate(Opcode.MOV,input1,0);
            default:
                return null;
        }
    }
}
