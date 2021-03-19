import java.util.LinkedList;
import java.util.Queue;

public class Processor4 {

    public Processor4(int[] mem, Instruction[] instructions) {
        this.mem = mem;
        this.instructions = instructions;
    }

    int cycle = 0;
    int pc = 0; //Program counter
    int executedInsts = 0; //Number of instructions executed
    int executeCycle = 0; // cycles that was spent at execution phase
    int stalledCycle = 0; // cycles that was spent while doing nothing
    int[] mem; // memory from user
    int[] rf = new int[65]; //Register file (physical)
    // register 0 always have value zero ($zero, input is ignored)
    // $32 is Program counter for users ($pc)
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    int QUEUE_SIZE = 4;
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    Queue<ExecutionResult> executionResults = new LinkedList<>();

    // execution result register
    ExecutionResult execResult;
    // final result registers before write back
    Integer finalData = null;
    Integer finalAddress = null;
    // state of phases
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean executeBlocked = false;

    // Execution units
    ALU alu0 = new ALU();
    ALU alu1 = new ALU();
    LSU lsu0 = new LSU();
    BRU bru0 = new BRU();
    
    Integer alu0ResultAddress = null;
    Integer alu1ResultAddress = null;

    private void Fetch() {
        if(fetchedQueue.size() < QUEUE_SIZE && pc < mem.length) {
            Instruction ins = instructions[pc];
            if(ins == null) {
                ins = new Instruction(); // NOOP
            }
            fetchedQueue.add(ins);
            pc++;
        }
    }

    private void Decode() {
        if(decodedQueue.size() < QUEUE_SIZE && !fetchedQueue.isEmpty()) {
            Instruction decoded = fetchedQueue.remove();
            decodedQueue.add(decoded);
        }
    }

    private boolean checkIfExecutable(Instruction ins) {
        switch (ins.opcode) {
            case ADD:
            case ADDI:
            case SUB:
            case MUL:
            case MULI:
            case DIV:
            case DIVI:
            case NOT:
            case AND:
            case OR:
            case CMP:
            case MOV:
            case MOVC:
            case SHL:
            case SHR:
                return (!alu0.busy || !alu1.busy);
            case HALT:
                return (!alu0.busy && !alu1.busy);
            default: return true;
        }
    }

    private void Execute() {
        if (!decodedQueue.isEmpty() && executionResults.size() <= QUEUE_SIZE) {
            Instruction executing = decodedQueue.peek();
            int input1 = 0;
            int input2 = 0;
            Integer alu0_result = null;
            Integer alu1_result = null;
            Integer memAddress = null;
//            switch (executing.opcode) {
//                case NOOP:
//                    rf[32]++;
//                    break;
//                case HALT:
//                    if(!alu0.busy && !alu1.busy) {
//                        finished = true;
//                        decodedQueue.remove();
//                    }
//                    break;
//                case ADD: // ALU OPs that use rf[Rs1] and rf[Rs2]
//                case SUB:
//                case MUL:
//                case DIV:
//                case CMP:
//                case AND:
//                case OR:
//                case SHL:
//                case SHR:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
//                    rf[32]++;
//                    break;
//                case ADDI: // ALU OPs that use rf[Rs1] and Const
//                case MULI:
//                case DIVI:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = executing.Const;
//                    rf[32]++;
//                    break;
//                case NOT: // ALU OPs that only use rf[Rs1]
//                case MOV:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    rf[32]++;
//                    break;
//                case MOVC: // ALU OPs that only use Const
//                    input1 = executing.Const;
//                    rf[32]++;
//                    break;
//                case LD: // rf[Rs1] + rf[Rs2]
//                case ST:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
////                    memoryOpcode = executing.opcode;
////                    memoryRd = executing.Rd;
////                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
//                    rf[32]++;
//                    break;
//                case LDI: // Const
//                case STI:
//                    input1 = executing.Const;
////                    memoryOpcode = executing.opcode;
////                    memoryRd = executing.Rd;
////                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
//                    rf[32]++;
//                    break;
//                case LDO: // rf[Rs1] + Const
//                case STO:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = executing.Const;
////                    memoryOpcode = executing.opcode;
////                    memoryRd = executing.Rd;
////                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
//                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
//                    rf[32]++;
//                    break;
//                case BR: // Unconditional branch
//                case JMP:
//                case JR:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
//                    rf[32] = pc = bru0.evaluateBranchTarget(executing.opcode, rf[32], input1, input2, executing.Const);
//                    fetchedQueue.clear();
//                    decodedQueue.clear();
//                    break;
//                case BEQ: // Conditional branch
//                case BLT:
//                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
//                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
//                    if (bru0.evaluateBranchCondition(executing.opcode, input1, input2)) {
//                        rf[32] = pc = bru0.evaluateBranchTarget(executing.opcode, rf[32], input1, input2, executing.Const);
//                        fetchedQueue.clear();
//                        decodedQueue.clear();
//                    } else {
//                        rf[32]++;
//                    }
//                    break;
//                default:
//                    System.out.println("Invalid instruction exception");
//                    finished = true;
//                    break;
//            }

            switch (executing.opcode) { // Not using execution unit case
                case NOOP:
                    rf[32]++;
                    break;
                case HALT:
                    if(!alu0.busy && !alu1.busy) {
                        finished = true;
                        decodedQueue.remove();
                    }
                    break;
                default:
                    break;
            }

            switch (executing.opcode) { // ALU using case
                case ADD: // ALU OPs that use rf[Rs1] and rf[Rs2]
                case SUB:
                case MUL:
                case DIV:
                case CMP:
                case AND:
                case OR:
                case SHL:
                case SHR:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
                    rf[32]++;
                    break;
                case ADDI: // ALU OPs that use rf[Rs1] and Const
                case MULI:
                case DIVI:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = executing.Const;
                    rf[32]++;
                    break;
                case NOT: // ALU OPs that only use rf[Rs1]
                case MOV:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    rf[32]++;
                    break;
                case MOVC: // ALU OPs that only use Const
                    input1 = executing.Const;
                    rf[32]++;
                    break;
                default:
                    break;
            }

            switch (executing.opcode) {
                case LD: // rf[Rs1] + rf[Rs2]
                case ST:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
//                    memoryOpcode = executing.opcode;
//                    memoryRd = executing.Rd;
//                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
                    rf[32]++;
                    break;
                case LDI: // Const
                case STI:
                    input1 = executing.Const;
//                    memoryOpcode = executing.opcode;
//                    memoryRd = executing.Rd;
//                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
                    rf[32]++;
                    break;
                case LDO: // rf[Rs1] + Const
                case STO:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = executing.Const;
//                    memoryOpcode = executing.opcode;
//                    memoryRd = executing.Rd;
//                    memoryAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    memAddress = lsu0.evaluate(executing.opcode, input1, input2);
                    executionResults.add(new ExecutionResult(executing.opcode,executing.Rd,memAddress));
                    rf[32]++;
                    break;
                default:
                    break;
            }

            switch (executing.opcode) {
                case BR: // Unconditional branch
                case JMP:
                case JR:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
                    rf[32] = pc = bru0.evaluateTarget(executing.opcode, rf[32], input1, input2, executing.Const);
                    fetchedQueue.clear();
                    decodedQueue.clear();
                    break;
                case BEQ: // Conditional branch
                case BLT:
                    input1 = resultForwarding(executing.Rs1, finalData, finalAddress);
                    input2 = resultForwarding(executing.Rs2, finalData, finalAddress);
                    if (bru0.evaluateCondition(executing.opcode, input1, input2)) {
                        rf[32] = pc = bru0.evaluateTarget(executing.opcode, rf[32], input1, input2, executing.Const);
                        fetchedQueue.clear();
                        decodedQueue.clear();
                    } else {
                        rf[32]++;
                    }
                    break;
                default:
                    break;
            }

            if (!alu0.busy) {
                alu0.update(executing.opcode, input1, input2);
                alu0ResultAddress = executing.Rd;
                decodedQueue.remove();
            } else if (!alu1.busy) {
                alu1.update(executing.opcode, input1, input2);
                alu1ResultAddress = executing.Rd;
                decodedQueue.remove();
            }
            alu0_result = alu0.execute();
            alu1_result = alu1.execute();
            if(alu0_result != null) {
                executionResults.add(new ExecutionResult(alu0_result, alu0ResultAddress));
                alu0.reset();
                alu0ResultAddress = null;
            }
            if(alu1_result != null) {
                executionResults.add(new ExecutionResult(alu1_result, alu1ResultAddress));
                alu1.reset();
                alu1ResultAddress = null;
            }
        }
    }

    private void Memory() {
        if(!executionResults.isEmpty()) {
            ExecutionResult result = executionResults.remove();
            if(result.memOp != null && result.memRd != null && result.memAddress != null) { // memory instructions
                switch (result.memOp) {
                    case LD:
                    case LDI:
                    case LDO:
                        finalData = mem[result.memAddress];
                        finalAddress = result.memRd;
                        break;
                    case ST:
                    case STI:
                    case STO:
                        mem[result.memAddress] = rf[result.memRd];
                        break;
                }
            }
            else { // non-memory instructions, skip the mem process
                if(result.execData != null && result.execAddress != null) {
                    finalData = result.execData;
                    finalAddress = result.execAddress;
                }
            }
        }
    }

    private void WriteBack() {
        if(finalData != null && finalAddress != null && finalAddress != 0 && finalAddress != 32) {
            rf[finalAddress] = finalData;
        }
        finalData = null;
        finalAddress = null;
    }

    private Integer resultForwarding(Integer insAddress, Integer forData, Integer forAddress) {
        if(forAddress != null && forAddress.equals(insAddress)) {
            return forData;
        }
        return rf[insAddress];
    }

    public void RunProcessor() {

        while(!finished && pc < instructions.length) {
            WriteBack();
            Memory();
            Execute();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || (executeBlocked && executeCycle > 1)) {
                stalledCycle++;
            }
        }
        WriteBack(); // Writing back last data
        cycle++;
        System.out.println("Scalar pipelined (5-stage) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }
}
