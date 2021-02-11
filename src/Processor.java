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

    public Instruction Fetch() {
        Instruction instruction = instr[pc];
        cycle++;
        return instruction;
    }

    public void Decode() {

    }

    public void Execute() {
        pc++;
    }

    public void Load() {

    }

    public void WriteBack() {

    }

    public void RunProcessor() {
        while(!finished && pc < instr.length) {
            Instruction ins = Fetch();
            Decode();
            Execute();

            System.out.println("PC " + pc);
        }
    }

}
