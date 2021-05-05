public class OpcodeCycle {

    public OpcodeCycle() {

    }

    public int getOpCycle(Opcode op) {
        int cycle = 1;
        switch (op) {
            case MUL:
            case MULI:
            case LD:
            case LDI:
            case ST:
            case STI:
                cycle = 2;
                break;
            case DIV:
            case DIVI:
                cycle = 16;  //change it to 16 later
                break;
            default:
                break;
        }
        return cycle;
    }
}
