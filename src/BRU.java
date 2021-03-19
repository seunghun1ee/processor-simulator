public class BRU {

    public BRU() {

    }

    public Integer evaluateTarget(Opcode op, int currentPC, int input1, int input2, int input3) {
        switch (op) {
            case BR:
                return input3;
            case JMP:
                return currentPC + input3;
            case JR:
                return input1 + input3;
            case BEQ:
                if(input1 == input2) {
                    return input3;
                }
                else {
                    return currentPC;
                }
            case BLT:
                if(input1 < input2) {
                    return input3;
                }
                else {
                    return currentPC;
                }
            default:
                return null;
        }
    }

    public boolean evaluateCondition(Opcode op, int input1, int input2) {
        switch (op) {
            case BR:
            case JMP:
            case JR:
                return true;
            case BEQ:
                return input1 == input2;
            case BLT:
                return input1 < input2;
            default:
                return false;
        }
    }
}
