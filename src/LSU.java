public class LSU extends ExecutionUnit{
    private final ALU agu;

    public LSU() {
        this.agu = new ALU();
    }

    public Instruction execute() {
        busy = true;
        if(unitCycles < opcodeCycle.getOpCycle(op) - 1) {
            unitCycles++;
            return null;
        }
        else {
            busy = false;
            unitCycles = 0;
            executing.memAddress = evaluate(op,input1,input2);
            return executing;
        }
    }

    @Override
    public Integer evaluate(Opcode opcode, Integer input1, Integer input2) {
        switch (opcode) {
            case LD:
            case LDO:
            case ST:
            case STO:
                return agu.evaluate(Opcode.ADD,input1,input2);
            default:
                return null;
        }
    }
}
