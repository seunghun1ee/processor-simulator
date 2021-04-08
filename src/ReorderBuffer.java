public class ReorderBuffer {

    public boolean busy = false;
    public Instruction ins = new Instruction();
    public int state;
    public int destination;
    public int value;

    public ReorderBuffer() {

    }
}
