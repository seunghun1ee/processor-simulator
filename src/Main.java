import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    public static void main(String[] args) throws IOException {
        int[] argValues = new int[args.length];
        for(int i = 0; i< args.length; i++) {
            argValues[i] = Integer.parseInt(args[i]);
        }
        // Default Configurations
        int superScalarWidth = 4;
        BranchMode branchMode = BranchMode.DYNAMIC_2BIT;
        int numOfALU = 4;
        int numOfLOAD = 2;
        int numOfSTORE = 1;
        int numOfBRU = 1;
        if(argValues[0] > 0) {
            superScalarWidth = argValues[0];
        }
        if(argValues[1] > 0) {
            switch (argValues[1]) {
                case 1:
                    branchMode = BranchMode.FIXED_NOT_TAKEN;
                    break;
                case 2:
                    branchMode = BranchMode.FIXED_TAKEN;
                    break;
                case 3:
                    branchMode = BranchMode.STATIC;
                    break;
                case 4:
                    branchMode = BranchMode.DYNAMIC_1BIT;
                    break;
                case 5:
                default:
                    branchMode = BranchMode.DYNAMIC_2BIT;
                    break;
            }
        }
        if(argValues[2] > 0) {
            numOfALU = argValues[2];
        }
        if(argValues[3] > 0) {
            numOfLOAD = argValues[3];
        }
        if(argValues[4] > 0) {
            numOfSTORE = argValues[4];
        }
        if(argValues[5] > 0) {
            numOfBRU = argValues[5];
        }


        Instruction[] bench1inst = new Instruction[512];
        int[] bench1mem = new int[1024];

        //Vector addition
        int length = 10;
        int ap = 0;
        int bp = 12;
        int cp = 24;
        for(int i = 0; i < length; i++) {
            bench1mem[ap + i] = i; //assigning A[i]
            bench1mem[bp + i] = 2 * i; //assigning B[i]
        }
        bench1inst[0] = new Instruction(Opcode.MOVC,1,0,0,ap); //pointer to array A
        bench1inst[1] = new Instruction(Opcode.MOVC,2,0,0,bp); //pointer to array B
        bench1inst[2] = new Instruction(Opcode.MOVC,3,0,0,cp); //pointer to array C
        bench1inst[3] = new Instruction(Opcode.MOVC,4,0,0,0); // i = 0
        bench1inst[4] = new Instruction(Opcode.MOVC,8,0,0,length); // for loop limit
        bench1inst[5] = new Instruction(Opcode.LD,5,1,4,0); // a = A[&A + i], for loop starts
        bench1inst[6] = new Instruction(Opcode.LD,6,2,4,0); // b = B[&B + i]
        bench1inst[7] = new Instruction(Opcode.ADD,7,5,6,0); // c = a + b
        bench1inst[8] = new Instruction(Opcode.ST,7,3,4,0); // C[&C + i] = c
        bench1inst[9] = new Instruction(Opcode.ADDI,4,4,0,1); // i++
        bench1inst[10] = new Instruction(Opcode.CMP,10,4,8,0); // $10 = cmp($4,$8)
        bench1inst[11] = new Instruction(Opcode.BRN,0,10,0,5); //branch back to for loop if i < 100
        bench1inst[12] = new Instruction(Opcode.HALT,0,0,0,0); //Terminate

        //Bubble sort
        Instruction[] bench2inst = new Instruction[512];
        int[] bench2mem = new int[1024];

        int[] arrayToSort = {512,52,61,3,-6,-127,75,21,98,1,874,-1239,431,94,10,36};
//        int[] arrayToSort = {512,52,61,-3,-127};
        int pointer = 2;
        System.arraycopy(arrayToSort,0,bench2mem,pointer,arrayToSort.length);
        bench2inst[0] = new Instruction(Opcode.MOVC,1,0,0,pointer); // load array pointer
        bench2inst[1] = new Instruction(Opcode.MOVC,2,0,0,0); // i = 0
        bench2inst[2] = new Instruction(Opcode.MOVC,4,0,0, arrayToSort.length); // load array length
        bench2inst[3] = new Instruction(Opcode.ADDI,5,4,0,-1); // outer for loop limit = length - 1
        bench2inst[4] = new Instruction(Opcode.SUB,6,4,2,0); // length - i, outer loop starting point
        bench2inst[5] = new Instruction(Opcode.MOVC,3,0,0,0); // j = 0
        bench2inst[6] = new Instruction(Opcode.ADDI,6,6,0,-1); //inner for loop limit = length - i - 1
        bench2inst[7] = new Instruction(Opcode.LD,7,1,3,0); // a = array[j], inner loop starting point
        bench2inst[8] = new Instruction(Opcode.ADD,9,1,3,0); // pointer + j
        bench2inst[9] = new Instruction(Opcode.LDI,8,9,0,1); // b = array[j + 1]
        bench2inst[10] = new Instruction(Opcode.CMP,11,8,7,0); // $11 = cmp(b,a)
        bench2inst[11] = new Instruction(Opcode.BRN,0,11,0,13); // if($11 < 0)
        bench2inst[12] = new Instruction(Opcode.BR,0,0,0,18); //if not b < a skip to the end of inner loop
        bench2inst[13] = new Instruction(Opcode.MOV,10,7,0,0); // temp = a
        bench2inst[14] = new Instruction(Opcode.MOV,7,8,0,0); // a = b
        bench2inst[15] = new Instruction(Opcode.MOV,8,10,0,0); // b = temp, swap complete
        bench2inst[16] = new Instruction(Opcode.ST,7,1,3,0); // store array[j] = a
        bench2inst[17] = new Instruction(Opcode.STI,8,9,0,1); // store array[j + 1] = b
        bench2inst[18] = new Instruction(Opcode.ADDI,3,3,0,1); // j++
        bench2inst[19] = new Instruction(Opcode.CMP,11,3,6,0); // $11 = cmp(j, length - i - 1)
        bench2inst[20] = new Instruction(Opcode.BRN,0,11,0,7); // loop back to inner starting point if j < length - i - 1
        bench2inst[21] = new Instruction(Opcode.ADDI,2,2,0,1); // i++
        bench2inst[22] = new Instruction(Opcode.CMP,11,2,5,0); // $11 = cmp(i, length - 1)
        bench2inst[23] = new Instruction(Opcode.BRN,0,11,0,4); // loop back to outer starting point if i < length - 1
        bench2inst[24] = new Instruction(Opcode.HALT,0,0,0,0); // halt

        // factorial (recursion)
        Instruction[] bench3inst = new Instruction[512];
        int[] bench3mem = new int[1024];
        int loc = 3;
        int num = 8;
        int sp = 100;
        bench3mem[loc] = num;
        //main
        bench3inst[0] = new Instruction(Opcode.MOVC,29,0,0,sp); // $29 is $sp
        bench3inst[1] = new Instruction(Opcode.LDI,1,0,0,loc); // load argument
        bench3inst[2] = new Instruction(Opcode.MOV,4,1,0,0); // copy argument to $a0 ($4)
        bench3inst[3] = new Instruction(Opcode.MOVC,14,0,0,7); // $t6(14) = main return address
        bench3inst[4] = new Instruction(Opcode.STI,14,29,0,0); // store main ra to sp + 0
        bench3inst[5] = new Instruction(Opcode.STI,4,29,0,1); // store arg to sp + 1
        bench3inst[6] = new Instruction(Opcode.BR,0,0,0,100); // call fac
        bench3inst[7] = new Instruction(Opcode.STI,2,0,0,loc+1); // store returned result at mem[loc + 1]
        bench3inst[8] = new Instruction(Opcode.HALT,0,0,0,0); // halt
        //fac
        bench3inst[100] = new Instruction(Opcode.LDI,13,29,0,1); // $13 = mem[sp + 1] (arg)
        bench3inst[101] = new Instruction(Opcode.CMP,15,13,0,0); // $t7(15) = cmp($13,$zero)
        bench3inst[102] = new Instruction(Opcode.BRZ,0,15,0,130); // branch to base if zero
        bench3inst[103] = new Instruction(Opcode.ADDI,13,13,0,-1); // decrement arg
        bench3inst[104] = new Instruction(Opcode.ADDI,29,29,0,2); // sp += 2
        bench3inst[105] = new Instruction(Opcode.MOVC, 14,0,0,120); // $t6(14) = pop ra
        bench3inst[106] = new Instruction(Opcode.STI,14,29,0,0); // mem[sp + 0] = pop ra
        bench3inst[107] = new Instruction(Opcode.STI,13,29,0,1); // mem[sp + 1] = arg
        bench3inst[108] = new Instruction(Opcode.BR,0,0,0,100); // recursively call fac
        //pop
        bench3inst[120] = new Instruction(Opcode.LDI,9,29,0,1); // $9 = mem[sp + 1] (arg)
        bench3inst[121] = new Instruction(Opcode.LDI,8,29,0,0); // $8 = mem[sp + 0] (ra)
        bench3inst[122] = new Instruction(Opcode.MUL,2,2,9,0); // $2 *= arg
        bench3inst[123] = new Instruction(Opcode.ADDI,29,29,0,-2); // sp -= 2
        bench3inst[124] = new Instruction(Opcode.BRR,0,8,0,0); // return to ra
        //base case
        bench3inst[130] = new Instruction(Opcode.MOVC,2,0,0,1); // result register $2 = 1
        bench3inst[131] = new Instruction(Opcode.STI,2,29,0,1); // replace zero with 1
        bench3inst[132] = new Instruction(Opcode.BR,0,0,0,120); // go to pop

        //Many dependencies
        Instruction[] bench4inst = new Instruction[512];
        int[] bench4mem = new int[1024];
        bench4mem[5] = 1000;
        bench4mem[10] = 123;
        bench4mem[11] = bench4mem[10] * 8;
        bench4inst[0] = new Instruction(Opcode.MOVC,1,0,0,10); // $1 = 10
        bench4inst[1] = new Instruction(Opcode.MOVC,2,0,0,2); // $2 = 2
        bench4inst[2] = new Instruction(Opcode.DIV,3,1,2,0); // $3 = $1 / $2 = 5 (6 cycles)
        bench4inst[3] = new Instruction(Opcode.LD,10,3,0,0); // $10 = mem[ 5 + 0 ] (2 cycles, dependent:2)
        bench4inst[4] = new Instruction(Opcode.MUL,4,1,2,0); // $4 = $1 * $2 = 20 (2 cycles)
        bench4inst[5] = new Instruction(Opcode.SUB,6,3,1,0); // $6 = $3 - $1 = -5 (div dependent:2)
        bench4inst[6] = new Instruction(Opcode.ADD,5,1,2,0); // $5 = $1 + $2 = 12
        bench4inst[7] = new Instruction(Opcode.SHL,11,1,2,0); // $11 = $1 << $2 = 40
        bench4inst[8] = new Instruction(Opcode.STI,6,3,0,1); // mem[ 5 + 1 ] = $6 (2 cycles, dependent:2,5)
        bench4inst[9] = new Instruction(Opcode.ADDI,7,2,0,4); // $7 = $2 + 4 = 6
        bench4inst[10] = new Instruction(Opcode.LDI,8,0,0,10); // $8 = mem[10] (2 cycles)
        bench4inst[11] = new Instruction(Opcode.LDI,9,0,0,11); // $9 = mem[11] (2 cycles)
        bench4inst[12] = new Instruction(Opcode.DIV, 12,9,8,0); // $12 = $9 / $8 = 8 (6 cycles, dependent:10,11)
        bench4inst[13] = new Instruction(Opcode.DIVI,13,12,0,2); // $13 = $12 / 2 = 4 (6 cycles, dependent:12)
        bench4inst[14] = new Instruction(Opcode.NOT,14,0,0,0); // $14 = ¬ $0 = -1
        bench4inst[15] = new Instruction(Opcode.ADDI,1,1,0,20); // $1 = $1 + 20 = 30
        bench4inst[16] = new Instruction(Opcode.MUL,4,4,4,0); // $4 = $4 * $4 = 400 (2 cycles)
        bench4inst[17] = new Instruction(Opcode.ADD,2,2,1,0); // $2 = $2 + $1 = 32
        bench4inst[18] = new Instruction(Opcode.AND,11,14,1,0); // $11 = $14 & $1 = 30
        bench4inst[19] = new Instruction(Opcode.BR,0,0,0,50); // PC <- 50
        bench4inst[20] = new Instruction(Opcode.HALT,0,0,0,0); // HALT
        bench4inst[50] = new Instruction(Opcode.ADD,15,1,2,0); // $15 = $1 + $2 = 62
        bench4inst[51] = new Instruction(Opcode.STI,15,3,0,2); // mem[5 + 2] = $15
        bench4inst[52] = new Instruction(Opcode.BR,0,0,0,20); // jump back to halt

        // Independent Math
        Instruction[] bench5inst = new Instruction[512];
        int[] bench5mem = new int[1024];
//        for(int i = 0; i < 30; i++) {
//            bench5inst[i] = new Instruction(Opcode.ADDI,i,0,0,i);
//        }
//        bench5inst[30] = new Instruction(Opcode.HALT,0,0,0,0);
        bench5inst[0] = new Instruction(Opcode.NOOP,0,0,0,0); // no op
        bench5inst[1] = new Instruction(Opcode.MOVC,1,0,0,1); // $1 = 1
        bench5inst[2] = new Instruction(Opcode.MOVC,2,0,0,2); // $2 = 2
        bench5inst[3] = new Instruction(Opcode.ADDI,3,0,0,3); // $3 = $0 + 3 = 3
        bench5inst[4] = new Instruction(Opcode.ADDI,4,0,0,4); // $4 = $0 + 4 = 4
        bench5inst[5] = new Instruction(Opcode.SUB,5,0,0,5); // $5 = $0 - 5 = -5
        bench5inst[6] = new Instruction(Opcode.SHL,6,0,0,0); // $6 = $0 << $0 = 0
        bench5inst[7] = new Instruction(Opcode.SHR,7,0,0,0); // $7 = $0 >> $0 = 0
        bench5inst[8] = new Instruction(Opcode.NOT,8,0,0,0); // $8 = ~ $0 = -1
        bench5inst[9] = new Instruction(Opcode.AND,9,0,0,0); // $9 = $0 & $0 = 0
        bench5inst[10] = new Instruction(Opcode.OR,10,0,0,0); // $10 = $0 | $0 = 0
        bench5inst[11] = new Instruction(Opcode.NOOP,0,0,0,0); // no op
        bench5inst[12] = new Instruction(Opcode.MOVC,11,0,0,1); // $1 = 1
        bench5inst[13] = new Instruction(Opcode.MOVC,12,0,0,2); // $2 = 2
        bench5inst[14] = new Instruction(Opcode.ADDI,13,0,0,3); // $3 = $0 + 3 = 3
        bench5inst[15] = new Instruction(Opcode.ADDI,14,0,0,4); // $4 = $0 + 4 = 4
        bench5inst[16] = new Instruction(Opcode.SUB,15,0,0,5); // $5 = $0 - 5 = -5
        bench5inst[17] = new Instruction(Opcode.SHL,16,0,0,0); // $6 = $0 << $0 = 0
        bench5inst[18] = new Instruction(Opcode.SHR,17,0,0,0); // $7 = $0 >> $0 = 0
        bench5inst[19] = new Instruction(Opcode.NOT,18,0,0,0); // $8 = ~ $0 = -1
        bench5inst[20] = new Instruction(Opcode.AND,19,0,0,0); // $9 = $0 & $0 = 0
        bench5inst[21] = new Instruction(Opcode.OR,20,0,0,0); // $10 = $0 | $0 = 0
        bench5inst[450] = new Instruction(Opcode.HALT,0,0,0,0); // halt

        //Independent memory load
        Instruction[] bench6Inst = new Instruction[512];
        int[] bench6mem = new int[1024];
        for(int i = 0; i < 64; i++) {
            bench6mem[i] = i; // mem[i] = i
            bench6Inst[i] = new Instruction(Opcode.LDI,i % 32,0,0,i); // $i <- mem[i]
        }
        bench6Inst[64] = new Instruction(Opcode.HALT,0,0,0,0);

        System.out.println("Benchmark1 - Vector addition (size: " + length + ")");
        Processor9 bench1 = new Processor9(bench1mem,bench1inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
        bench1.RunProcessor();
        createDump(bench1.mem, "mem_bench1.txt");
        createDump(bench1.rf,"rf_bench1.txt");

        System.out.println("Benchmark2 - Bubble sort (size: " + arrayToSort.length + ")");
        Processor9 bench2 = new Processor9(bench2mem,bench2inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
        bench2.RunProcessor();
        createDump(bench2.mem, "mem_bench2.txt");
        createDump(bench2.rf,"rf_bench2.txt");

        System.out.println("Benchmark3 - Factorial(" + num + ")");
	    Processor9 bench3 = new Processor9(bench3mem,bench3inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
	    bench3.RunProcessor();
	    createDump(bench3.mem, "mem_bench3.txt");
	    createDump(bench3.rf,"rf_bench3.txt");

	    System.out.println("Benchmark4 - many dependencies");
	    Processor9 bench4 = new Processor9(bench4mem,bench4inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
	    bench4.RunProcessor();
	    createDump(bench4.mem, "mem_bench4.txt");
	    createDump(bench4.rf,"rf_bench4.txt");

        System.out.println("Benchmark5 - Independent Math");
        Processor9 bench5 = new Processor9(bench5mem,bench5inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
        bench5.RunProcessor();
        createDump(bench5.mem, "mem_bench5.txt");
        createDump(bench5.rf,"rf_bench5.txt");

//        System.out.println("Benchmark6 - Independent Load");
//        Processor9 bench6 = new Processor9(bench6mem,bench6Inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU);
//        bench6.RunProcessor();
//        createDump(bench6.mem,"mem_bench6.txt");
//        createDump(bench6.rf,"rf_bench6.txt");

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
