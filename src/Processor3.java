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

    // Execution units
    ALU alu0 = new ALU();
    ALU alu1 = new ALU();
    LSU lsu0 = new LSU();
    BRU bru0 = new BRU();


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
            if(fetched != null) {
                fetched.insAddress = pc;
            }
            pc++;
        }
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
                int input1 = 0;
                int input2 = 0;
                switch (executing.opcode) {
                    case NOOP:
                        rf[32]++;
                        break;
                    case HALT:
                        finished = true;
                        break;
                    case ADD: // ALU OPs that use rf[Rs1] and rf[Rs2]
                    case SUB:
                    case MUL:
                    case DIV:
                    case CMP:
                    case AND:
                    case OR:
                    case SHL:
                    case SHR:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        input2 = resultForwarding(executing.Rs2,resultData,resultAddress);
                        executedData = alu0.evaluate(executing.opcode, input1, input2);
                        executedAddress = executing.Rd;
                        rf[32]++;
                        break;
                    case ADDI: // ALU OPs that use rf[Rs1] and Const
                    case MULI:
                    case DIVI:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        input2 = executing.Const;
                        executedData = alu0.evaluate(executing.opcode, input1, input2);
                        executedAddress = executing.Rd;
                        rf[32]++;
                        break;
                    case NOT: // ALU OPs that only use rf[Rs1]
                    case MOV:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        executedData = alu0.evaluate(executing.opcode, input1, input2);
                        executedAddress = executing.Rd;
                        rf[32]++;
                        break;
                    case MOVC: // ALU OPs that only use Const
                        input1 = executing.Const;
                        executedData = alu0.evaluate(executing.opcode, input1, input2);
                        executedAddress = executing.Rd;
                        rf[32]++;
                        break;
                    case LD: // rf[Rs1] + rf[Rs2]
                    case ST:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        input2 = resultForwarding(executing.Rs2,resultData,resultAddress);
                        memoryOpcode = executing.opcode;
                        memoryRd = executing.Rd;
                        memoryAddress = lsu0.evaluate(executing.opcode,input1,input2);
                        rf[32]++;
                        break;
                    case LDI: // rf[Rs1] + Const
                    case STI:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        input2 = executing.Const;
                        memoryOpcode = executing.opcode;
                        memoryRd = executing.Rd;
                        memoryAddress = lsu0.evaluate(executing.opcode,input1,input2);
                        rf[32]++;
                        break;
                    case JMP:
                    case BR:
                    case BRZ:
                    case BRN:
                        input1 = resultForwarding(executing.Rs1,resultData,resultAddress);
                        input2 = executing.Const;
                        if(bru0.evaluateCondition(executing.opcode,input1)) {
                            rf[32] = pc = bru0.evaluateTarget(executing.opcode,rf[32],input1,input2);
                            fetched = null;
                        }
                        else {
                            rf[32]++;
                        }
                        break;
                    default:
                        System.out.println("Invalid instruction exception");
                        finished = true;
                        break;
                }
                executeBlocked = false;
                executedInsts++;
            }
        }
        executeCycle++;
    }

    private void Memory() {
        if(memoryOpcode != null && memoryRd != null && memoryAddress != null) {
            switch (memoryOpcode) {
                case LD:
                case LDI:
                    resultData = mem[memoryAddress];
                    resultAddress = memoryRd;
                    memoryOpcode = null;
                    memoryRd = null;
                    memoryAddress = null;
                    break;
                case ST:
                case STI:
                    mem[memoryAddress] = rf[memoryRd];
                    memoryOpcode = null;
                    memoryRd = null;
                    memoryAddress = null;
                    break;
                default:
                    break;
            }
        }
        else { // not a memory instructions, skip
            resultData = executedData;
            resultAddress = executedAddress;
            executedData = null;
            executedAddress = null;
        }
    }

    private void WriteBack() {
        if(resultData != null && resultAddress != null) {
            if(resultAddress != 0 && resultAddress != 32) {
                rf[resultAddress] = resultData;
            }
            resultAddress = null;
            resultData = null;
        }
    }

    private int getInstCycle(Instruction ins) {
        int cycle = 1;
        switch (ins.opcode) {
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
                cycle = 6;  //change it to 16 later
                break;
            default:
                break;
        }
        return cycle;
    }

    private Integer resultForwarding(Integer insAddress, Integer forData, Integer forAddress) {
        if(forAddress != null && forAddress.equals(insAddress)) {
            return forData;
        }
        return rf[insAddress];
    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            WriteBack();
            Memory();
            Execute();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || (executeBlocked && executeCycle > 1)) {
                stalledCycle++;
            }
//            System.out.println("PC: "+ pc + " rf[32]: " + rf[32]);
        }
        WriteBack(); // Writing back last data
        cycle++;
        System.out.println("Scalar pipelined (5-stage) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }

}
