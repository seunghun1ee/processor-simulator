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
        int RS_SIZE = 64;
        if(argValues[0] > 0) {
            superScalarWidth = argValues[0];
        }
        if(argValues[1] > 0 && argValues[1] <= 5) {
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
        if(argValues[6] > 0) {
            RS_SIZE = argValues[6];
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
        bench1inst[0] = new Instruction(Opcode.MOVC,1,0,0,ap); // pointer to array A
        bench1inst[1] = new Instruction(Opcode.MOVC,2,0,0,bp); // pointer to array B
        bench1inst[2] = new Instruction(Opcode.MOVC,3,0,0,cp); // pointer to array C
        bench1inst[3] = new Instruction(Opcode.MOVC,4,0,0,0); // i = 0
        bench1inst[4] = new Instruction(Opcode.MOVC,8,0,0,length); // for loop limit
        bench1inst[5] = new Instruction(Opcode.LD,5,1,4,0); // $5 = mem[&A + i], for loop starts
        bench1inst[6] = new Instruction(Opcode.LD,6,2,4,0); //$6 = mem[&B + i]
        bench1inst[7] = new Instruction(Opcode.ADD,7,5,6,0); // $7 = $5 + $6
        bench1inst[8] = new Instruction(Opcode.ST,7,3,4,0); // mem[&C + i] = $7
        bench1inst[9] = new Instruction(Opcode.ADDI,4,4,0,1); // i++
        bench1inst[10] = new Instruction(Opcode.CMP,10,4,8,0); // $10 = cmp(i,$8)
        bench1inst[11] = new Instruction(Opcode.BRN,0,10,0,5); //branch back to for loop if i < 10
        bench1inst[12] = new Instruction(Opcode.HALT,0,0,0,0); //Terminate

        //Bubble sort
        Instruction[] bench2inst = new Instruction[512];
        int[] bench2mem = new int[1024];

        int[] arrayToSort = {512,52,61,3,-6,-127,75,21,98,1,874,-1239,431,94,10,36};
        int pointer = 2;
        System.arraycopy(arrayToSort,0,bench2mem,pointer,arrayToSort.length);
        bench2inst[0] = new Instruction(Opcode.MOVC,1,0,0,pointer); // load array pointer
        bench2inst[1] = new Instruction(Opcode.MOVC,2,0,0,0); // i = 0
        bench2inst[2] = new Instruction(Opcode.MOVC,4,0,0, arrayToSort.length); // load array length
        bench2inst[3] = new Instruction(Opcode.ADDI,5,4,0,-1); // outer for loop limit = length - 1
        bench2inst[4] = new Instruction(Opcode.SUB,6,4,2,0); // length - i, outer loop starting point
        bench2inst[5] = new Instruction(Opcode.MOVC,3,0,0,0); // j = 0
        bench2inst[6] = new Instruction(Opcode.ADDI,6,6,0,-1); //inner for loop limit = length - i - 1
        bench2inst[7] = new Instruction(Opcode.LD,7,1,3,0); // a = array[j], inner loop starting point (WAR: 16)
        bench2inst[8] = new Instruction(Opcode.ADD,9,1,3,0); // pointer + j
        bench2inst[9] = new Instruction(Opcode.LDI,8,9,0,1); // b = array[j + 1] (WAR: 17)
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
        bench2inst[22] = new Instruction(Opcode.CMP,11,2,5,0); // $11 = cmp(i, length - 1) (WAW hazard: 19)
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
        bench3inst[100] = new Instruction(Opcode.LDI,13,29,0,1); // $13 = mem[sp + 1] (arg) (WAR hazard: 107)
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
        bench4inst[0] = new Instruction(Opcode.MOVC,1,0,0,10); // First loop starting point, $1 = 10
        bench4inst[1] = new Instruction(Opcode.MOVC,2,0,0,2); // $2 = 2
        bench4inst[2] = new Instruction(Opcode.DIV,3,1,2,0); // $3 = $1 / $2 = 5 (16 cycles)
        bench4inst[3] = new Instruction(Opcode.LD,10,3,0,0); // $10 = mem[ 5 + 0 ] (2 cycles, dependent:2)
        bench4inst[4] = new Instruction(Opcode.MUL,4,1,2,0); // $4 = $1 * $2 = 20 (2 cycles)
        bench4inst[5] = new Instruction(Opcode.SUB,6,3,1,0); // $6 = $3 - $1 = -5 (div dependent:2)
        bench4inst[6] = new Instruction(Opcode.ADD,5,1,2,0); // $5 = $1 + $2 = 12
        bench4inst[7] = new Instruction(Opcode.SHL,11,5,2,0); // $11 = $5 << $2 = 48 (dependent: 6)
        bench4inst[8] = new Instruction(Opcode.STI,6,3,0,1); // mem[ 5 + 1 ] = $6 (2 cycles, dependent:2,5)
        bench4inst[9] = new Instruction(Opcode.ADDI,6,2,0,4); // $6 = $2 + 4 = 6 (WAR: 8)
        bench4inst[10] = new Instruction(Opcode.LDI,8,0,0,10); // $8 = mem[10] (2 cycles)
        bench4inst[11] = new Instruction(Opcode.LDI,9,0,0,11); // $9 = mem[11] (2 cycles)
        bench4inst[12] = new Instruction(Opcode.DIV, 12,9,8,0); // $12 = $9 / $8 = 8 (16 cycles, dependent:10,11)
        bench4inst[13] = new Instruction(Opcode.DIVI,13,12,0,2); // $13 = $12 / 2 = 4 (16 cycles, dependent:12)
        bench4inst[14] = new Instruction(Opcode.NOT,14,0,0,0); // $14 = ¬ $0 = -1
        bench4inst[15] = new Instruction(Opcode.DIVI,20,9,0,8); // $20 = $9 / 8 (16 cycles, dependent:11)
        bench4inst[16] = new Instruction(Opcode.ADDI,22,22,0,1); // $22 += 1
        bench4inst[17] = new Instruction(Opcode.MUL,4,4,4,0); // $4 = $4 * $4 = 400 (2 cycles)
        bench4inst[18] = new Instruction(Opcode.SHR,21,20,2,0); // $21 = $20 >> $2 (dependent: 15)
        bench4inst[19] = new Instruction(Opcode.ADD,2,2,1,0); // $2 = $2 + $1 = 12
        bench4inst[20] = new Instruction(Opcode.AND,11,14,1,0); // $11 = $14 & $1 = 30
        bench4inst[21] = new Instruction(Opcode.ADDI,30,30,0,1); // $30 += 1
        bench4inst[22] = new Instruction(Opcode.ST,22,30,0,0); // mem[ $30 ] = $22 (dependent: 16, 21)
        bench4inst[23] = new Instruction(Opcode.CMP,31,22,1,0); // $31 = cmp($22,$1) (dep: 16)
        bench4inst[24] = new Instruction(Opcode.BRN,0,31,0,50); // branch to second loop
        bench4inst[25] = new Instruction(Opcode.NOOP,0,0,0,0);
        bench4inst[26] = new Instruction(Opcode.NOOP,0,0,0,0);
        bench4inst[27] = new Instruction(Opcode.HALT,0,0,0,0); // HALT
        bench4inst[50] = new Instruction(Opcode.ADDI,18,18,0,2); // Second loop starting point, $18 += 2
        bench4inst[51] = new Instruction(Opcode.NOOP,0,0,0,0);
        bench4inst[52] = new Instruction(Opcode.AND,19,18,0,0); // $19 = $18 & $0 = 0
        bench4inst[53] = new Instruction(Opcode.CMP,29,18,1,0); // $29 = cmp($18,$1) (dep: 50, 52)
        bench4inst[54] = new Instruction(Opcode.BRN,0,29,0,50); // branch to second loop
        bench4inst[55] = new Instruction(Opcode.MOVC,18,0,0,0); // $18 = 0
        bench4inst[56] = new Instruction(Opcode.BR,0,0,0,0); // branch to first loop

        // Independent Math
        Instruction[] bench5inst = new Instruction[512];
        int[] bench5mem = new int[1024];
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

        // 3 x 3 Game of life 1 iteration
        Instruction[] bench6inst = new Instruction[512];
        int[] bench6mem = new int[1024];
        int old_pointer = 6;
        int new_pointer = 31;
        // Glider (expected result: 1 at mem[36], mem[38], mem[42] and mem[43])
        bench6mem[old_pointer] = 0;
        bench6mem[old_pointer + 1] = 1;
        bench6mem[old_pointer + 2] = 0;
        bench6mem[old_pointer + 5] = 0;
        bench6mem[old_pointer + 6] = 0;
        bench6mem[old_pointer + 7] = 1;
        bench6mem[old_pointer + 10] = 1;
        bench6mem[old_pointer + 11] = 1;
        bench6mem[old_pointer + 12] = 1;
        //gol_prep
        bench6inst[0] = new Instruction(Opcode.MOVC,1,0,0,3); // loop limit
        bench6inst[1] = new Instruction(Opcode.MOVC,2,0,0,old_pointer);
        bench6inst[2] = new Instruction(Opcode.MOVC,3,0,0,new_pointer);
        bench6inst[3] = new Instruction(Opcode.MOVC,29,0,0,1); // cell
        bench6inst[4] = new Instruction(Opcode.MOVC,30,0,0,2); // low limit
        bench6inst[5] = new Instruction(Opcode.MOVC,31,0,0,3); // high limit
        //gol_logic
        bench6inst[6] = new Instruction(Opcode.MOVC,4,0,0,0); // row
        bench6inst[7] = new Instruction(Opcode.MOVC,5,0,0,0); // col, logic_outer_loop_start
        bench6inst[8] = new Instruction(Opcode.MULI,6,4,0,5); // logic_inner_loop_start
        bench6inst[9] = new Instruction(Opcode.ADD,6,6,5,0); // offset
        bench6inst[10] = new Instruction(Opcode.ADD,13,6,2,0); // old cell address
        bench6inst[11] = new Instruction(Opcode.LDI,7,13,0,0); // get old cell
        bench6inst[12] = new Instruction(Opcode.MOV,20,13,0,0); // pass arg to count
        bench6inst[13] = new Instruction(Opcode.BR,0,0,0,400); // call count, $26 is return value
        bench6inst[14] = new Instruction(Opcode.SUB,8,26,7,0); // logic return point, $8 is num of neighbour
        bench6inst[15] = new Instruction(Opcode.MOV,14,6,0,0); // pass arg for updating (offset)
        bench6inst[16] = new Instruction(Opcode.CMP,9,0,7,0); // $9 is cell state (-1: alive, 0:dead)
        bench6inst[17] = new Instruction(Opcode.BRN,0,9,0,100); // call alive_case
        bench6inst[18] = new Instruction(Opcode.CMP,9,31,8,0); // $10 is 0 when cell is dead and has exactly 3 neighbours (WAW: 16)
        bench6inst[19] = new Instruction(Opcode.BRZ,0,9,0,300); // call reproduce_case
        bench6inst[20] = new Instruction(Opcode.ADDI,5,5,0,1); // col++, comeback_point
        bench6inst[21] = new Instruction(Opcode.CMP,9,5,1,0); // compare col with 3 (WAW: 18)
        bench6inst[22] = new Instruction(Opcode.BRN,0,9,0,8); // branch back to logic_inner_loop_start
        bench6inst[23] = new Instruction(Opcode.ADDI,4,4,0,1); // row++
        bench6inst[24] = new Instruction(Opcode.CMP,12,4,1,0); // compare row with 3
        bench6inst[25] = new Instruction(Opcode.BRN,0,12,0,7); // branch back to logic_outer_loop_start
        bench6inst[26] = new Instruction(Opcode.NOOP,0,0,0,0);
        bench6inst[27] = new Instruction(Opcode.HALT,0,0,0,0); // HALT
        //alive case
        bench6inst[100] = new Instruction(Opcode.CMP,15,8,30,0); // compare number of neighbours with 2
        bench6inst[101] = new Instruction(Opcode.BRN,0,15,0,200); // call dead_case
        bench6inst[102] = new Instruction(Opcode.CMP,15,31,8,0); // compare 3 with number of neighbours (WAW: 100)
        bench6inst[103] = new Instruction(Opcode.BRN,0,15,0,200); //call dead_case
        bench6inst[104] = new Instruction(Opcode.BR,0,0,0,300); // call reproduce_case
        //dead case
        bench6inst[200] = new Instruction(Opcode.ST,0,3,14,0); // store new dead cell
        bench6inst[201] = new Instruction(Opcode.BR,0,0,0,20); // branch back to comeback_point
        //reproduce case
        bench6inst[300] = new Instruction(Opcode.ST,29,3,14,0); // store new alive cell
        bench6inst[301] = new Instruction(Opcode.BR,0,0,0,20); // branch back to comeback_point
        //count
        bench6inst[400] = new Instruction(Opcode.ADDI,21,20,0,-6); // $21 is left top corner neighbour pointer
        bench6inst[401] = new Instruction(Opcode.MOVC,26,0,0,0); // counter reset
        bench6inst[402] = new Instruction(Opcode.MOVC,22,0,0,0); // row
        bench6inst[403] = new Instruction(Opcode.MOVC,23,0,0,0); // col, outer_loop_start
        bench6inst[404] = new Instruction(Opcode.MULI,24,22,0,5); // row * 5, inner loop start
        bench6inst[405] = new Instruction(Opcode.ADD,24,24,23,0); // col + row * 5
        bench6inst[406] = new Instruction(Opcode.ADD,24,24,21,0); // $24 is neighbour address
        bench6inst[407] = new Instruction(Opcode.LDI,25,24,0,0); // $25 is neighbour
        bench6inst[408] = new Instruction(Opcode.ADD,26,26,25,0); // counter += neighbour
        bench6inst[409] = new Instruction(Opcode.ADDI,23,23,0,1); // col++
        bench6inst[410] = new Instruction(Opcode.CMP,27,23,1,0); // compare col with 3
        bench6inst[411] = new Instruction(Opcode.BRN,0,27,0,404); // branch back to inner loop
        bench6inst[412] = new Instruction(Opcode.ADDI,22,22,0,1); // row++
        bench6inst[413] = new Instruction(Opcode.CMP,27,22,1,0); // compare row with 3 (WAW: 410)
        bench6inst[414] = new Instruction(Opcode.BRN,0,27,0,403); // branch back to outer loop
        bench6inst[415] = new Instruction(Opcode.BR,0,0,0,14); // branch back to logic_return_point

        System.out.println("Benchmark1 - Vector addition (size: " + length + ")");
        Processor9 bench1 = new Processor9(bench1mem,bench1inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
        bench1.RunProcessor();
        createDump(bench1.mem, "mem_bench1.txt");
        createDump(bench1.rf,"rf_bench1.txt");

        System.out.println("Benchmark2 - Bubble sort (size: " + arrayToSort.length + ")");
        Processor9 bench2 = new Processor9(bench2mem,bench2inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
        bench2.RunProcessor();
        createDump(bench2.mem, "mem_bench2.txt");
        createDump(bench2.rf,"rf_bench2.txt");

        System.out.println("Benchmark3 - Factorial(" + num + ")");
	    Processor9 bench3 = new Processor9(bench3mem,bench3inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
	    bench3.RunProcessor();
	    createDump(bench3.mem, "mem_bench3.txt");
	    createDump(bench3.rf,"rf_bench3.txt");

	    System.out.println("Benchmark4 - many dependencies");
	    Processor9 bench4 = new Processor9(bench4mem,bench4inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
	    bench4.RunProcessor();
	    createDump(bench4.mem, "mem_bench4.txt");
	    createDump(bench4.rf,"rf_bench4.txt");

        System.out.println("Benchmark5 - Independent Math");
        Processor9 bench5 = new Processor9(bench5mem,bench5inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
        bench5.RunProcessor();
        createDump(bench5.mem, "mem_bench5.txt");
        createDump(bench5.rf,"rf_bench5.txt");

        System.out.println("Benchmark6 - 3 x 3 Game of life 1 iteration");
        Processor9 bench6 = new Processor9(bench6mem,bench6inst,superScalarWidth,branchMode,numOfALU,numOfLOAD,numOfSTORE,numOfBRU,RS_SIZE);
        bench6.RunProcessor();
        createDump(bench6.mem,"mem_bench6.txt");
        createDump(bench6.rf,"rf_bench6.txt");
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
