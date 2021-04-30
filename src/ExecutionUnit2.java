public class ExecutionUnit2 {

    public boolean busy = false;
    public Instruction executing = new Instruction();
    public int destination = -1;
    public int unitCycles = 0;
    public final OpcodeCycle opcodeCycle = new OpcodeCycle();

    public void update(Instruction ins) {
        this.executing = ins;
        this.destination = ins.rsIndex;
    }

    public void reset() {
        this.busy = false;
        this.executing = new Instruction();
        this.destination = -1;
        this.unitCycles = 0;
    }
}
