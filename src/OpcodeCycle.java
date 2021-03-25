public class OpcodeCycle {

    public OpcodeCycle() {

    }

    public int getOpCycle(Opcode op) {
        int cycle = 1;
        switch (op) {
            case MUL:
            case MULI:
            case LD:
            case LDO:
            case LDI:
            case ST:
            case STO:
            case STI:
                cycle = 2;
                break;
            case DIV:
            case DIVI:
                cycle = 6;  //change it to 16 later
                break;
            default:
                break;
        }
        return cycle;
    }
}
