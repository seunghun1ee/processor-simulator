public class Processor {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[32]; //Register file (physical) register 0 always have value zero (input is ignored)
    Instruction[] instructions;
    boolean finished = false;

    public Processor() {

    }
    //This will fetch int instead later
    public Instruction Fetch() {
        Instruction instruction = instructions[pc];
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
                pc++;
                break;
            case ADD:
                data = rf[ins.Rs1] + rf[ins.Rs2];
                cycle += 2;
                pc++;
                break;
            case ADDI:
                data = rf[ins.Rs1] + ins.Const;
                cycle += 2;
                pc++;
                break;
            case SUB:
                data = rf[ins.Rs1] - rf[ins.Rs2];
                cycle += 2;
                pc++;
                break;
            case MUL:
                data = rf[ins.Rs1] * rf[ins.Rs2];
                cycle += 3;
                pc++;
                break;
            case MULI:
                data = rf[ins.Rs1] * ins.Const;
                cycle += 3;
                pc++;
                break;
            case DIV:
                data = rf[ins.Rs1] / rf[ins.Rs2];
                cycle += 4;
                pc++;
                break;
            case DIVI:
                data = rf[ins.Rs1] / ins.Const;
                cycle += 4;
                pc++;
                break;
            case NOT:
                data = ~rf[ins.Rs1];
                cycle++;
                pc++;
                break;
            case AND:
                data = rf[ins.Rs1] & rf[ins.Rs2];
                cycle++;
                pc++;
                break;
            case OR:
                data = rf[ins.Rs1] | rf[ins.Rs2];
                cycle++;
                pc++;
                break;
            case MV:
                data = rf[ins.Rs1];
                cycle++;
                pc++;
                break;
            case BR:
                pc = ins.Const;
                cycle++;
                break;
            case JMP:
                pc = pc + ins.Const;
                cycle++;
                break;
            case BEQ:
                if(rf[ins.Rs1] == rf[ins.Rs2]) {
                    pc = ins.Const;
                }
                else {
                    pc++;
                }
                cycle++;
                break;
            case BLT:
                if(rf[ins.Rs1] < rf[ins.Rs2]) {
                    pc = ins.Const;
                }
                else {
                    pc++;
                }
                cycle++;
                break;
            case CMP:
                data = Integer.compare(rf[ins.Rs1], rf[ins.Rs2]);
                cycle++;
                pc++;
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

        while(!finished && pc < instructions.length) {
            System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Integer data = Execute(instruction);
            Memory(instruction);
            executedInsts++;
            WriteBack(instruction, data);
        }
        System.out.println("Terminated");
    }

}
