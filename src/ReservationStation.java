public class ReservationStation {
    public boolean busy = false;
    public Opcode op = Opcode.NOOP; // Operation of the instruction
    public int Q1, Q2 = -1; // index of dependent reservation station
    public int V1, V2 = 0; // effective value of operands
    public int A = 0; // effective value of target address
    public int Qs = -1; // index of dependent reservation station (store)
    public int Vs = 0; // effective value to store
    public Instruction ins = new Instruction();
    public boolean executing = false;
    public OpType type = OpType.UNDEFINED;

    public ReservationStation() {

    }
}
