public class ALU extends ExecutionUnit {

    public ALU() {

    }

    public Instruction execute() {
        busy = true;
        if(unitCycles < opcodeCycle.getOpCycle(op) - 1) {
            unitCycles++;
            return null;
        }
        else {
            busy = false;
            unitCycles = 0;
            executing.result = evaluate(op,input1,input2);
            return executing;
        }
    }

    @Override
    public Integer evaluate(Opcode opcode, Integer input1, Integer input2) {
        switch (opcode) {
            case MOV:
            case MOVC:
                return input1;
            case ADD:
            case ADDI:
                return input1 + input2;
            case SUB:
                return input1 - input2;
            // Maybe mul and div to different execution unit?
            case MUL:
            case MULI:
                return input1 * input2;
            case DIV:
            case DIVI:
                return input1 / input2;
            case SHL:
                return input1 << input2;
            case SHR:
                return input1 >> input2;
            case NOT:
                return ~input1;
            case AND:
                return input1 & input2;
            case OR:
                return input1 | input2;
            case CMP:
                return Integer.compare(input1,input2);
            default:
                return null;
        }
    }
}
