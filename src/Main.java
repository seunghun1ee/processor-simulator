public class Main {

    public static void main(String[] args) {
        Instruction[] instructions = new Instruction[512];
        int[] mem = new int[1024];

        mem[0] = 5;
        mem[1] = 7;
        instructions[0] = new Instruction(Opcode.NOOP,0,0,0,0);
        instructions[1] = new Instruction(Opcode.LDI,0,0,0,0);
        instructions[2] = new Instruction(Opcode.LDI,1,0,0,1);
        instructions[3] = new Instruction(Opcode.ADD, 2,0,1,0);
        instructions[4] = new Instruction(Opcode.ADDI,3,2,0,10);
        instructions[5] = new Instruction(Opcode.ST,1,3,0,-2);
        instructions[6] = new Instruction(Opcode.JMP,0,0,0,510);

        Processor processor = new Processor();
        processor.instructions = instructions;
        processor.mem = mem;
	    processor.RunProcessor();
    }
}
