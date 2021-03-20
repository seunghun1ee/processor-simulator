public class LSU extends ExecutionUnit{
    private final ALU agu;

    public LSU() {
        this.agu = new ALU();
    }

    @Override
    public Integer evaluate(Opcode opcode, Integer input1, Integer input2) {
        switch (opcode) {
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
