public class ReservationStation {
    public boolean busy = false;
    public Opcode op = Opcode.NOOP; // Operation of the instruction
    public int Q1 = -1; // index of dependent ReorderBuffer
    public int Q2 = -1;
    public int V1 = 0; // effective value of operands
    public int V2 = 0;
    public int A = 0; // effective value of target address
    public int Qs = -1; // index of dependent reservation station (store)
    public int Vs = 0; // effective value to store
    public int robIndex = -1; // index of ROB for the instruction
    public Instruction ins = new Instruction();
    public boolean executing = false;
    public boolean addressReady = false;
    public OpType type = OpType.UNDEFINED;

    public ReservationStation() {

    }
}
