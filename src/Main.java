public class Main {

    public static void main(String[] args) {
        Instruction[] instructions = new Instruction[512];
        int[] mem = new int[1024];
        //Testing program
        mem[0] = 5;
        mem[1] = 7;
        instructions[0] = new Instruction(Opcode.NOOP,0,0,0,0);
        instructions[1] = new Instruction(Opcode.LDI,0,0,0,0);
        instructions[2] = new Instruction(Opcode.LDI,1,0,0,1);
        instructions[3] = new Instruction(Opcode.ADD, 2,0,1,0);
        instructions[4] = new Instruction(Opcode.ADDI,3,2,0,10);
        instructions[5] = new Instruction(Opcode.ST,3,1,0,-2);
        instructions[6] = new Instruction(Opcode.MUL,4,0,1,0);
        instructions[7] = new Instruction(Opcode.DIV, 5, 3,1,0);
        instructions[8] = new Instruction(Opcode.AND,6,0,1,0);
        instructions[9] = new Instruction(Opcode.OR,7,0,1,0);
        instructions[10] = new Instruction(Opcode.MOVE,8,7,0,0);
        instructions[11] = new Instruction(Opcode.BEQ, 0,0,1,509);
        instructions[12] = new Instruction(Opcode.BEQ, 0,0,0,14);
        instructions[14] = new Instruction(Opcode.BNE,0,0,0,508);
        instructions[15] = new Instruction(Opcode.BNE,0,0,1,18);
        instructions[18] = new Instruction(Opcode.LDC,9,0,0,1000);
        instructions[19] = new Instruction(Opcode.LD,10,2,0,-7);
        instructions[20] = new Instruction(Opcode.CMP,11,1,0,0);
        instructions[21] = new Instruction(Opcode.CMP,11,0,0,0);
        instructions[22] = new Instruction(Opcode.CMP,11,0,1,0);
        instructions[23] = new Instruction(Opcode.STI,1,0,0,3);
        instructions[24] = new Instruction(Opcode.JMP,0,0,0,510);

        Processor processor = new Processor();
        processor.instructions = instructions;
        processor.mem = mem;
	    processor.RunProcessor();
    }
}
