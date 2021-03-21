import java.util.Arrays;
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
    boolean[] validBits = new boolean[65]; // simple scoreboard
    // register 0 always have value zero ($zero, input is ignored)
    // $32 is Program counter for users ($pc)
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    int QUEUE_SIZE = 4;
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    Queue<MicroOperation> dispatchedQueue = new LinkedList<>();
    Queue<ExecutionResult> executionResults = new LinkedList<>();

    // final result registers before write back
    Integer finalData = null;
    Integer finalAddress = null;
    // state of phases
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean dispatchBlocked = false;
    boolean executeBlocked = false;

    // Execution units
    ALU alu0 = new ALU();
    ALU alu1 = new ALU();
    LSU lsu0 = new LSU();
    BRU bru0 = new BRU();
    
    Integer alu0ResultAddress = null;
    Integer alu1ResultAddress = null;

    Integer lsu0Rd = null;

    private void Fetch() {
        fetchBlocked = fetchedQueue.size() >= QUEUE_SIZE;
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
        decodeBlocked = decodedQueue.size() >= QUEUE_SIZE;
        if(decodedQueue.size() < QUEUE_SIZE && !fetchedQueue.isEmpty()) {
            Instruction decoded = fetchedQueue.remove();
            decodedQueue.add(decoded);
        }
    }

    private void Dispatch() {
        dispatchBlocked = dispatchedQueue.size() >= QUEUE_SIZE;
        Instruction beforeDispatch = decodedQueue.peek();
        if(dispatchedQueue.size() < QUEUE_SIZE && !decodedQueue.isEmpty() && ((validBits[beforeDispatch.Rs1] || beforeDispatch.Rs1 == 0) && (validBits[beforeDispatch.Rs2] || beforeDispatch.Rs2 == 0))) {
            Instruction dispatching = decodedQueue.remove();
            MicroOperation mop = new MicroOperation();
            mop.opcode = dispatching.opcode;
            int input1;
            int input2;
            int input3;
            switch (dispatching.opcode) {
                case NOOP:
                case HALT:
                    break;
                case ADD: // ALU OPs that use rf[Rs1] and rf[Rs2]
                case SUB:
                case MUL:
                case DIV:
                case CMP:
                case AND:
                case OR:
                case SHL:
                case SHR:
                case LD: // LSU ops using rf[Rs1] + rf[Rs2]
                case ST:
                    input1 = resultForwarding2(dispatching.Rs1);
                    input2 = resultForwarding2(dispatching.Rs2);
                    mop = new MicroOperation(dispatching.opcode,dispatching.Rd,input1,input2);
                    break;
                case ADDI: // ALU OPs that use rf[Rs1] and Const
                case MULI:
                case DIVI:
                case LDO: // LSU ops using rf[Rs1] + Const
                case STO:
                    input1 = resultForwarding2(dispatching.Rs1);
                    input2 = dispatching.Const;
                    mop = new MicroOperation(dispatching.opcode,dispatching.Rd,input1,input2);
                    break;
                case NOT: // ALU OPs that only use rf[Rs1]
                case MOV:
                    input1 = resultForwarding2(dispatching.Rs1);
                    mop = new MicroOperation(dispatching.opcode,dispatching.Rd,input1,0);
                    break;
                case MOVC: // ALU OPs that only use Const
                case LDI: // LSU ops only using Const
                case STI:
                    mop = new MicroOperation(dispatching.opcode,dispatching.Rd,dispatching.Const,0);
                    break;
                case BR: // Unconditional branch (Branch using Rs1, Rs2 and Const)
                case JMP:
                case JR:
                case BEQ: // Conditional branch
                case BLT:
                    input1 = resultForwarding2(dispatching.Rs1);
                    input2 = resultForwarding2(dispatching.Rs2);
                    input3 = dispatching.Const;
                    mop = new MicroOperation(dispatching.opcode,0,input1,input2,input3);
                    break;
                default:
                    System.out.println("Invalid instruction exception");
                    finished = true;
                    break;
            }
            validBits[dispatching.Rd] = false;
            dispatchedQueue.add(mop);
        }
    }

    private void Execute() {
        executeBlocked = executionResults.size() >= QUEUE_SIZE || (alu0.busy && alu1.busy && lsu0.busy);
        if(!dispatchedQueue.isEmpty() && executionResults.size() < QUEUE_SIZE) {
            MicroOperation executing = dispatchedQueue.peek();
            switch (executing.opcode) { //Updating executing micro ops
                case HALT:
                    finished = true;
                    break;
                case ADD: // ALU OPs that use rf[Rs1] and rf[Rs2]
                case SUB:
                case MUL:
                case DIV:
                case CMP:
                case AND:
                case OR:
                case SHL:
                case SHR:
                case ADDI: // ALU OPs that use rf[Rs1] and Const
                case MULI:
                case NOT: // ALU OPs that only use rf[Rs1]
                case MOV:
                case MOVC: // ALU OPs that only use Const
                    if (!alu0.busy) {
                        alu0.update(executing.opcode, executing.input1, executing.input2);
                        alu0ResultAddress = executing.Rd;
                        dispatchedQueue.remove();
                    } else if (!alu1.busy) {
                        alu1.update(executing.opcode, executing.input1, executing.input2);
                        alu1ResultAddress = executing.Rd;
                        dispatchedQueue.remove();
                    }
                    break;
                case LD:
                case LDO:
                case LDI:
                case ST:
                case STO:
                case STI:
                    if(!lsu0.busy) {
                        lsu0.update(executing.opcode, executing.input1, executing.input2);
                        lsu0Rd = executing.Rd;
                        dispatchedQueue.remove();
                    }
                    break;
                case BR: // Unconditional branch (Branches executed by BRU immediately)
                case JMP:
                case JR:
                    dispatchedQueue.remove();
                    rf[32] = pc = bru0.evaluateTarget(executing.opcode, rf[32], executing.input1, executing.input2, executing.input3);
                    fetchedQueue.clear();
                    decodedQueue.clear();
                    dispatchedQueue.clear();
                    executedInsts++;
                    break;
                case BEQ: // Conditional branch
                case BLT:
                    dispatchedQueue.remove();
                    if (bru0.evaluateCondition(executing.opcode, executing.input1, executing.input2)) {
                        rf[32] = pc = bru0.evaluateTarget(executing.opcode, rf[32], executing.input1, executing.input2, executing.input3);
                        fetchedQueue.clear();
                        decodedQueue.clear();
                        dispatchedQueue.clear();
                    } else {
                        rf[32]++;
                    }
                    executedInsts++;
                    break;
            }
            // ALUs and LSU works at here
            Integer alu0_result = alu0.execute();
            Integer alu1_result = alu1.execute();
            Integer memAddress = lsu0.execute();
            if(alu0_result != null) {
                executionResults.add(new ExecutionResult(alu0_result, alu0ResultAddress));
                alu0.reset();
                alu0ResultAddress = null;
                rf[32]++;
                executedInsts++;
            }
            if(alu1_result != null) {
                executionResults.add(new ExecutionResult(alu1_result, alu1ResultAddress));
                alu1.reset();
                alu1ResultAddress = null;
                rf[32]++;
                executedInsts++;
            }
            if(memAddress != null) {
                ExecutionResult result = new ExecutionResult(lsu0.op,lsu0Rd,memAddress);
                result.execData = resultForwarding(lsu0Rd,finalData,finalAddress);
                executionResults.add(result);
                lsu0.reset();
                lsu0Rd = null;
                rf[32]++;
                executedInsts++;
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
                        mem[result.memAddress] = result.execData;
                        validBits[result.memRd] = true;
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
            validBits[finalAddress] = true;
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

    private int resultForwarding2(Integer source) {
        for(ExecutionResult result : executionResults) {
            if(result.execAddress != null && result.execAddress.equals(source)) {
                return result.execData;
            }
        }
        return rf[source];
    }

    private Integer resultForwarding3(Integer insAddress, Integer forData, Integer forAddress) {
        if(forAddress != null && forAddress.equals(insAddress)) {
            return forData;
        }
        return insAddress;
    }

    public void RunProcessor() {
        Arrays.fill(validBits, true);

        while(!finished && pc < instructions.length) {
            WriteBack();
            Memory();
            Execute();
            Dispatch();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || dispatchBlocked || executeBlocked) {
                stalledCycle++;
            }
//            System.out.println("PC: "+ pc + " rf[32]: " + rf[32]);
        }
        WriteBack(); // Writing back last data
        cycle++;
        System.out.println("Scalar pipelined (6-stage) processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }
}
