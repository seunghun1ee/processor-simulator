public class ReorderBuffer {

    public boolean busy = false;
    public boolean ready = false;
    public Instruction ins = new Instruction();
    public int state;
    public int destination;
    public int value;
    public int address;

    public ReorderBuffer() {

    }

}