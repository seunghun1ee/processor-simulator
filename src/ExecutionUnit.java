public class ExecutionUnit {

    public boolean busy = false;
    public boolean ready = false;
    public Instruction executing = new Instruction();
    public int destination = -1;
    public int unitCycles = 0;
    public final OpcodeCycle opcodeCycle = new OpcodeCycle();

    public void update(Instruction ins) {
        this.executing = ins;
        this.destination = ins.rsIndex;
        this.ready = true;
    }

    public void reset() {
        this.busy = false;
        this.ready = false;
        this.executing = new Instruction();
        this.destination = -1;
        this.unitCycles = 0;
    }
}
