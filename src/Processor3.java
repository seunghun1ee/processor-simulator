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
        else {
            fetchBlocked = false;
            fetched = instructions[pc];
            pc++;
        }
        cycle++;

    }

    private void Decode() {
        if(executeBlocked && executeCycle > 1) { // if execute is processing previous instruction more than 1 cycle
            decodeBlocked = true; // decode input is blocked
        }
        else {
            decodeBlocked = false;
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

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Execute();
            Decode();
            Fetch();
        }
        System.out.println("Terminated");
    }

}
