public class ReorderBuffer {

    public boolean ready = false;
    public Instruction ins = new Instruction();
    public int destination;
    public int value;
    public int address;

    public ReorderBuffer() {

    }

}
