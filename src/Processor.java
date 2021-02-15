public class Processor {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int[] mem;
    int[] rf = new int[32]; //Register file (physical)
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
            case MUL:
                data = rf[ins.Rs1] * rf[ins.Rs2];
                cycle += 3;
                pc++;
                break;
            case DIV:
                data = rf[ins.Rs1] / rf[ins.Rs2];
                cycle += 4;
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
            case MOVE:
                data = rf[ins.Rs1];
                cycle++;
                pc++;
                break;
            case JMP:
                pc = ins.Const;
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
            case BNE:
                if(rf[ins.Rs1] != rf[ins.Rs2]) {
                    pc = ins.Const;
                }
                else {
                    pc++;
                }
                cycle++;
                break;
            case CMP:
                if(rf[ins.Rs1] > rf[ins.Rs2]) {
                    data = 1;
                }
                else if(rf[ins.Rs1] == rf[ins.Rs2]) {
                    data = 0;
                }
                else {
                    data = -1;
                }
                cycle++;
                pc++;
                break;
            default:
                break;
        }

        return data;
    }

    public void Memory(Instruction ins) {
        switch (ins.opcode) {
            case LD:
                rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                pc++;
                break;
            case LDC:
                rf[ins.Rd] = ins.Const;
                pc++;
                break;
            case LDI:
                rf[ins.Rd] = mem[ins.Const];
                pc++;
                break;
            case ST:
                mem[rf[ins.Rs1] + ins.Const] = rf[ins.Rd];
                pc++;
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rd];
                pc++;
                break;
            default:
                break;
        }
        cycle++;
    }

    public void WriteBack(Instruction ins, Integer data) {
        if(data != null) {
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
