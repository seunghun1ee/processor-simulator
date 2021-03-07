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

    Integer Rd = null;
    Integer Rs1 = null;
    Integer Rs2 = null;
    Integer Const = null;
    Instruction executing = null;

    Integer resultData = null;
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

    }

    private void WriteBack() {
        if(resultData != null) {

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

    private void finishExecution(Instruction ins) {
        if(ins.Rd != 0 && ins.Rd != 32) { // register 0 & 32 is read-only ($zero, $pc)
            switch (ins.opcode) {
                case ADD:
                    rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                    rf[32]++;
                    break;
                case ADDI:
                    rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                    rf[32]++;
                    break;
                case SUB:
                    rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MUL:
                    rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                    rf[32]++;
                    break;
                case MULI:
                    rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                    rf[32]++;
                    break;
                case DIV:
                    rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                    rf[32]++;
                    break;
                case DIVI:
                    rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                    rf[32]++;
                    break;
                case NOT:
                    rf[ins.Rd] = ~rf[ins.Rs1];
                    rf[32]++;
                    break;
                case AND:
                    rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                    rf[32]++;
                    break;
                case OR:
                    rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                    rf[32]++;
                    break;
                case LD:
                    rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                    rf[32]++;
                    break;
                case LDC:
                    rf[ins.Rd] = ins.Const;
                    rf[32]++;
                    break;
                case LDI:
                    rf[ins.Rd] = mem[ins.Const];
                    rf[32]++;
                    break;
                case LDO:
                    rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                    rf[32]++;
                    break;
                case MV:
                    rf[ins.Rd] = rf[ins.Rs1];
                    rf[32]++;
                    break;
                case CMP:
                    rf[ins.Rd] = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                    rf[32]++;
                    break;
                default:
                    break;
            }
        }

        switch (ins.opcode) { // instructions that are safe with gpr[0]
            case ST:
                mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                rf[32]++;
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rd];
                rf[32]++;
                break;
            case STO:
                mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
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
