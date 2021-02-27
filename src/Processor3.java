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
    int execEndCycle = 0;


    public Processor3() {

    }
    //This will fetch int instead later
    private void Fetch() {
        if(decodeBlocked && executeCycle > 1) { // if decode input is blocked and execute is processed more than 1 cycle
            fetchBlocked = true; // fetch input is blocked
        }
        else {
            fetchBlocked = false;
            fetched = instructions[pc];
            pc++;
        }
        cycle++;

    }

    private void Decode() {
        if(executeCycle > 1) { // if execute is processing previous instruction more than 1 cycle
            decodeBlocked = true; // decode input is blocked
        }
        else { // getting overwritten too fast for MUL
            decodeBlocked = false;

        }

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

    private void Decode2() {
        if(cycle < execEndCycle) { // if execute is processing previous instruction more than 1 cycle
            decodeBlocked = true; // decode input is blocked
        }
        else { // getting overwritten too fast for MUL
            decodeBlocked = false;

        }

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
        if(!executeBlocked) { // if execute input is not block, update executing instruction
            executing = decoded;
        }
        switch (executing.opcode) {
            case ADD:
                if(executeCycle < 1) {
                    executeBlocked = true;
                    executeCycle++;
                }
                else {
                    rf[executing.Rd] = rf[executing.Rs1] + rf[executing.Rs2];
                    executeBlocked = false;
                    executeCycle = 0;
                }
                cycle++;
                break;
            case ADDI:
                rf[executing.Rd] = rf[executing.Rs1] + executing.Const;
                cycle += 2;
                break;
            case SUB:
                rf[executing.Rd] = rf[executing.Rs1] - rf[executing.Rs2];
                cycle += 2;
                break;
            case MUL:
                rf[executing.Rd] = rf[executing.Rs1] * rf[executing.Rs2];
                cycle += 3;
                break;
            case MULI:
                rf[executing.Rd] = rf[executing.Rs1] * executing.Const;
                cycle += 3;
                break;
            case DIV:
                rf[executing.Rd] = rf[executing.Rs1] / rf[executing.Rs2];
                cycle += 4;
                break;
            case DIVI:
                rf[executing.Rd] = rf[executing.Rs1] / executing.Const;
                cycle += 4;
                break;
            case NOT:
                rf[executing.Rd] = ~rf[executing.Rs1];
                cycle++;
                break;
            case AND:
                rf[executing.Rd] = rf[executing.Rs1] & rf[executing.Rs2];
                cycle++;
                break;
            case OR:
                rf[executing.Rd] = rf[executing.Rs1] | rf[executing.Rs2];
                cycle++;
                break;
            case MV:
                rf[executing.Rd] = rf[executing.Rs1];
                cycle++;
                break;
            case BR:
                pc = executing.Const;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JMP:
                pc = pc + executing.Const - 1;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JR:
                pc = executing.Rs1;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case BEQ:
                if(rf[executing.Rs1] == rf[executing.Rs2]) {
                    pc = executing.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                cycle++;
                break;
            case BLT:
                if(rf[executing.Rs1] < rf[executing.Rs2]) {
                    pc = executing.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                cycle++;
                break;
            case CMP:
                rf[executing.Rd] = Integer.compare(rf[executing.Rs1], rf[executing.Rs2]);
                cycle++;
                break;
            case LD:
                if(executing.Rd != 0) {
                    rf[executing.Rd] = mem[rf[executing.Rs1] + rf[executing.Rs2]];
                }
                cycle++;
                break;
            case LDC:
                if(executing.Rd != 0) {
                    rf[executing.Rd] = executing.Const;
                }
                cycle++;
                break;
            case LDI:
                if(executing.Rd != 0) {
                    rf[executing.Rd] = mem[executing.Const];
                }
                cycle++;
                break;
            case LDO:
                if(executing.Rd != 0) {
                    rf[executing.Rd] = mem[rf[executing.Rs1] + executing.Const];
                }
                cycle++;
                break;
            case ST:
                mem[rf[executing.Rs1] + rf[executing.Rs2]] = rf[executing.Rd];
                cycle++;
                break;
            case STI:
                mem[executing.Const] = rf[executing.Rd];
                cycle++;
                break;
            case STO:
                mem[rf[executing.Rs1] + executing.Const] = rf[executing.Rd];
                cycle++;
                break;
            case HALT:
                finished = true;
                cycle++;
                break;
            case NOOP:
            default:
                cycle++;
                break;
        }
        executedInsts++;
    }

    private void Execute2() {
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

    private void Execute3() {
        if(!executeBlocked) {
            executing = decoded;
            execEndCycle = cycle + executing.numCycles;
        }
        if(cycle < execEndCycle - 1) {
            executeBlocked = true;
        }
        else {
            finishExecution(executing);
            executeBlocked = false;
        }
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
                pc = pc + ins.Const - 1;
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



    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Execute2();
            Decode();
            Fetch();
        }
        System.out.println("Terminated");
    }

}
