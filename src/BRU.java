public class BRU extends ExecutionUnit {

    public BRU() {

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
