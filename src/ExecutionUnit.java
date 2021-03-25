public abstract class ExecutionUnit { // update after Processor4 deprecation

    public boolean busy = false;
    public Instruction executing = new Instruction();
    public Opcode op = Opcode.NOOP;
    public int input1 = 0;
    public int input2 = 0;
    public int unitCycles = 0;
    public final OpcodeCycle opcodeCycle = new OpcodeCycle();

    public void update(Opcode op, int input1, int input2) {
        this.op = op;
        this.input1 = input1;
        this.input2 = input2;
    }

    public void reset() {
        this.op = Opcode.NOOP;
        this.input1 = 0;
        this.input2 = 0;
        executing = new Instruction();
    }

    public Integer execute() {
        busy = true;
        if(unitCycles < opcodeCycle.getOpCycle(op) - 1) {
            unitCycles++;
            return null;
        }
        else {
            busy = false;
            unitCycles = 0;
            return evaluate(op,input1,input2);
        }
    }

    public abstract Integer evaluate(Opcode opcode, Integer input1, Integer input2);
}
