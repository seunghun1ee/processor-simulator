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
    // 3 pipeline registers
    Instruction fetched = null;
    Instruction decoded = null;
    Instruction executing = null;

    Opcode memoryOpcode = null;
    Integer memoryRd = null;
    Integer memoryAddress = null;

    Integer resultData_ex = null;
    Integer resultAddress_ex = null;
    Integer resultData_mem = null;
    Integer resultAddress_mem = null;
    Integer writeBackData = null;
    Integer writeBackLoc = null;
    // state of phases
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean executeBlocked = false;



    public Processor3() {

    }
    //This will fetch int instead later
    private void Fetch() {
        if(decodeBlocked && executeCycle > 1) { // if decode input is blocked and execute is processed more than 1 cycle
            fetchBlocked = true; // fetch input is blocked
        }
        else if(pc < mem.length) {
            fetchBlocked = false;
            fetched = instructions[pc];
            pc++;
        }
        cycle++;
        if(fetched == null) {
            stalledCycle++;
        }
    }

    private void Decode() {
        // if execute is processing previous instruction more than 1 cycle
        // decode input is blocked
        decodeBlocked = executeCycle > 1;
        if(!decodeBlocked) {
            decoded = fetched;
        }
        cycle++;
        if(decoded == null) {
            stalledCycle++;
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
                finishExecution(executing);
                executeBlocked = false;
            }
        }
        else {
            stalledCycle++; // when executing is null, it's stall
        }
        executeCycle++;
        cycle++;
    }

    private void Memory() {
        if(memoryOpcode != null && memoryRd != null && memoryAddress != null) {
            switch (memoryOpcode) {
                case LD:
                case LDI:
                case LDO:
                    resultData_mem = mem[memoryAddress];
                    resultAddress_mem = memoryRd;
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
        cycle++;
    }

    private void WriteBack() {
        writeBackData = resultData_mem;
        writeBackLoc = resultAddress_mem;
        if(resultData_mem != null && resultAddress_mem != null) {
            rf[writeBackLoc] = writeBackData;
            resultAddress_mem = null;
            resultData_mem = null;
        }
        writeBackData = resultData_ex;
        writeBackLoc = resultAddress_ex;
        if(resultData_ex != null && resultAddress_ex != null) {
            rf[writeBackLoc] = writeBackData;
            resultAddress_ex = null;
            resultData_ex = null;
        }

        cycle++;
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

    private void finishExecution(Instruction ins) {
        if(ins.Rd != 0 && ins.Rd != 32) { // register 0 & 32 is read-only ($zero, $pc)
            switch (ins.opcode) {
                case ADD:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] + rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                    rf[32]++;
                    break;
                case ADDI:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] + ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                    rf[32]++;
                    break;
                case SUB:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] - rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MUL:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] * rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MULI:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] * ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                    rf[32]++;
                    break;
                case DIV:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] / rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                    rf[32]++;
                    break;
                case DIVI:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] / ins.Const;
                    //rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                    rf[32]++;
                    break;
                case NOT:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = ~rf[ins.Rs1];
                    //rf[ins.Rd] = ~rf[ins.Rs1];
                    rf[32]++;
                    break;
                case AND:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] & rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                    rf[32]++;
                    break;
                case OR:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1] | rf[ins.Rs2];
                    //rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                    rf[32]++;
                    break;
                case LD:
                    //rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                    memoryOpcode = ins.opcode;
                    memoryRd = ins.Rd;
                    memoryAddress = rf[ins.Rs1] + rf[ins.Rs2];
                    rf[32]++;
                    break;
                case LDC:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = ins.Const;
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
                    memoryAddress = rf[ins.Rs1] + ins.Const;
                    rf[32]++;
                    break;
                case MV:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = rf[ins.Rs1];
                    //rf[ins.Rd] = rf[ins.Rs1];
                    rf[32]++;
                    break;
                case CMP:
                    resultAddress_ex = ins.Rd;
                    resultData_ex = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
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
                memoryAddress = rf[ins.Rs1] + rf[ins.Rs2];
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
                memoryAddress = rf[ins.Rs1] + ins.Const;
                rf[32]++;
                break;
            case BR:
                rf[32] = pc = ins.Const;
                fetched = null;
                break;
            case JMP:
                rf[32] = pc = rf[32] + ins.Const; // By the time JMP is executed, pc is already incremeted twice
                fetched = null;
                break;
            case JR:
                rf[32] = pc = rf[ins.Rs1] + ins.Const;
                fetched = null;
                break;
            case BEQ:
                if(rf[ins.Rs1] == rf[ins.Rs2]) {
                    rf[32] = pc = ins.Const;
                    fetched = null;
                }
                else {
                    rf[32]++;
                }
                break;
            case BLT:
                if(rf[ins.Rs1] < rf[ins.Rs2]) {
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
            Decode();
            Fetch();
        }
        System.out.println("Scalar pipelined (3-way) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }

}
