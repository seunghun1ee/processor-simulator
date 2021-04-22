import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    public static void main(String[] args) throws IOException {
        Instruction[] instructions = new Instruction[512];
        int[] mem = new int[1024];

        //Vector addition
        int length = 10;
        int ap = 0;
        int bp = 12;
        int cp = 24;
        for(int i = 0; i < length; i++) {
            mem[ap + i] = i; //assigning A[i]
            mem[bp + i] = 2 * i; //assigning B[i]
        }
        instructions[0] = new Instruction(Opcode.MOVC,1,0,0,ap); //pointer to array A
        instructions[1] = new Instruction(Opcode.MOVC,2,0,0,bp); //pointer to array B
        instructions[2] = new Instruction(Opcode.MOVC,3,0,0,cp); //pointer to array C
        instructions[3] = new Instruction(Opcode.MOVC,4,0,0,0); // i = 0
        instructions[4] = new Instruction(Opcode.MOVC,8,0,0,length); // for loop limit
        instructions[5] = new Instruction(Opcode.LD,5,1,4,0); // a = A[&A + i], for loop starts
        instructions[6] = new Instruction(Opcode.LD,6,2,4,0); // b = B[&B + i]
        instructions[7] = new Instruction(Opcode.ADD,7,5,6,0); // c = a + b
        instructions[8] = new Instruction(Opcode.ST,7,3,4,0); // C[&C + i] = c
        instructions[9] = new Instruction(Opcode.ADDI,4,4,0,1); // i++
        instructions[10] = new Instruction(Opcode.CMP,10,4,8,0); // $10 = cmp($4,$8)
        instructions[11] = new Instruction(Opcode.BRN,0,10,0,5); //branch back to for loop if i < 100
        instructions[12] = new Instruction(Opcode.HALT,0,0,0,0); //Terminate

        //Bubble sort
        Instruction[] instructions2 = new Instruction[512];
        int[] mem2 = new int[1024];

        int[] arrayToSort = {512,52,61,3,-6,-127,75,21,98,1,874,-1239,431,94,10,36};
//        int[] arrayToSort = {512,52,61,3-6,-127};
        int pointer = 2;
        System.arraycopy(arrayToSort,0,mem2,pointer,arrayToSort.length);
        instructions2[0] = new Instruction(Opcode.MOVC,1,0,0,pointer); // load array pointer
        instructions2[1] = new Instruction(Opcode.MOVC,2,0,0,0); // i = 0
        instructions2[2] = new Instruction(Opcode.MOVC,4,0,0, arrayToSort.length); // load array length
        instructions2[3] = new Instruction(Opcode.ADDI,5,4,0,-1); // outer for loop limit = length - 1
        instructions2[4] = new Instruction(Opcode.SUB,6,4,2,0); // length - i, outer loop starting point
        instructions2[5] = new Instruction(Opcode.MOVC,3,0,0,0); // j = 0
        instructions2[6] = new Instruction(Opcode.ADDI,6,6,0,-1); //inner for loop limit = length - i - 1
        instructions2[7] = new Instruction(Opcode.LD,7,1,3,0); // a = array[j], inner loop starting point
        instructions2[8] = new Instruction(Opcode.ADD,9,1,3,0); // pointer + j
        instructions2[9] = new Instruction(Opcode.LDI,8,9,0,1); // b = array[j + 1]
        instructions2[10] = new Instruction(Opcode.CMP,11,8,7,0); // $11 = cmp(b,a)
        instructions2[11] = new Instruction(Opcode.BRN,0,11,0,13); // if($11 < 0)
        instructions2[12] = new Instruction(Opcode.BR,0,0,0,18); //if not b < a skip to the end of inner loop
        instructions2[13] = new Instruction(Opcode.MOV,10,7,0,0); // temp = a
        instructions2[14] = new Instruction(Opcode.MOV,7,8,0,0); // a = b
        instructions2[15] = new Instruction(Opcode.MOV,8,10,0,0); // b = temp, swap complete
        instructions2[16] = new Instruction(Opcode.ST,7,1,3,0); // store array[j] = a
        instructions2[17] = new Instruction(Opcode.STI,8,9,0,1); // store array[j + 1] = b
        instructions2[18] = new Instruction(Opcode.ADDI,3,3,0,1); // j++
        instructions2[19] = new Instruction(Opcode.CMP,11,3,6,0); // $11 = cmp(j, length - i - 1)
        instructions2[20] = new Instruction(Opcode.BRN,0,11,0,7); // loop back to inner starting point if j < length - i - 1
        instructions2[21] = new Instruction(Opcode.ADDI,2,2,0,1); // i++
        instructions2[22] = new Instruction(Opcode.CMP,11,2,5,0); // $11 = cmp(i, length - 1)
        instructions2[23] = new Instruction(Opcode.BRN,0,11,0,4); // loop back to outer starting point if i < length - 1
        instructions2[24] = new Instruction(Opcode.HALT,0,0,0,0); // halt

        // factorial (recursion)
        Instruction[] instructions3 = new Instruction[512];
        int[] mem3 = new int[1024];
        int loc = 3;
        int num = 8;
        int sp = 100;
        mem3[loc] = num;
        //main
        instructions3[0] = new Instruction(Opcode.MOVC,29,0,0,sp); // $29 is $sp
        instructions3[1] = new Instruction(Opcode.LDI,1,0,0,loc); // load argument
        instructions3[2] = new Instruction(Opcode.MOV,4,1,0,0); // copy argument to $a0 ($4)
        instructions3[3] = new Instruction(Opcode.MOVC,14,0,0,7); // $t6(14) = main return address
        instructions3[4] = new Instruction(Opcode.STI,14,29,0,0); // store main ra to sp + 0
        instructions3[5] = new Instruction(Opcode.STI,4,29,0,1); // store arg to sp + 1
        instructions3[6] = new Instruction(Opcode.BR,0,0,0,100); // call fac
        instructions3[7] = new Instruction(Opcode.STI,2,0,0,loc+1); // store returned result at mem[loc + 1]
        instructions3[8] = new Instruction(Opcode.HALT,0,0,0,0); // halt
        //fac
        instructions3[100] = new Instruction(Opcode.LDI,13,29,0,1); // $13 = mem[sp + 1] (arg)
        instructions3[101] = new Instruction(Opcode.CMP,15,13,0,0); // $t7(15) = cmp($13,$zero)
        instructions3[102] = new Instruction(Opcode.BRZ,0,15,0,130); // branch to base if zero
        instructions3[103] = new Instruction(Opcode.ADDI,13,13,0,-1); // decrement arg
        instructions3[104] = new Instruction(Opcode.ADDI,29,29,0,2); // sp += 2
        instructions3[105] = new Instruction(Opcode.MOVC, 14,0,0,120); // $t6(14) = pop ra
        instructions3[106] = new Instruction(Opcode.STI,14,29,0,0); // mem[sp + 0] = pop ra
        instructions3[107] = new Instruction(Opcode.STI,13,29,0,1); // mem[sp + 1] = arg
        instructions3[108] = new Instruction(Opcode.BR,0,0,0,100); // recursively call fac
        //pop
        instructions3[120] = new Instruction(Opcode.LDI,9,29,0,1); // $9 = mem[sp + 1] (arg)
        instructions3[121] = new Instruction(Opcode.LDI,8,29,0,0); // $8 = mem[sp + 0] (ra)
        instructions3[122] = new Instruction(Opcode.MUL,2,2,9,0); // $2 *= arg
        instructions3[123] = new Instruction(Opcode.ADDI,29,29,0,-2); // sp -= 2
        instructions3[124] = new Instruction(Opcode.BR,0,8,0,0); // return to ra
        //base case
        instructions3[130] = new Instruction(Opcode.MOVC,2,0,0,1); // result register $2 = 1
        instructions3[131] = new Instruction(Opcode.STI,2,29,0,1); // replace zero with 1
        instructions3[132] = new Instruction(Opcode.BR,0,0,0,120); // go to pop

        //Many dependencies
        Instruction[] instructions4 = new Instruction[512];
        int[] mem4 = new int[1024];
        mem4[5] = 1000;
        mem4[10] = 123;
        mem4[11] = mem4[10] * 8;
        instructions4[0] = new Instruction(Opcode.MOVC,1,0,0,10); // $1 = 10
        instructions4[1] = new Instruction(Opcode.MOVC,2,0,0,2); // $2 = 2
        instructions4[2] = new Instruction(Opcode.DIV,3,1,2,0); // $3 = $1 / $2 = 5 (6 cycles)
        instructions4[3] = new Instruction(Opcode.LD,10,3,0,0); // $10 = mem[ 5 + 0 ] (2 cycles, dependent:2)
        instructions4[4] = new Instruction(Opcode.MUL,4,1,2,0); // $4 = $1 * $2 = 20 (2 cycles)
        instructions4[5] = new Instruction(Opcode.SUB,6,3,1,0); // $6 = $3 - $1 = -5 (div dependent:2)
        instructions4[6] = new Instruction(Opcode.ADD,5,1,2,0); // $5 = $1 + $2 = 12
        instructions4[7] = new Instruction(Opcode.SHL,11,1,2,0); // $11 = $1 << $2 = 40
        instructions4[8] = new Instruction(Opcode.STI,6,3,0,1); // mem[ 5 + 1 ] = $6 (2 cycles, dependent:2,5)
        instructions4[9] = new Instruction(Opcode.ADDI,7,2,0,4); // $7 = $2 + 4 = 6
        instructions4[10] = new Instruction(Opcode.LDI,8,0,0,10); // $8 = mem[10] (2 cycles)
        instructions4[11] = new Instruction(Opcode.LDI,9,0,0,11); // $9 = mem[11] (2 cycles)
        instructions4[12] = new Instruction(Opcode.DIV, 12,9,8,0); // $12 = $9 / $8 = 8 (6 cycles, dependent:10,11)
        instructions4[13] = new Instruction(Opcode.DIVI,13,12,0,2); // $13 = $12 / 2 = 4 (6 cycles, dependent:12)
        instructions4[14] = new Instruction(Opcode.NOT,14,0,0,0); // $14 = ¬ $0 = -1
        instructions4[15] = new Instruction(Opcode.ADDI,1,1,0,20); // $1 = $1 + 20 = 30
        instructions4[16] = new Instruction(Opcode.MUL,4,4,4,0); // $4 = $4 * $4 = 400 (2 cycles)
        instructions4[17] = new Instruction(Opcode.ADD,2,2,1,0); // $2 = $2 + $1 = 32
        instructions4[18] = new Instruction(Opcode.AND,11,14,1,0); // $11 = $14 & $1 = 30
        instructions4[19] = new Instruction(Opcode.BR,0,0,0,50); // PC <- 50
        instructions4[20] = new Instruction(Opcode.HALT,0,0,0,0); // HALT
        instructions4[50] = new Instruction(Opcode.ADD,15,1,2,0); // $15 = $1 + $2 = 62
        instructions4[51] = new Instruction(Opcode.STI,15,3,0,2); // mem[5 + 2] = $15
        instructions4[52] = new Instruction(Opcode.BR,0,0,0,20); // jump back to halt

        //Branch hell
//        Instruction[] instructions5 = new Instruction[512];
//        int[] mem5 = new int[1024];
//        mem5[2] = 120;
//        mem5[3] = 3;
//        mem5[4] = 9;
//        instructions5[0] = new Instruction(Opcode.LDI,1,0,0,2); // $1 = mem[2] = 120
//        instructions5[1] = new Instruction(Opcode.LDI,2,0,0,3); // $2 = mem[3] = 3
//        instructions5[2] = new Instruction(Opcode.DIV,3,1,2,0); // $3 = 120 / 3 = 40
//        instructions5[3] = new Instruction(Opcode.ADDI,4,2,0,1); // $4 = $2 + 1 = 4
//        instructions5[4] = new Instruction(Opcode.JMP,0,0,0,11); // pc = pc + 11 = 15
//        instructions5[5] = new Instruction(Opcode.HALT,0,0,0,0); // HALT
//
//        instructions5[15] = new Instruction()


        System.out.println("Benchmark1 - Vector addition (size: " + length + ")");
        Processor8 processor = new Processor8(mem,instructions);
        processor.RunProcessor();
        createDump(processor.mem, "mem_bench1.txt");
        createDump(processor.rf,"rf_bench1.txt");

        System.out.println("Benchmark2 - Bubble sort (size: " + arrayToSort.length + ")");
        Processor8 processor2 = new Processor8(mem2,instructions2);
        processor2.RunProcessor();
        createDump(processor2.mem, "mem_bench2.txt");
        createDump(processor2.rf,"rf_bench2.txt");

//        System.out.println("Benchmark3 - Factorial(" + num + ")");
//	    Processor7 processor3 = new Processor7(mem3,instructions3);
//	    processor3.RunProcessor();
//	    createDump(processor3.mem, "mem_bench3.txt");
//	    createDump(processor3.rf,"rf_bench3.txt");

	    System.out.println("Benchmark4 - many dependencies");
	    Processor8 processor4 = new Processor8(mem4,instructions4);
	    processor4.RunProcessor();
	    createDump(processor4.mem, "mem_bench4.txt");
	    createDump(processor4.rf,"rf_bench4.txt");

    }

    private static void createDump(int[] array, String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("Address  Value");
        for(int i = 0; i < array.length; i++) {
            printWriter.printf("%04d     %d\n",i,array[i]);
        }
        printWriter.close();
    }
}
