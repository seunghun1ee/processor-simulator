public class BRU {

    public BRU() {

    }

    public boolean evaluateCondition(Opcode op, int input1) {
        switch (op) {
            case JMP:
            case BR:
                return true;
            case BRZ:
                return input1 == 0;
            case BRN:
                return input1 < 0;
            default:
                return false;
        }
    }

    public Integer evaluateTarget(Opcode op, int currentPC, int input1, int input2) {
        switch (op) {
            case JMP:
                return currentPC + input2;
            case BR:
                return input1 + input2;
            case BRZ:
                if(input1 == 0) {
                    return input2;
                }
                else {
                    return currentPC;
                }
            case BRN:
                if(input1 < 0) {
                    return input2;
                }
                else {
                    return currentPC;
                }
            default:
                return null;
        }
    }

}
