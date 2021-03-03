public class Processor2 {

    int cycle = 0;
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[65]; //Register file (physical)
    // register 0 always have value zero (input is ignored)
    // $32 is Program counter ($pc)
    Instruction[] instructions;
    boolean finished = false;

    public Processor2() {

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

    public Integer Execute(Instruction ins) {
        Integer data = null;
        switch (ins.opcode) {
            case NOOP:
            case LD:
            case LDC:
            case LDI:
            case LDO:
            case ST:
            case STI:
            case STO:
                cycle++;
                rf[32]++;
                break;
            case ADD:
                data = rf[ins.Rs1] + rf[ins.Rs2];
                cycle += 2;
                rf[32]++;
                break;
            case ADDI:
                data = rf[ins.Rs1] + ins.Const;
                cycle += 2;
                rf[32]++;
                break;
            case SUB:
                data = rf[ins.Rs1] - rf[ins.Rs2];
                cycle += 2;
                rf[32]++;
                break;
            case MUL:
                data = rf[ins.Rs1] * rf[ins.Rs2];
                cycle += 3;
                rf[32]++;
                break;
            case MULI:
                data = rf[ins.Rs1] * ins.Const;
                cycle += 3;
                rf[32]++;
                break;
            case DIV:
                data = rf[ins.Rs1] / rf[ins.Rs2];
                cycle += 4;
                rf[32]++;
                break;
            case DIVI:
                data = rf[ins.Rs1] / ins.Const;
                cycle += 4;
                rf[32]++;
                break;
            case NOT:
                data = ~rf[ins.Rs1];
                cycle++;
                rf[32]++;
                break;
            case AND:
                data = rf[ins.Rs1] & rf[ins.Rs2];
                cycle++;
                rf[32]++;
                break;
            case OR:
                data = rf[ins.Rs1] | rf[ins.Rs2];
                cycle++;
                rf[32]++;
                break;
            case MV:
                data = rf[ins.Rs1];
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
                data = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
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

        return data;
    }

    public void Memory(Instruction ins) {
        switch (ins.opcode) {
            case LD:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[rf[ins.Rs1] + rf[ins.Rs2]];
                }
                cycle++;
                break;
            case LDC:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = ins.Const;
                }
                cycle++;
                break;
            case LDI:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[ins.Const];
                }
                cycle++;
                break;
            case LDO:
                if(ins.Rd != 0) {
                    rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                }
                cycle++;
                break;
            case ST:
                mem[rf[ins.Rs1] + rf[ins.Rs2]] = rf[ins.Rd];
                cycle++;
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rd];
                cycle++;
                break;
            case STO:
                mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                cycle++;
                break;
            default:
                cycle++;
                break;
        }
    }

    public void WriteBack(Instruction ins, Integer data) {
        if(data != null && ins.Rd != 0) {
            rf[ins.Rd] = data;
        }
        cycle++;
    }

    public void RunProcessor() {

        while(!finished && rf[32] < instructions.length) {
            //System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Integer data = Execute(instruction);
            Memory(instruction);
            executedInsts++;
            WriteBack(instruction, data);
        }
        System.out.println("5 cycle scalar non-pipelined processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
    }

}
