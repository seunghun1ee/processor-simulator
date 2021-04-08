import java.io.IOException;
import java.util.*;

public class Processor5 {

    public Processor5(int[] mem, Instruction[] instructions) {
        this.mem = mem;
        this.instructions = instructions;
    }

    int cycle = 0;
    int pc = 0; // Program counter
    int executedInsts = 0; // Number of instructions executed
    int stalledCycle = 0;
    int insIdCount = 1; // for assigning id to instructions
    int[] mem; // memory from user
    int[] rf = new int[64]; //Register file (physical)
    boolean[] validBits = new boolean[rf.length]; // simple scoreboard
    // register 0 always have value zero ($zero, input is ignored)
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    int QUEUE_SIZE = 4;
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    Queue<Instruction> reservationStations = new LinkedList<>(); // unified
    Queue<Instruction> executionResults = new LinkedList<>();

    // final result registers before write back
    Instruction beforeWriteBack;

    // state of pipeline stages
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean issueBlocked = false;
    boolean executeBlocked = false;
    boolean euAllBusy = false;

    // Execution units
    ALU alu0 = new ALU();
    ALU alu1 = new ALU();
    LSU lsu0 = new LSU();
    BRU bru0 = new BRU();

    //For visualisation
    List<Instruction> finishedInsts = new ArrayList<>();
    List<Probe> probes = new ArrayList<>();

    private void Fetch() {
        fetchBlocked = fetchedQueue.size() >= QUEUE_SIZE;
        if(!fetchBlocked && pc < instructions.length) {
            Instruction ins = instructions[pc];
            if(ins == null) {
                ins = new Instruction(); // NOOP
            }
            ins.id = insIdCount; // assign id
            ins.insAddress = pc; // assign ins address
            ins.fetchComplete = cycle; // save cycle number of fetch stage

            fetchedQueue.add(ins);
            pc++;
            insIdCount++; // prepare next id
        }

        if(fetchBlocked) { // stall can't fetch because the buffer is full
            probes.add(new Probe(cycle,0,0));
        }
    }

    private void Decode() {
        decodeBlocked = decodedQueue.size() >= QUEUE_SIZE;
        if(!decodeBlocked && !fetchedQueue.isEmpty()) {
            Instruction decoded = fetchedQueue.remove();
            decoded.decodeComplete = cycle; // save cycle number of decode stage
            decodedQueue.add(decoded);
        }

        if(decodeBlocked) { // stall: can't decode because the buffer is full
            Instruction ins = fetchedQueue.peek();
            if(ins != null) {
                probes.add(new Probe(cycle,1,ins.id));
            }
        }
    }

    private void Issue() {
        issueBlocked = reservationStations.size() >= QUEUE_SIZE;
        Instruction beforeIssue = decodedQueue.peek();
        if(!issueBlocked && !decodedQueue.isEmpty()) {
            // Checking valid bit
            if(validBits[beforeIssue.Rs1] && validBits[beforeIssue.Rs2]) {
                Instruction issuing = decodedQueue.remove();
                issuing.data1 = rf[issuing.Rs1];
                issuing.data2 = rf[issuing.Rs2];
                issuing = resultForwarding(issuing);
                if(issuing.Rd != 0 && issuing.Rd != 32) {
                    validBits[issuing.Rd] = false;
                }
                issuing.issueComplete = cycle; // save cycle number of issue stage
                reservationStations.add(issuing);
            }
        }

        if(issueBlocked) { // stall: can't issue since rs is full
            Instruction ins = decodedQueue.peek();
            if(ins != null) {
                probes.add(new Probe(cycle,6,ins.id));
            }
        }
    }

    private void Execute() {
        executeBlocked = executionResults.size() >= QUEUE_SIZE;
        euAllBusy = (alu0.busy && alu1.busy && lsu0.busy);
        if(!executeBlocked && !euAllBusy && !reservationStations.isEmpty()) {
            Instruction executing = reservationStations.peek();
            switch (executing.opcode) {
                case HALT:
                    if(!alu0.busy && !alu1.busy && !lsu0.busy && executionResults.isEmpty() && beforeWriteBack == null) {
                        reservationStations.remove();
                        finished = true;
                        rf[32]++;
                        executing.executeComplete = cycle;
                        finishedInsts.add(executing);
                    }
                    break;
                case NOOP:
                    reservationStations.remove();
                    rf[32]++;
                    executing.executeComplete = cycle;
                    finishedInsts.add(executing);
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
                    if (!alu0.busy) {
                        alu0.update(executing.opcode, executing.data1, executing.data2);
                        alu0.executing = executing;
                        reservationStations.remove();
                    } else if (!alu1.busy) {
                        alu1.update(executing.opcode, executing.data1, executing.data2);
                        alu1.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                case ADDI: // ALU OPs that use rf[Rs1] and Const
                case MULI:
                case DIVI:
                    if (!alu0.busy) {
                        alu0.update(executing.opcode, executing.data1, executing.Const);
                        alu0.executing = executing;
                        reservationStations.remove();
                    } else if (!alu1.busy) {
                        alu1.update(executing.opcode, executing.data1, executing.Const);
                        alu1.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                case NOT: // ALU OPs that only use rf[Rs1]
                case MOV:
                    if (!alu0.busy) {
                        alu0.update(executing.opcode, executing.data1, 0);
                        alu0.executing = executing;
                        reservationStations.remove();
                    } else if (!alu1.busy) {
                        alu1.update(executing.opcode, executing.data1, 0);
                        alu1.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                case MOVC: // ALU OPs that only use Const
                    if (!alu0.busy) {
                        alu0.update(executing.opcode, executing.Const, 0);
                        alu0.executing = executing;
                        reservationStations.remove();
                    } else if (!alu1.busy) {
                        alu1.update(executing.opcode, executing.Const, 0);
                        alu1.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                case LD:
                case ST:
                    if(!lsu0.busy) {
                        lsu0.update(executing.opcode, executing.data1, executing.data2);
                        lsu0.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                case LDI:
                case STI:
                    if(!lsu0.busy) {
                        lsu0.update(executing.opcode, executing.data1, executing.Const);
                        lsu0.executing = executing;
                        reservationStations.remove();
                    }
                    break;
                // Unconditional branch (Branches executed by BRU immediately)
                case JMP:
                case BR:
                case BRZ: // Conditional branch
                case BRN:
                    reservationStations.remove();
                    if (bru0.evaluateCondition(executing.opcode, executing.data1)) {
                        rf[32] = pc = bru0.evaluateTarget(executing.opcode, rf[32], executing.data1, executing.Const);
                        fetchedQueue.clear();
                        decodedQueue.clear();
                        reservationStations.clear();
                    } else {
                        rf[32]++;
                    }
                    executing.executeComplete = cycle;
                    finishedInsts.add(executing);
                    executedInsts++;
                    break;
                default:
                    System.out.println("Invalid instruction exception");
                    finished = true;
                    break;
            }
        }
        // ALUs and LSU works at here
        Instruction alu0_result = alu0.execute();
        Instruction alu1_result = alu1.execute();
        Instruction lsu0_result = lsu0.execute();
        if(alu0_result != null && alu0_result.result != null) {
            alu0_result.executeComplete = cycle; // save cycle number of execute stage
            executionResults.add(alu0_result);
            validBits[alu0_result.Rd] = true;
            alu0.reset();
            rf[32]++;
            executedInsts++;
        }
        if(alu1_result != null && alu1_result.result != null) {
            alu1_result.executeComplete = cycle; // save cycle number of execute stage
            executionResults.add(alu1_result);
            validBits[alu1_result.Rd] = true;
            alu1.reset();
            rf[32]++;
            executedInsts++;
        }
        if(lsu0_result != null && lsu0_result.memAddress != null) {
            lsu0_result.executeComplete = cycle; // save cycle number of execute stage
            executionResults.add(lsu0_result);
            lsu0.reset();
            rf[32]++;
            executedInsts++;
        }
        if(executeBlocked) { // stall: buffer is full
            Instruction ins = reservationStations.peek();
            if(ins != null) {
                probes.add(new Probe(cycle,11,ins.id));
            }
        }
        if(euAllBusy) { // stall: all EUs are busy
            Instruction ins = reservationStations.peek();
            if(ins != null) {
                probes.add(new Probe(cycle,7,ins.id));
            }
        }
    }

    private void Memory() {
        if(!executionResults.isEmpty()) {
            Instruction executed = executionResults.remove();
            if(executed.memAddress != null) {
                switch (executed.opcode) {
                    case LD:
                    case LDI:
                        executed.result = mem[executed.memAddress];
                        break;
                    case ST:
                    case STI:
                        mem[executed.memAddress] = rf[executed.Rd];
                        break;
                }
                validBits[executed.Rd] = true;
                executed.memoryComplete = cycle; // save cycle number of memory stage
                beforeWriteBack = executed;
            }
            else if(executed.result != null) { // non-memory instructions, skip the mem process
                executed.memoryComplete = cycle; // save cycle number of memory stage
                beforeWriteBack = executed;
            }
            else {
                System.out.println("Invalid executed result");
                finished = true;
            }
        }
    }

    private void WriteBack() {
        if(beforeWriteBack != null) {
            Instruction writeBack = beforeWriteBack;
            if(writeBack.Rd != 0 && writeBack.Rd != 32 && writeBack.opcode != Opcode.ST && writeBack.opcode != Opcode.STI) {
                rf[writeBack.Rd] = writeBack.result;
                validBits[writeBack.Rd] = true;
            }
            writeBack.writeBackComplete = cycle; // save cycle number of write back stage
            finishedInsts.add(writeBack);
        }
        beforeWriteBack = null;
    }

    private Instruction resultForwarding(Instruction ins) {
        Instruction clone = new Instruction(ins);
        if(beforeWriteBack != null) {
            if(ins.Rs1.equals(beforeWriteBack.Rd) && beforeWriteBack.result != null) {
                clone.data1 = beforeWriteBack.result;
            }
            if(ins.Rs2.equals(beforeWriteBack.Rd) && beforeWriteBack.result != null) {
                clone.data2 = beforeWriteBack.result;
            }
        }
        for(Instruction resultIns : executionResults) {
            if(ins.Rs1.equals(resultIns.Rd) && resultIns.result != null) {
                clone.data1 = resultIns.result;
            }
            if(ins.Rs2.equals(resultIns.Rd) && resultIns.result != null) {
                clone.data2 = resultIns.result;
            }
        }
        return clone;
    }

    public void RunProcessor() {
        Arrays.fill(validBits, true);
        int cycleLimit = 10000;
        while(!finished && pc < instructions.length && cycle < cycleLimit) {
            WriteBack();
            Memory();
            Execute();
            Issue();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || issueBlocked || executeBlocked || euAllBusy) {
                stalledCycle++;
            }
//            System.out.println("PC: "+ pc + " rf[32]: " + rf[32]);
        }
        TraceEncoder traceEncoder = new TraceEncoder(finishedInsts);
        ProbeEncoder probeEncoder = new ProbeEncoder(probes,cycle);
        try {
            traceEncoder.createTrace("../ACA-tracer/trace.out");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            probeEncoder.createProbe("../ACA-tracer/probe.out");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(cycle >= cycleLimit) {
            System.out.println("Time out");
        }
        System.out.println("Scalar 6-stage pipeline processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }
}
