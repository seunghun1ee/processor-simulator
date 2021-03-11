public class Processor3 {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int executeCycle = 0; // cycles that was spent at execution phase
    int stalledCycle = 0; // cycles that was spent while doing nothing
    int[] mem; // memory from user
    int[] rf = new int[65]; //Register file (physical)
    // register 0 always have value zero ($zero, input is ignored)
    // $32 is Program counter for users ($pc)
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    // pipeline registers
    Instruction fetched = null;
    Instruction decoded = null;
    Instruction executing = null;
    // execution result registers
    Integer executedData = null;
    Integer executedAddress = null;
    Opcode memoryOpcode = null;
    Integer memoryRd = null;
    Integer memoryAddress = null;
    // final result registers before write back
    Integer resultData = null;
    Integer resultAddress = null;
    // state of phases
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean executeBlocked = false;


    public Processor3(int[] mem, Instruction[] instructions) {
        this.mem = mem;
        this.instructions = instructions;
    }

    private void Fetch() {
        if(decodeBlocked && executeCycle > 1) { // if decode input is blocked and execute is processed more than 1 cycle
            fetchBlocked = true; // fetch input is blocked
        }
        else if(pc < mem.length) {
            fetchBlocked = false;
            fetched = instructions[pc];
            pc++;
        }
    }

    private void Dispatch() {

    }

    private void Decode() {
        // if execute is processing previous instruction more than 1 cycle
        // decode input is blocked
        decodeBlocked = executeCycle > 1;
        if(!decodeBlocked) {
            decoded = fetched;
        }
    }

    private void Execute() {
        if(!executeBlocked) { // if execute input is not blocked, update executing instruction
            executing = decoded;
            executeCycle = 0;
        }
        if(executing != null) {
            if(executeCycle < getInstCycle(executing) - 1) {
                executeBlocked = true;
            }
            else {
                switch (executing.opcode) {
                    case ADD:
                    case ADDI:
                    case SUB:
                    case MUL:
                    case MULI:
                    case DIV:
                    case DIVI:
                    case NOT:
                    case AND:
                    case OR:
                    case MOV:
                    case CMP:
                        ALU_exe(executing);
                        break;
                    case LD:
                    case LDI:
                    case LDO:
                    case ST:
                    case STI:
                    case STO:
                        loadStore_exe(executing);
                        break;
                    default:
                        finishExecution(executing);
                        break;
                }

                executeBlocked = false;
            }
        }
        executeCycle++;
    }

    private void Memory() {
        if(memoryOpcode != null && memoryRd != null && memoryAddress != null) {
            switch (memoryOpcode) {
                case LD:
                case LDI:
                case LDO:
                    resultData = mem[memoryAddress];
                    resultAddress = memoryRd;
                    memoryOpcode = null;
                    memoryRd = null;
                    memoryAddress = null;
                    break;
                case ST:
                case STI:
                case STO:
                    mem[memoryAddress] = rf[memoryRd];
                    memoryOpcode = null;
                    memoryRd = null;
                    memoryAddress = null;
                    break;
                default:
                    break;
            }
        }
        else {
            resultData = executedData;
            resultAddress = executedAddress;
            executedData = null;
            executedAddress = null;
        }
    }

    private void WriteBack() {
        if(resultData != null && resultAddress != null) {
            rf[resultAddress] = resultData;
            resultAddress = null;
            resultData = null;
        }
    }

    private int getInstCycle(Instruction ins) {
        int cycle = 1;
        switch (ins.opcode) {
            case ADD:
            case ADDI:
            case SUB:
                cycle = 2;
                break;
            case MUL:
            case MULI:
                cycle = 3;
                break;
            case DIV:
            case DIVI:
                cycle = 4;
                break;
            default:
                break;
        }
        return cycle;
    }

    private void ALU_exe(Instruction ins) {
        int source1 = rf[ins.Rs1];
        int source2 = rf[ins.Rs2];
        // Result forwarding from memory stage
        if(resultAddress != null && resultAddress.equals(ins.Rs1)) {
            source1 = resultData;
        }
        if(resultAddress != null && resultAddress.equals(ins.Rs2)) {
            source2 = resultData;
        }
        // Result forwarding from execute stage
        if(executedAddress != null && executedAddress.equals(ins.Rs1)) {
            source1 = executedData;
        }
        if(executedAddress != null && executedAddress.equals(ins.Rs2)) {
            source2 = executedData;
        }
        if(ins.Rd != 0 && ins.Rd != 32) { // register 0 & 32 is read-only ($zero, $pc)
            switch (ins.opcode) {
                case ADD:
                    executedAddress = ins.Rd;
                    executedData = source1 + source2;
                    //rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                    rf[32]++;
                    break;
                case ADDI:
                    executedAddress = ins.Rd;
                    executedData = source1 + ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                    rf[32]++;
                    break;
                case SUB:
                    executedAddress = ins.Rd;
                    executedData = source1 - source2;
                    //rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MUL:
                    executedAddress = ins.Rd;
                    executedData = source1 * source2;
                    //rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MULI:
                    executedAddress = ins.Rd;
                    executedData = source1 * ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                    rf[32]++;
                    break;
                case DIV:
                    executedAddress = ins.Rd;
                    executedData = source1 / source2;
                    //rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                    rf[32]++;
                    break;
                case DIVI:
                    executedAddress = ins.Rd;
                    executedData = source1 / ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                    rf[32]++;
                    break;
                case NOT:
                    executedAddress = ins.Rd;
                    executedData = ~source1;
                    //rf[ins.Rd] = ~rf[ins.Rs1];
                    rf[32]++;
                    break;
                case AND:
                    executedAddress = ins.Rd;
                    executedData = source1 & source2;
                    //rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                    rf[32]++;
                    break;
                case OR:
                    executedAddress = ins.Rd;
                    executedData = source1 | source2;
                    //rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MOVC:
                    executedAddress = ins.Rd;
                    executedData = ins.Const;
                    //rf[ins.Rd] = ins.Const;
                    rf[32]++;
                    break;
                case MOV:
                    executedAddress = ins.Rd;
                    executedData = source1;
                    //rf[ins.Rd] = rf[ins.Rs1];
                    rf[32]++;
                    break;
                case CMP:
                    executedAddress = ins.Rd;
                    executedData = Integer.compare(source1, source2);
                    //rf[ins.Rd] = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                    rf[32]++;
                    break;
                default:
                    break;
            }
        }
        executedInsts++;
    }

    private void loadStore_exe(Instruction ins) {
        int source1 = rf[ins.Rs1];
        int source2 = rf[ins.Rs2];
        // Result forwarding from memory stage
        if(resultAddress != null && resultAddress.equals(ins.Rs1)) {
            source1 = resultData;
        }
        if(resultAddress != null && resultAddress.equals(ins.Rs2)) {
            source2 = resultData;
        }
        // Result forwarding from execute stage
        if(executedAddress != null && executedAddress.equals(ins.Rs1)) {
            source1 = executedData;
        }
        if(executedAddress != null && executedAddress.equals(ins.Rs2)) {
            source2 = executedData;
        }

        if(ins.Rd != 0 && ins.Rd != 32) { // register 0 & 32 is read-only ($zero, $pc)
            switch (ins.opcode) {
                case LD:
                    //rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = source1 + source2;
                    rf[32]++;
                    break;
                case LDI:
                    //rf[ins.Rd] = mem[ins.Const];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = ins.Const;
                    rf[32]++;
                    break;
                case LDO:
                    //rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = source1 + ins.Const;
                    rf[32]++;
                    break;
                default:
                    break;
            }
        }

        switch (ins.opcode) { // instructions that are safe with gpr[0]
            case ST:
                //mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = source1 + source2;
                rf[32]++;
                break;
            case STI:
                //mem[ins.Const] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = ins.Const;
                rf[32]++;
                break;
            case STO:
                //mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = source1 + ins.Const;
                rf[32]++;
                break;
            default:
                break;
        }
        executedInsts++;
    }

    private void finishExecution(Instruction ins) {
        int source1 = rf[ins.Rs1];
        int source2 = rf[ins.Rs2];
        // Result forwarding from memory stage
        if(resultAddress != null && resultAddress.equals(ins.Rs1)) {
            source1 = resultData;
        }
        if(resultAddress != null && resultAddress.equals(ins.Rs2)) {
            source2 = resultData;
        }
        // Result forwarding from execute stage
        if(executedAddress != null && executedAddress.equals(ins.Rs1)) {
            source1 = executedData;
        }
        if(executedAddress != null && executedAddress.equals(ins.Rs2)) {
            source2 = executedData;
        }

        if(ins.Rd != 0 && ins.Rd != 32) { // register 0 & 32 is read-only ($zero, $pc)
            switch (ins.opcode) {
                case ADD:
                    executedAddress = ins.Rd;
                    executedData = source1 + source2;
                    //rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                    rf[32]++;
                    break;
                case ADDI:
                    executedAddress = ins.Rd;
                    executedData = source1 + ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                    rf[32]++;
                    break;
                case SUB:
                    executedAddress = ins.Rd;
                    executedData = source1 - source2;
                    //rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MUL:
                    executedAddress = ins.Rd;
                    executedData = source1 * source2;
                    //rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MULI:
                    executedAddress = ins.Rd;
                    executedData = source1 * ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                    rf[32]++;
                    break;
                case DIV:
                    executedAddress = ins.Rd;
                    executedData = source1 / source2;
                    //rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                    rf[32]++;
                    break;
                case DIVI:
                    executedAddress = ins.Rd;
                    executedData = source1 / ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                    rf[32]++;
                    break;
                case NOT:
                    executedAddress = ins.Rd;
                    executedData = ~source1;
                    //rf[ins.Rd] = ~rf[ins.Rs1];
                    rf[32]++;
                    break;
                case AND:
                    executedAddress = ins.Rd;
                    executedData = source1 & source2;
                    //rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                    rf[32]++;
                    break;
                case OR:
                    executedAddress = ins.Rd;
                    executedData = source1 | source2;
                    //rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                    rf[32]++;
                    break;
                case LD:
                    //rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = source1 + source2;
                    rf[32]++;
                    break;
                case MOVC:
                    executedAddress = ins.Rd;
                    executedData = ins.Const;
                    //rf[ins.Rd] = ins.Const;
                    rf[32]++;
                    break;
                case LDI:
                    //rf[ins.Rd] = mem[ins.Const];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = ins.Const;
                    rf[32]++;
                    break;
                case LDO:
                    //rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = source1 + ins.Const;
                    rf[32]++;
                    break;
                case MOV:
                    executedAddress = ins.Rd;
                    executedData = source1;
                    //rf[ins.Rd] = rf[ins.Rs1];
                    rf[32]++;
                    break;
                case CMP:
                    executedAddress = ins.Rd;
                    executedData = Integer.compare(source1, source2);
                    //rf[ins.Rd] = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                    rf[32]++;
                    break;
                default:
                    break;
            }
        }

        switch (ins.opcode) { // instructions that are safe with gpr[0]
            case ST:
                //mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = source1 + source2;
                rf[32]++;
                break;
            case STI:
                //mem[ins.Const] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = ins.Const;
                rf[32]++;
                break;
            case STO:
                //mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                memoryOpcode = ins.opcode;
                memoryRd = ins.Rd;
                memoryAddress = source1 + ins.Const;
                rf[32]++;
                break;
            case BR:
                rf[32] = pc = ins.Const;
                fetched = null;
                break;
            case JMP:
                rf[32] = pc = rf[32] + ins.Const;
                fetched = null;
                break;
            case JR:
                rf[32] = pc = source1 + ins.Const;
                fetched = null;
                break;
            case BEQ:
                if(source1 == source2) {
                    rf[32] = pc = ins.Const;
                    fetched = null;
                }
                else {
                    rf[32]++;
                }
                break;
            case BLT:
                if(source1 < source2) {
                    rf[32] = pc = ins.Const;
                    fetched = null;
                }
                else {
                    rf[32]++;
                }
                break;
            case HALT:
                finished = true;
                break;
            case NOOP:
                rf[32]++;
                break;
            default:
                break;
        }
        executedInsts++;
    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            WriteBack();
            Memory();
            Execute();
            Dispatch();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || (executeBlocked && executeCycle > 1)) {
                stalledCycle++;
            }
        }
        WriteBack(); // Writing back last data
        cycle++;
        System.out.println("Scalar pipelined (3-way) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }

}
