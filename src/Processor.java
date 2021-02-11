public class Processor {

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int[] mem = new int[1024];
    int[] rf = new int[32]; //Register file (physical)
    Instruction[] instr = new Instruction[512];
    boolean finished = false;

    public Processor() {

    }
    //This will fetch int instead later
    public Instruction Fetch() {
        Instruction instruction = instr[pc];
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
            default:
                System.out.println("Error, invalid opcode");
        }
        executedInsts++;
    }

    public void Load() {

    }

    public void WriteBack() {

    }

    public void RunProcessor() {
        mem[0] = 5;
        mem[1] = 7;
        instr[0] = new Instruction(Opcode.NOOP,0,0,0,0);
        instr[1] = new Instruction(Opcode.LDI,0,0,0,0);
        instr[2] = new Instruction(Opcode.LDI,1,0,0,1);
        instr[3] = new Instruction(Opcode.ADD, 2,0,1,0);
        instr[4] = new Instruction(Opcode.ADDI,3,2,0,10);
        instr[5] = new Instruction(Opcode.ST,1,3,0,-2);

        while(!finished && pc < instr.length) {
            System.out.println("PC " + pc);
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Execute(instruction);
        }
        System.out.println("Terminated");
    }

}
