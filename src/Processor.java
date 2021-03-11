public class Processor {

    int cycle = 0;
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[65]; //Register file (physical)
    // register 0 always have value zero (input is ignored)
    // $32 is Program counter ($pc)
    Instruction[] instructions;
    boolean finished = false;

    public Processor() {

    }
    //This will fetch int instead later
    public Instruction Fetch() {
        Instruction instruction = instructions[rf[32]];
        cycle++;
        return instruction;
    }

    public Instruction Decode(Instruction ins) {
        if(ins == null) {
            ins = new Instruction();
            ins.opcode = Opcode.NOOP;
        }
        cycle++;
        return ins;
    }

    public void Execute(Instruction ins) {
        switch (ins.opcode) {
            case NOOP:
                cycle++;
                rf[32]++;
                break;
            case ADD:
                rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                cycle += 2;
                rf[32]++;
                break;
            case ADDI:
                rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                cycle += 2;
                rf[32]++;
                break;
            case SUB:
                rf[ins.Rd] = rf[ins.Rs1] - rf[ins.Rs2];
                cycle += 2;
                rf[32]++;
                break;
            case MUL:
                rf[ins.Rd] = rf[ins.Rs1] * rf[ins.Rs2];
                cycle += 3;
                rf[32]++;
                break;
            case MULI:
                rf[ins.Rd] = rf[ins.Rs1] * ins.Const;
                cycle += 3;
                rf[32]++;
                break;
            case DIV:
                rf[ins.Rd] = rf[ins.Rs1] / rf[ins.Rs2];
                cycle += 4;
                rf[32]++;
                break;
            case DIVI:
                rf[ins.Rd] = rf[ins.Rs1] / ins.Const;
                cycle += 4;
                rf[32]++;
                break;
            case NOT:
                rf[ins.Rd] = ~rf[ins.Rs1];
                cycle++;
                rf[32]++;
                break;
            case AND:
                rf[ins.Rd] = rf[ins.Rs1] & rf[ins.Rs2];
                cycle++;
                rf[32]++;
                break;
            case OR:
                rf[ins.Rd] = rf[ins.Rs1] | rf[ins.Rs2];
                cycle++;
                rf[32]++;
                break;
            case LD:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                }
                cycle++;
                rf[32]++;
                break;
            case MOVC:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = ins.Const;
                }
                cycle++;
                rf[32]++;
                break;
            case LDI:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[ins.Const];
                }
                cycle++;
                rf[32]++;
                break;
            case LDO:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                }
                cycle++;
                rf[32]++;
                break;
            case ST:
                mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                cycle++;
                rf[32]++;
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rd];
                cycle++;
                rf[32]++;
                break;
            case STO:
                mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                cycle++;
                rf[32]++;
                break;
            case MOV:
                rf[ins.Rd] = rf[ins.Rs1];
                cycle++;
                rf[32]++;
                break;
            case BR:
                rf[32] = ins.Const;
                cycle++;
                break;
            case JMP:
                rf[32] = rf[32] + ins.Const;
                cycle++;
                break;
            case JR:
                rf[32] = rf[ins.Rs1];
                cycle++;
                break;
            case BEQ:
                if(rf[ins.Rs1] == rf[ins.Rs2]) {
                    rf[32] = ins.Const;
                }
                else {
                    rf[32]++;
                }
                cycle++;
                break;
            case BLT:
                if(rf[ins.Rs1] < rf[ins.Rs2]) {
                    rf[32] = ins.Const;
                }
                else {
                    rf[32]++;
                }
                cycle++;
                break;
            case CMP:
                rf[ins.Rd] = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                cycle++;
                rf[32]++;
                break;
            case HALT:
                finished = true;
                cycle++;
                break;
            default:
                cycle++;
                break;
        }

    }

    public void RunProcessor() {

        while(!finished && rf[32] < instructions.length) {
            //System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Execute(instruction);
            executedInsts++;
        }
        System.out.println("Scalar non-pipelined (3-way) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");

        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));

    }

}
