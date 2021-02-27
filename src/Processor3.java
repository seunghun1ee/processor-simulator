public class Processor3 {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[64]; //Register file register 0 always have value zero (input is ignored) 32 ARF 64 PRF
    Instruction[] instructions;
    boolean finished = false;
    Instruction fetched = new Instruction(Opcode.NOOP,0,0,0,0);
    Instruction decoded = new Instruction(Opcode.NOOP,0,0,0,0);
    Instruction executing = new Instruction(Opcode.NOOP,0,0,0,0);
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean executeBlocked = false;
    int executeCycle = 0;


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
    }

    private void Decode() {
        // if execute is processing previous instruction more than 1 cycle
        // decode input is blocked
        decodeBlocked = executeCycle > 1;
        if(!decodeBlocked) {
            if(fetched == null) {
                decoded = new Instruction();
                decoded.opcode = Opcode.NOOP;
            }
            else {
                decoded = fetched;
            }
        }
        cycle++;
    }

    private void Execute() {
        if(!executeBlocked) { // if execute input is not blocked, update executing instruction
            executing = decoded;
            executeCycle = 0;
        }
        if(executeCycle < executing.numCycles - 1) {
            executeBlocked = true;
        }
        else {
            finishExecution(executing);
            executeBlocked = false;
        }
        executeCycle++;
        cycle++;
    }

    private void finishExecution(Instruction ins) {
        if(ins.Rd != 0) { // register 0 is read-only
            switch (ins.opcode) {
                case ADD:
                    rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                    break;
                case ADDI:
                    rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                    break;
                case SUB:
                    rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                    break;
                case MUL:
                    rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                    break;
                case MULI:
                    rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                    break;
                case DIV:
                    rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                    break;
                case DIVI:
                    rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                    break;
                case NOT:
                    rf[ins.Rd] = ~rf[ins.Rs1];
                    break;
                case AND:
                    rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                    break;
                case OR:
                    rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                    break;
                case LD:
                    rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                    break;
                case LDC:
                    rf[ins.Rd] = ins.Const;
                    break;
                case LDI:
                    rf[ins.Rd] = mem[ins.Const];
                    break;
                case LDO:
                    rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                    break;
                case MV:
                    rf[ins.Rd] = rf[ins.Rs1];
                    break;
                case CMP:
                    rf[ins.Rd] = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                    break;
                case NOOP:
                default:
                    break;
            }
        }

        switch (ins.opcode) { // instructions that are safe with gpr[0]
            case ST:
                mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rd];
                break;
            case STO:
                mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                break;
            case BR:
                pc = ins.Const;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JMP:
                pc = pc + ins.Const - 2; // By the time JMP is executed, pc is already incremeted twice
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JR:
                pc = ins.Rs1;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case BEQ:
                if(rf[ins.Rs1] == rf[ins.Rs2]) {
                    pc = ins.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                break;
            case BLT:
                if(rf[ins.Rs1] < rf[ins.Rs2]) {
                    pc = ins.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                break;
            case HALT:
                finished = true;
                break;
            case NOOP:
            default:
                break;
        }

        executedInsts++;

    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            //System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Execute();
            Decode();
            Fetch();
        }
        System.out.println("3 cycle scalar pipelined processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
    }

}
