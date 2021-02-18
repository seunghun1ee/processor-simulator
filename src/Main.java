public class Main {

    public static void main(String[] args) {
        Instruction[] instructions = new Instruction[512];
        int[] mem = new int[1024];
        //Functionality testing program
        /*
        mem[0] = 5;
        mem[1] = 7;
        instructions[0] = new Instruction(Opcode.LDC,0,0,0,1000);
        instructions[1] = new Instruction(Opcode.LDI,1,0,0,0);
        instructions[2] = new Instruction(Opcode.LDI,2,0,0,1);
        instructions[3] = new Instruction(Opcode.ADD, 3,1,2,0);
        instructions[4] = new Instruction(Opcode.ADDI,4,3,0,10);
        instructions[5] = new Instruction(Opcode.ST,4,1,0,0);
        instructions[6] = new Instruction(Opcode.MUL,5,1,2,0);
        instructions[7] = new Instruction(Opcode.DIV, 6, 3,1,0);
        instructions[8] = new Instruction(Opcode.AND,7,1,2,0);
        instructions[9] = new Instruction(Opcode.OR,8,1,2,0);
        instructions[10] = new Instruction(Opcode.MV,9,7,0,0);
        instructions[11] = new Instruction(Opcode.BEQ, 0,1,2,509);
        instructions[12] = new Instruction(Opcode.BEQ, 0,1,1,14);
        instructions[14] = new Instruction(Opcode.BLT,0,2,1,508);
        instructions[15] = new Instruction(Opcode.BLT,0,1,2,18);
        instructions[18] = new Instruction(Opcode.LDC,10,0,0,1000);
        instructions[19] = new Instruction(Opcode.LD,11,0,1,0);
        instructions[20] = new Instruction(Opcode.CMP,12,1,0,0);
        instructions[21] = new Instruction(Opcode.CMP,12,0,0,0);
        instructions[22] = new Instruction(Opcode.CMP,12,0,1,0);
        instructions[23] = new Instruction(Opcode.STI,1,0,0,3);
        instructions[24] = new Instruction(Opcode.JMP,0,0,0,6);
        instructions[30] = new Instruction(Opcode.MULI,1,1,0,2);
        instructions[31] = new Instruction(Opcode.DIVI,1,4,0,6);
        instructions[32] = new Instruction(Opcode.NOT,1,2,0,0);
        instructions[33] = new Instruction(Opcode.BR,0,0,0,500);
        instructions[500] = new Instruction(Opcode.HALT,0,0,0,0);
         */

        //Vector addition
        int length = 5;
        int ap = 2;
        int bp = 10;
        int cp = 16;
        for(int i = 0; i < length; i++) {
            mem[ap + i] = i; //assigning A[i]
            mem[bp + i] = 2 * i; //assigning B[i]
        }
        instructions[0] = new Instruction(Opcode.LDC,1,0,0,ap); //pointer to array A
        instructions[1] = new Instruction(Opcode.LDC,2,0,0,bp); //pointer to array B
        instructions[2] = new Instruction(Opcode.LDC,3,0,0,cp); //pointer to array C
        instructions[3] = new Instruction(Opcode.LDC,4,0,0,0); // i = 0
        instructions[4] = new Instruction(Opcode.LDC,8,0,0,length); // for loop limit
        instructions[5] = new Instruction(Opcode.LD,5,1,4,0); // a = A[&A + i], for loop starts
        instructions[6] = new Instruction(Opcode.LD,6,2,4,0); // b = B[&B + i]
        instructions[7] = new Instruction(Opcode.ADD,7,5,6,0); // c = a + b
        instructions[8] = new Instruction(Opcode.ST,7,3,4,0); // C[&C + i] = c
        instructions[9] = new Instruction(Opcode.ADDI,4,4,0,1); // i++
        instructions[10] = new Instruction(Opcode.BLT,0,4,8,5); //branch back to for loop if i < 100
        instructions[11] = new Instruction(Opcode.HALT,0,0,0,0); //Terminate

        Processor processor = new Processor();
        processor.instructions = instructions;
        processor.mem = mem;
	    processor.RunProcessor();
    }
}
