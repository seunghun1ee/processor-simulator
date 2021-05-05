public class BRU2 extends ExecutionUnit2{

    public BRU2() {

    }

    public boolean evaluateCondition() {
        switch (this.executing.opcode){
            case JMP:
            case BR:
            case BRR:
                return true;
            case BRZ:
                return this.executing.data1 == 0;
            case BRN:
                return this.executing.data1 < 0;
            default:
                return false;
        }
    }

}
