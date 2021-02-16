public class Main {

    public static void main(String[] args) {
        Instruction[] instructions = new Instruction[512];
        int[] mem = new int[1024];
        //Functionality testing program
        /*
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
         */

        //Vector addition
        for(int i = 0; i < 100; i++) {
            mem[10 + i] = i; //assigning A[i]
            mem[120 + i] = i; //assigning B[i]
        }
        instructions[0] = new Instruction(Opcode.LDC,1,0,0,10); //pointer to array A
        instructions[1] = new Instruction(Opcode.LDC,2,0,0,120); //pointer to array B
        instructions[2] = new Instruction(Opcode.LDC,3,0,0,230); //pointer to array C
        instructions[3] = new Instruction(Opcode.LDC,4,0,0,0); // i = 0
        instructions[4] = new Instruction(Opcode.LDC,8,0,0,100); // for loop limit (100)
        instructions[5] = new Instruction(Opcode.LDX,5,1,4,0); // a = A[&A + i]
        instructions[6] = new Instruction(Opcode.LDX,6,2,4,0); // b = B[&B + i]
        instructions[7] = new Instruction(Opcode.ADD,7,5,6,0); // c = a + b
        instructions[8] = new Instruction(Opcode.STX,7,3,4,0); // C[&C + i] = c
        instructions[9] = new Instruction(Opcode.ADDI,4,4,0,1); // i++
        instructions[10] = new Instruction(Opcode.CMP,9,8,4,0); // i < 100 ?
        instructions[11] = new Instruction(Opcode.BNE,0,9,0,5); //when i hit 100 don't branch
        Processor processor = new Processor();
        processor.instructions = instructions;
        processor.mem = mem;
	    processor.RunProcessor();
    }
}
