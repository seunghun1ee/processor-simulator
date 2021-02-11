public class Processor {

    int cycle = 0;
    int pc = 0; //Program counter
    int instructions = 0;
    int[] mem = new int[1024];
    int[] rf = new int[32]; //Register file (physical)
    //int[] instr = new int[512];
    boolean finished = false;

    public Processor() {

    }

    public void Fetch() {

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
        while(!finished && pc < mem.length) {
            Fetch();
            Decode();
            Execute();

            System.out.println("PC " + pc);
        }
    }

}
