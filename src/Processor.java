public class Processor {

    int cycle = 0;
    int pc = 0; //Program counter
    int instructions = 0; //Number of instructions executed
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
            default:
                System.out.println("Error, invalid opcode");
        }
    }

    public void Load() {

    }

    public void WriteBack() {

    }

    public void RunProcessor() {
        rf[1] = 5;
        rf[2] = 7;
        instr[0] = new Instruction(Opcode.NOOP,0,0,0,0);
        instr[1] = new Instruction(Opcode.ADD, 0,1,2,0);
        while(!finished && pc < instr.length) {
            System.out.println("PC " + pc);
            Instruction fetched = Fetch();
            Instruction instruction = Decode(fetched);
            Execute(instruction);
        }
        System.out.println("Terminated");
    }

}
