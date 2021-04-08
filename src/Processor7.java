import java.io.IOException;
        import java.util.*;

public class Processor7 {

    public Processor7(int[] mem, Instruction[] instructions) {
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
    int[] Qi = new int[rf.length]; // Tomasulo: number of rs that the operation result will be stored to the register
    // register 0 always have value zero ($zero, input is ignored)
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    int QUEUE_SIZE = 4;
    int RS_SIZE = 4;
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    Queue<Instruction> reservationStations = new LinkedList<>(); // old
    ReservationStation[] RS = new ReservationStation[RS_SIZE * 4]; // unified reservation station
    Queue<Instruction> executionResults = new LinkedList<>();

    int rs_aluReady = -1;
    int rs_lsuReady = -1;
    int rs_bruReady = -1;
    int rs_otherReady = -1;

    // final result registers before write back
    Instruction beforeWriteBack;

    // state of pipeline stages
    boolean fetchBlocked = false;
    boolean decodeBlocked = false;
    boolean issueBlocked = false;
    boolean dispatchBlocked = false;
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
            Instruction fetch = instructions[pc];
            Instruction ins = new Instruction(); // NOOP
            if(fetch != null) {
                ins = new Instruction(fetch);
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

    private void Issue() { // issuing decoded instruction to reservation stations
        int rsIndex = -1;
        issueBlocked = true;
        for(int i = 0; i < RS.length; i++) {
            if(!RS[i].busy) { // there is available rs
                issueBlocked = false; // issue is not blocked
                rsIndex = i; // get available rs index
                break;
            }
        }
        if(!issueBlocked && !decodedQueue.isEmpty()) {
            Instruction issuing = decodedQueue.remove();
            switch (issuing.opcode) {
                case NOOP:
                case HALT:
                    RS[rsIndex].op = issuing.opcode;
                    RS[rsIndex].Q1 = -1;
                    RS[rsIndex].Q2 = -1;
                    RS[rsIndex].Qs = -1;
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.OTHER;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
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
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }

                    if(Qi[issuing.Rs2] != -1) { // Rs2 is dependent to instruction before
                        RS[rsIndex].Q2 = Qi[issuing.Rs2]; // store dependency
                    }
                    else { // no Rs2 dependency
                        RS[rsIndex].V2 = rf[issuing.Rs2];
                        RS[rsIndex].Q2 = -1;
                    }

                    // no dependency setting to special purpose registers
                    if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.ALU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case LD: // Load OP that uses rf[Rs1] and rf[Rs2]
                case ST: // Store OP that uses rf[Rs1] and rf[Rs2]
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }

                    if(Qi[issuing.Rs2] != -1) { // Rs2 is dependent to instruction before
                        RS[rsIndex].Q2 = Qi[issuing.Rs2]; // store dependency
                    }
                    else { // no Rs2 dependency
                        RS[rsIndex].V2 = rf[issuing.Rs2];
                        RS[rsIndex].Q2 = -1;
                    }

                    if(issuing.opcode.equals(Opcode.ST)) { // if store instruction
                        if(Qi[issuing.Rd] != -1) { // Register to store is dependent to instructions before
                            RS[rsIndex].Qs = Qi[issuing.Rd];
                        }
                        else { // no register to store dependency
                            RS[rsIndex].Vs = rf[issuing.Rd];
                            RS[rsIndex].Qs = -1;
                        }
                    }
                    // no dependency setting to special purpose registers
                    else if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.LSU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case ADDI: // ALU OPs that use rf[Rs1] and Const
                case MULI:
                case DIVI:
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }
                    // Const
                    RS[rsIndex].V2 = issuing.Const;
                    RS[rsIndex].Q2 = -1;
                    // no dependency setting to special purpose registers
                    if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.ALU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case LDI: // Load OP that uses rf[Rs1] and Const
                case STI: // Store OP that uses rf[Rs1] and Const
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }
                    // Const
                    RS[rsIndex].V2 = issuing.Const;
                    RS[rsIndex].Q2 = -1;
                    if(issuing.opcode.equals(Opcode.STI)) { // if store instruction
                        if(Qi[issuing.Rd] != -1) { // Register to store is dependent to instructions before
                            RS[rsIndex].Qs = Qi[issuing.Rd];
                        }
                        else { // no register to store dependency
                            RS[rsIndex].Vs = rf[issuing.Rd];
                            RS[rsIndex].Qs = -1;
                        }
                    }
                    // no dependency setting to special purpose registers
                    else if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.LSU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case BR: // Unconditional branch that uses rf[Rs1] and Const
                case BRZ: // Conditional branches that use rf[Rs1] and Const
                case BRN:
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }
                    // Const
                    RS[rsIndex].V2 = issuing.Const;
                    RS[rsIndex].Q2 = -1;
                    //branch instruction

                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.BRU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case NOT: // ALU OPs that only use rf[Rs1]
                case MOV:
                    RS[rsIndex].op = issuing.opcode;
                    if(Qi[issuing.Rs1] != -1) { // Rs1 is dependent to instructions before
                        RS[rsIndex].Q1 = Qi[issuing.Rs1]; // store dependency
                    }
                    else { // no Rs1 dependency
                        RS[rsIndex].V1 = rf[issuing.Rs1];
                        RS[rsIndex].Q1 = -1;
                    }
                    // No second operand
                    RS[rsIndex].V2 = 0;
                    RS[rsIndex].Q2 = -1;
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.ALU;
                    // no dependency setting to special purpose registers
                    if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case MOVC: // ALU OPs that only use Const
                    RS[rsIndex].op = issuing.opcode;
                    // Const
                    RS[rsIndex].V1 = issuing.Const;
                    RS[rsIndex].Q1 = -1;
                    // No second operand
                    RS[rsIndex].V2 = 0;
                    RS[rsIndex].Q2 = -1;
                    if(issuing.Rd != 0) {
                        Qi[issuing.Rd] = rsIndex; // Set dependency to destination
                    }
                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.ALU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                case JMP: // Unconditional branches that only use Const
                    RS[rsIndex].op = issuing.opcode;
                    // Const
                    RS[rsIndex].V1 = issuing.Const;
                    RS[rsIndex].Q1 = -1;
                    // No second operand
                    RS[rsIndex].V2 = 0;
                    RS[rsIndex].Q2 = -1;

                    RS[rsIndex].busy = true;
                    RS[rsIndex].type = OpType.BRU;
                    issuing.issueComplete = cycle; // save cycle number of issue stage
                    issuing.rsIndex = rsIndex;
                    RS[rsIndex].ins = issuing;
                    break;
                default:
                    System.out.println("Invalid instruction");
                    finished = true;
                    break;
            }
        }

        if(issueBlocked && !decodedQueue.isEmpty()) {
            probes.add(new Probe(cycle,7,decodedQueue.peek().id));
        }
    }

    private void Dispatch() { // finding ready to execute rs
        rs_aluReady = getReadyRSIndex(RS,OpType.ALU);
        rs_lsuReady = getReadyRSIndex(RS, OpType.LSU);
        rs_bruReady = getReadyRSIndex(RS, OpType.BRU);
        rs_otherReady = getReadyRSIndex(RS, OpType.OTHER);
        if(rs_aluReady > -1) {
            RS[rs_aluReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
        }
        if(rs_lsuReady > -1) {
            RS[rs_lsuReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
        }
        if(rs_bruReady > -1) {
            RS[rs_bruReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
        }
        if(rs_otherReady > -1) {
            RS[rs_otherReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
        }
        dispatchBlocked = (rs_aluReady == -1) && (rs_lsuReady == -1) && (rs_bruReady == -1) && (rs_otherReady == -1);
    }


    private int getReadyRSIndex(ReservationStation[] RS, OpType opType) {
        for(int i=0; i < RS.length; i++) {
            if(
                    RS[i].busy &&
                            !RS[i].executing &&
                            RS[i].type.equals(opType) &&
                            RS[i].Q1 == -1 &&
                            RS[i].Q2 == -1 &&
                            RS[i].Qs == -1
            ) {
                return i;
            }
        }
        return -1;
    }

    private void Execute() {
        executeBlocked = executionResults.size() >= QUEUE_SIZE;
        euAllBusy = (alu0.busy && alu1.busy && lsu0.busy);
        boolean branchTaken = false;
        if(!executeBlocked && !euAllBusy) {
            Instruction executing;
            if(rs_bruReady > -1) {
                ReservationStation rs_execute = RS[rs_bruReady];
                rs_execute.ins.data1 = rs_execute.V1;
                rs_execute.ins.data2 = rs_execute.V2;
                executing = rs_execute.ins;
                executing.executeComplete = cycle;
                RS[executing.rsIndex].executing = true;
                if (bru0.evaluateCondition(executing.opcode, executing.data1)) {
                    branchTaken = true;
                    executing.result = pc = bru0.evaluateTarget(executing.opcode, executing.insAddress, executing.data1, executing.data2);
                    // Flushing
                    fetchedQueue.clear();
                    decodedQueue.clear();
                    for(int i = 0; i < RS.length; i++) {
                        // if the instruction is issued later than the branch execution
                        if(!RS[i].executing && RS[i].ins.id > executing.id) {
                            Qi[RS[i].ins.Rd] = -1;
                            RS[i] = new ReservationStation();
                            // if flushed one was dispatched one, flush dispatch
                            if(rs_aluReady == i) {
                                rs_aluReady = -1;
                            }
                            else if(rs_lsuReady == i) {
                                rs_lsuReady = -1;
                            }
                            else if(rs_bruReady == i) {
                                rs_bruReady = -1;
                            }
                            else if(rs_otherReady == i) {
                                rs_otherReady = -1;
                            }
                        }
                    }
                }
                finishedInsts.add(executing);
                executedInsts++;
                resultForwarding2(executing);

                RS[rs_bruReady] = new ReservationStation();
            }
            if(rs_aluReady > -1 && !branchTaken) {
                ReservationStation rs_execute = RS[rs_aluReady];
                rs_execute.ins.data1 = rs_execute.V1;
                rs_execute.ins.data2 = rs_execute.V2;
                executing = rs_execute.ins;
                executing.executeComplete = cycle;
                if (!alu0.busy) {
                    alu0.update(executing.opcode, executing.data1, executing.data2);
                    alu0.executing = executing;
                    RS[executing.rsIndex].executing = true;
                } else if (!alu1.busy) {
                    alu1.update(executing.opcode, executing.data1, executing.data2);
                    alu1.executing = executing;
                    RS[executing.rsIndex].executing = true;
                }
            }
            if(rs_lsuReady > -1 && !branchTaken) {
                ReservationStation rs_execute = RS[rs_lsuReady];
                rs_execute.ins.data1 = rs_execute.V1;
                rs_execute.ins.data2 = rs_execute.V2;
                executing = rs_execute.ins;
                executing.executeComplete = cycle;
                if(!lsu0.busy) {
                    lsu0.update(executing.opcode, executing.data1, executing.data2);
                    lsu0.executing = executing;
                    RS[executing.rsIndex].executing = true;
                }
            }
            if(rs_otherReady > -1 && !branchTaken) {
                ReservationStation rs_execute = RS[rs_otherReady];
                rs_execute.ins.data1 = rs_execute.V1;
                rs_execute.ins.data2 = rs_execute.V2;
                executing = rs_execute.ins;
                if(executing.opcode.equals(Opcode.HALT)) {
                    if(!alu0.busy && !alu1.busy && !lsu0.busy && executionResults.isEmpty() && beforeWriteBack == null) {
                        RS[executing.rsIndex].executing = true;
                        finished = true;
                        executing.executeComplete = cycle;
                        executing.memoryComplete = cycle + 1;
                        executing.writeBackComplete = cycle + 2;
                        finishedInsts.add(executing);
                        RS[rs_otherReady] = new ReservationStation();
                    }
                }
                else {
                    RS[executing.rsIndex].executing = true;
                    executing.executeComplete = cycle;
                    executing.memoryComplete = cycle + 1;
                    executing.writeBackComplete = cycle + 2;
                    finishedInsts.add(executing);
                    RS[rs_otherReady] = new ReservationStation();
                }
            }
        }
        // ALUs and LSU works at here
        Instruction alu0_result = alu0.execute();
        Instruction alu1_result = alu1.execute();
        Instruction lsu0_result = lsu0.execute();
        if(alu0_result != null && alu0_result.result != null) {
            executionResults.add(alu0_result);
            resultForwarding2(alu0_result);
            alu0.reset();
            executedInsts++;
        }
        if(alu1_result != null && alu1_result.result != null) {
            executionResults.add(alu1_result);
            resultForwarding2(alu1_result);
            alu1.reset();
            executedInsts++;
        }
        if(lsu0_result != null && lsu0_result.memAddress != null) {
            executionResults.add(lsu0_result);
            RS[lsu0_result.rsIndex].A = lsu0_result.memAddress;
            lsu0.reset();
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
                        resultForwarding2(executed);
                        break;
                    case ST:
                    case STI:
                        mem[executed.memAddress] = RS[executed.rsIndex].Vs;
                        break;
                }
                executed.memoryComplete = cycle; // save cycle number of memory stage
                beforeWriteBack = executed;
            }
            else if(executed.result != null) { // non-memory instructions, skip the mem process
                executed.memoryComplete = cycle; // save cycle number of memory stage
                beforeWriteBack = executed;
                resultForwarding2(executed);
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
            if(writeBack.Rd != 0 && writeBack.opcode != Opcode.ST && writeBack.opcode != Opcode.STI) {
                resultForwarding2(writeBack);
                // if the latest destination dependency is this one
                if(Qi[writeBack.Rd] == writeBack.rsIndex) {
                    Qi[writeBack.Rd] = -1;
                }
                rf[writeBack.Rd] = writeBack.result;
            }
            RS[writeBack.rsIndex] = new ReservationStation();
            writeBack.writeBackComplete = cycle; // save cycle number of write back stage
            finishedInsts.add(writeBack);
        }
        beforeWriteBack = null;
    }

    private void resultForwarding2(Instruction ins) {
        for (ReservationStation rs : RS) {
            if (rs.busy) {
                if (rs.Q1 == ins.rsIndex) {
                    rs.V1 = ins.result;
                    rs.Q1 = -1;
                }
                if (rs.Q2 == ins.rsIndex) {
                    rs.V2 = ins.result;
                    rs.Q2 = -1;
                }
                if (rs.Qs == ins.rsIndex) {
                    rs.Vs = ins.result;
                    rs.Qs = -1;
                }
            }
        }
    }

    public void RunProcessor() {
        Arrays.fill(Qi,-1);
        for(int i=0; i < RS.length; i++) {
            RS[i] = new ReservationStation();
        }
        int cycleLimit = 10000;
        while(!finished && pc < instructions.length && cycle < cycleLimit) {
            WriteBack();
            Memory();
            Execute();
            Dispatch();
            Issue();
            Decode();
            Fetch();
            cycle++;
            if(fetchBlocked || decodeBlocked || issueBlocked || executeBlocked || euAllBusy) {
                stalledCycle++;
            }
//            System.out.println("PC: "+ pc);
        }
        finishedInsts.sort(Comparator.comparingInt((Instruction i) -> i.id));
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
        System.out.println("Scalar Out of Order 7-stage pipeline processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }
}
