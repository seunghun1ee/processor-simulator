public class Processor3 {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[32]; //Register file (physical) register 0 always have value zero (input is ignored)
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
        fetched = instructions[pc];
        cycle++;
        pc++;
    }

    private void Decode() {
        if(fetched == null) {
            decoded = new Instruction();
            decoded.opcode = Opcode.NOOP;
        }
        else {
            decoded = fetched;
        }
        cycle++;
    }

    private void Execute() {
        switch (decoded.opcode) {
            case ADD:
                rf[decoded.Rd] = rf[decoded.Rs1] + rf[decoded.Rs2];
                cycle += 2;
                break;
            case ADDI:
                rf[decoded.Rd] = rf[decoded.Rs1] + decoded.Const;
                cycle += 2;
                break;
            case SUB:
                rf[decoded.Rd] = rf[decoded.Rs1] - rf[decoded.Rs2];
                cycle += 2;
                break;
            case MUL:
                rf[decoded.Rd] = rf[decoded.Rs1] * rf[decoded.Rs2];
                cycle += 3;
                break;
            case MULI:
                rf[decoded.Rd] = rf[decoded.Rs1] * decoded.Const;
                cycle += 3;
                break;
            case DIV:
                rf[decoded.Rd] = rf[decoded.Rs1] / rf[decoded.Rs2];
                cycle += 4;
                break;
            case DIVI:
                rf[decoded.Rd] = rf[decoded.Rs1] / decoded.Const;
                cycle += 4;
                break;
            case NOT:
                rf[decoded.Rd] = ~rf[decoded.Rs1];
                cycle++;
                break;
            case AND:
                rf[decoded.Rd] = rf[decoded.Rs1] & rf[decoded.Rs2];
                cycle++;
                break;
            case OR:
                rf[decoded.Rd] = rf[decoded.Rs1] | rf[decoded.Rs2];
                cycle++;
                break;
            case MV:
                rf[decoded.Rd] = rf[decoded.Rs1];
                cycle++;
                break;
            case BR:
                pc = decoded.Const;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JMP:
                pc = pc + decoded.Const - 1;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case JR:
                pc = decoded.Rs1;
                cycle++;
                fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                break;
            case BEQ:
                if(rf[decoded.Rs1] == rf[decoded.Rs2]) {
                    pc = decoded.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                cycle++;
                break;
            case BLT:
                if(rf[decoded.Rs1] < rf[decoded.Rs2]) {
                    pc = decoded.Const;
                    fetched = new Instruction(Opcode.NOOP,0,0,0,0);
                }
                cycle++;
                break;
            case CMP:
                rf[decoded.Rd] = Integer.compare(rf[decoded.Rs1], rf[decoded.Rs2]);
                cycle++;
                break;
            case LD:
                if(decoded.Rd != 0) {
                    rf[decoded.Rd] = mem[rf[decoded.Rs1] + rf[decoded.Rs2]];
                }
                cycle++;
                break;
            case LDC:
                if(decoded.Rd != 0) {
                    rf[decoded.Rd] = decoded.Const;
                }
                cycle++;
                break;
            case LDI:
                if(decoded.Rd != 0) {
                    rf[decoded.Rd] = mem[decoded.Const];
                }
                cycle++;
                break;
            case LDO:
                if(decoded.Rd != 0) {
                    rf[decoded.Rd] = mem[rf[decoded.Rs1] + decoded.Const];
                }
                cycle++;
                break;
            case ST:
                mem[rf[decoded.Rs1] + rf[decoded.Rs2]] = rf[decoded.Rd];
                cycle++;
                break;
            case STI:
                mem[decoded.Const] = rf[decoded.Rd];
                cycle++;
                break;
            case STO:
                mem[rf[decoded.Rs1] + decoded.Const] = rf[decoded.Rd];
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
