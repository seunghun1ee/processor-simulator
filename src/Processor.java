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

    public void Execute(Instruction ins) {
        switch (ins.opcode) {
            case NOOP:
                cycle++;
                pc++;
                break;
            case ADD:
                rf[ins.Rd] = rf[ins.Rs1] + rf[ins.Rs2];
                cycle += 2;
                pc++;
                break;
            case ADDI:
                rf[ins.Rd] = rf[ins.Rs1] + ins.Const;
                cycle += 2;
                pc++;
                break;
            case LD:
                rf[ins.Rd] = mem[rf[ins.Rs1] + ins.Const];
                cycle++;
                pc++;
                break;
            case LDI:
                rf[ins.Rd] = mem[ins.Const];
                cycle++;
                pc++;
                break;
            case ST:
                mem[rf[ins.Rd] + ins.Const] = rf[ins.Rs1];
                cycle++;
                pc++;
                break;
            case STI:
                mem[ins.Const] = rf[ins.Rs1];
                cycle++;
                pc++;
                break;
            case JMP:
                pc = ins.Const;
                cycle++;
                break;
            default:
                System.out.println("Error, invalid opcode");
        }
        executedInsts++;
    }

    public void Memory() {

    }

    public void WriteBack() {

    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            System.out.println("PC " + pc + " " + cycle + " number of cycles passed");
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Execute(instruction);
        }
        System.out.println("Terminated");
    }

}
