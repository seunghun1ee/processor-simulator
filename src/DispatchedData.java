public class DispatchedData {
    Opcode opcode;
    int D1;
    int D2;
    int Const;

    public DispatchedData() {
        this.opcode = Opcode.NOOP;
        this.D1 = 0;
        this.D2 = 0;
        this.Const = 0;
    }

    public DispatchedData(Opcode opcode, int D1, int D2, int Const) {
        this.opcode = opcode;
        this.D1 = D1;
        this.D2 = D2;
        this.Const = Const;
    }
}
