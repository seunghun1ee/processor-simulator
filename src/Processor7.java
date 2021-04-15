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
    // register 0 always have value zero ($zero, input is ignored)
    RegisterStatus[] regStats = new RegisterStatus[rf.length];
    Instruction[] instructions; // instructions from user
    boolean finished = false;
    int QUEUE_SIZE = 4;
    int ISSUE_SIZE = 16;
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    ReservationStation[] RS = new ReservationStation[ISSUE_SIZE]; // unified reservation station
    CircularBufferROB ROB = new CircularBufferROB(ISSUE_SIZE); // Reorder buffer
    Queue<Instruction> executionResults = new LinkedList<>();

    int rs_aluReady = -1;
    int rs_lsuReady = -1;
    int rs_loadReady = -1;
    int rs_storeReady = -1;
    int rs_bruReady = -1;
    int rs_otherReady = -1;

    // final result registers before write back
    Queue<Instruction> beforeWriteBack = new LinkedList<>();

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
    ALU agu = new ALU();
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

            finishedInsts.add(ins);
        }

        if(fetchBlocked) { // stall can't fetch because the buffer is full
            probes.add(new Probe(cycle,0,0));
        }
    }

    private void Decode() {
        decodeBlocked = decodedQueue.size() >= QUEUE_SIZE;
        if(!decodeBlocked && !fetchedQueue.isEmpty()) {
            Instruction decoded = fetchedQueue.remove();
            switch (decoded.opcode) {
                case NOOP:
                case HALT:
                    decoded.opType = OpType.OTHER;
                    break;
                case ADD:
                case ADDI:
                case SUB:
                case MUL:
                case MULI:
                case DIV:
                case DIVI:
                case SHL:
                case SHR:
                case NOT:
                case AND:
                case OR:
                case MOV:
                case MOVC:
                case CMP:
                    decoded.opType = OpType.ALU;
                    break;
                case LD:
                case LDI:
                    decoded.opType = OpType.LOAD;
                    break;
                case ST:
                case STI:
                    decoded.opType = OpType.STORE;
                    break;
                case BR:
                case JMP:
                case BRZ:
                case BRN:
                    decoded.opType = OpType.BRU;
                    break;
                default:
                    System.out.println("invalid opcode detected while decoding");
                    finished = true;
                    break;
            }

            decoded.decodeComplete = cycle; // save cycle number of decode stage
            decodedQueue.add(decoded);

            int i = finishedInsts.indexOf(decoded);
            finishedInsts.set(i,decoded);
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
        boolean rsBlocked = true;
        for(int i = 0; i < RS.length; i++) {
            if(!RS[i].busy) { // there is available rs
                rsBlocked = false; // RS available
                rsIndex = i; // get available rs index
                break;
            }
        }
        boolean robBlocked = ROB.size() >= ROB.capacity; // ROB full
        issueBlocked = rsBlocked || robBlocked;
        if(!issueBlocked && !decodedQueue.isEmpty()) {
            Instruction issuing = decodedQueue.remove();
            ReorderBuffer allocatedROB = new ReorderBuffer();
            int robIndex;
            // for all ins
            issuing.issueComplete = cycle;
            issuing.rsIndex = rsIndex;
            if(issuing.Rs1 != 0 && regStats[issuing.Rs1].busy) { // there is in-flight ins that writes Rs1
                int Rs1robIndex = regStats[issuing.Rs1].robIndex;
                if(ROB.buffer[Rs1robIndex].ready) { // dependent instruction is completed and ready
                    //dependency resolved from ROB
                    RS[rsIndex].V1 = ROB.buffer[Rs1robIndex].value;
                    RS[rsIndex].Q1 = -1;
                }
                else {
                    // wait for result from ROB
                    RS[rsIndex].Q1 = Rs1robIndex;
                }
            }
            else { // no Rs1 dependency
                RS[rsIndex].V1 = rf[issuing.Rs1]; // 0 if Rs1 = 0
                RS[rsIndex].Q1 = -1;
            }
            if(issuing.Rs2 != 0 && regStats[issuing.Rs2].busy) { // there is in-flight ins that writes Rs2
                int Rs2robIndex = regStats[issuing.Rs2].robIndex;
                if(ROB.buffer[Rs2robIndex].ready) { // dependent instruction is completed and ready
                    //dependency resolved from ROB
                    RS[rsIndex].V2 = ROB.buffer[Rs2robIndex].value;
                    RS[rsIndex].Q2 = -1;
                }
                else {
                    // wait for result from ROB
                    RS[rsIndex].Q2 = Rs2robIndex;
                }
            }
            else { // no Rs2 dependency
                RS[rsIndex].V2 = rf[issuing.Rs2]; // 0 if Rs2 = 0
                RS[rsIndex].Q2 = -1;
            }
            // set Reorder Buffer
            allocatedROB.ins = issuing;
            allocatedROB.destination = issuing.Rd;
            allocatedROB.ready = false;
            robIndex = ROB.push(allocatedROB);
            // set Reservation Station
            RS[rsIndex].op = issuing.opcode;
            RS[rsIndex].ins = issuing;
            RS[rsIndex].busy = true;
            RS[rsIndex].robIndex = robIndex;
            RS[rsIndex].type = issuing.opType;

            switch (issuing.opType) {
                case ALU:
                    // for ins that only use Const
                    if(RS[rsIndex].Q1 == -1 && issuing.opcode.equals(Opcode.MOVC)) {
                        RS[rsIndex].V1 += issuing.Const;
                    }
                    // when second operand is ready
                    else if(RS[rsIndex].Q2 == -1) {

                        RS[rsIndex].V2 += issuing.Const; // for imm instructions
                    }
                    // set regStats
                    if(issuing.Rd != 0) {
                        regStats[issuing.Rd].robIndex = robIndex;
                        regStats[issuing.Rd].busy = true;
                    }
                    break;
                case LOAD:
                    RS[rsIndex].A = issuing.Const; // for LDI
                    // set regStats
                    if(issuing.Rd != 0) {
                        regStats[issuing.Rd].robIndex = robIndex;
                        regStats[issuing.Rd].busy = true;
                    }
                    break;
                case STORE:
                    if(issuing.Rd != 0 && regStats[issuing.Rd].busy) { // there is in-flight ins that writes at Rd
                        int storeRobIndex = regStats[issuing.Rd].robIndex;
                        if(ROB.buffer[storeRobIndex].ready) { // dependent instruction is completed and ready
                            //dependency resolved from ROB
                            RS[rsIndex].Vs = ROB.buffer[storeRobIndex].value;
                            RS[rsIndex].Qs = -1;
                        }
                        else {
                            // wait for result from ROB
                            RS[rsIndex].Qs = storeRobIndex;
                        }
                    }
                    else { // no Rd dependency
                        RS[rsIndex].Vs = rf[issuing.Rd]; // 0 if Rd = 0
                        RS[rsIndex].Qs = -1;
                    }
                    RS[rsIndex].A = issuing.Const; // for STI
                    // no regStats set for stores
                    break;
                case BRU:
                    // for ins that only use Const
                    if(RS[rsIndex].Q1 == -1 && issuing.opcode.equals(Opcode.JMP)) {
                        RS[rsIndex].V1 += issuing.Const;
                    }
                    // when second operand is ready
                    else if(RS[rsIndex].Q2 == -1) {
                        RS[rsIndex].V2 += issuing.Const; // for imm instructions
                    }
                    // no regStats set for branch operations
                    break;
                case OTHER:
                    break;
                default:
                    System.out.println("invalid instruction detected at issue stage");
                    finished = true;
                    break;
            }

            int i = finishedInsts.indexOf(issuing);
            finishedInsts.set(i,issuing);
        }
        if(issueBlocked && !decodedQueue.isEmpty()) {
            probes.add(new Probe(cycle,7,decodedQueue.peek().id));
        }
    }

    private void Dispatch() { // finding ready to execute rs
        rs_aluReady = getReadyRSIndex(OpType.ALU);

        rs_loadReady = getReadyLoadIndex();
        rs_storeReady = getReadyStoreIndex();

        rs_bruReady = getReadyRSIndex(OpType.BRU);
        rs_otherReady = getReadyRSIndex(OpType.OTHER);
        if(rs_aluReady > -1) {
            RS[rs_aluReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_aluReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        if(rs_loadReady > -1) {
            RS[rs_loadReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_loadReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        if(rs_storeReady > -1) {
            RS[rs_storeReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_storeReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        if(rs_bruReady > -1) {
            RS[rs_bruReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_bruReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        if(rs_otherReady > -1) {
            RS[rs_otherReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_otherReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        dispatchBlocked = (rs_aluReady == -1) && (rs_loadReady == -1) && (rs_storeReady == -1) && (rs_bruReady == -1) && (rs_otherReady == -1);
    }

    private int getReadyLoadIndex() {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i = 0; i < RS.length; i++) {
            if(
                    RS[i].busy
                    && !RS[i].executing
                    && RS[i].type.equals(OpType.LOAD)
                    && RS[i].Q1 == -1
                    && RS[i].Q2 == -1
            ) {
                int j = RS[i].robIndex; // j is ROB index of the ins
                if(checkRobForLoadStage1(j) && RS[i].ins.id < priority) {
                    priority = RS[i].ins.id;
                    readyIndex = i;
                }
            }
        }
        return readyIndex;
    }

    private boolean checkRobForLoadStage1(int currentRobIndex) {
        int j = currentRobIndex;
        while (j != ROB.head) {
            System.out.println("load1 while loop " + j);
            if(j == 0) {
                j = ROB.capacity -1;
            }
            else {
                j--;
            }
            if(ROB.buffer[j] != null && ROB.buffer[j].ins.opType.equals(OpType.STORE)) {
                return false;
            }
        }
        return true;
    }

    private int getReadyStoreIndex() {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i = 0; i < RS.length; i++) {
            int robIndex = RS[i].robIndex;
            if(
                    RS[i].busy
                    && !RS[i].executing
                    && RS[i].type.equals(OpType.STORE)
                    && RS[i].Q1 == -1
                    && RS[i].Q2 == -1
                    && RS[i].Qs == -1
                    && robIndex == ROB.head
            ) {
                // if this was fetched earlier than current priority
                if(RS[i].ins.id < priority) {
                    // this is new ready RS
                    priority = RS[i].ins.id;
                    readyIndex = i;
                }
            }
        }
        return readyIndex;
    }

    private int getReadyRSIndex(OpType opType) {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i=0; i < RS.length; i++) {
            if(
                    RS[i].busy &&
                            !RS[i].executing &&
                            RS[i].type.equals(opType) &&
                            RS[i].Q1 == -1 &&
                            RS[i].Q2 == -1 &&
                            RS[i].Qs == -1
            ) {
                // if this was fetched earlier than current priority
                if(RS[i].ins.id < priority) {
                    // this is new ready RS
                    priority = RS[i].ins.id;
                    readyIndex = i;
                }
            }
        }
        return readyIndex;
    }

    private void Execute() {
        executeBlocked = executionResults.size() >= QUEUE_SIZE;
        euAllBusy = (alu0.busy && alu1.busy && lsu0.busy);
        boolean branchTaken = false;
        boolean loadAddressReady = false;
        boolean storeAddressReady = false;
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
                            regStats[RS[i].ins.Rd].busy = false;
                            regStats[RS[i].ins.Rd].robIndex = -1;
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
//                finishedInsts.add(executing);
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
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
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
            }

            // load stage 1
            if(rs_loadReady > -1 && !RS[rs_loadReady].addressReady && !branchTaken) {
                RS[rs_loadReady].executing = true;
                RS[rs_loadReady].ins.executeComplete = cycle;
                int memAddress = agu.evaluate(Opcode.ADD,RS[rs_loadReady].V1,RS[rs_loadReady].A);
                RS[rs_loadReady].A = memAddress;
                RS[rs_loadReady].ins.memAddress = memAddress;
                RS[rs_loadReady].addressReady = true;
                loadAddressReady = true;
                int i = finishedInsts.indexOf(RS[rs_loadReady].ins);
                finishedInsts.set(i,RS[rs_loadReady].ins);
            }
            if(rs_storeReady > -1 && !branchTaken) {
                int robIndex = RS[rs_storeReady].robIndex;
                RS[rs_storeReady].executing = true;
                RS[rs_storeReady].ins.executeComplete = cycle;
                int memAddress = agu.evaluate(Opcode.ADD,RS[rs_storeReady].V1,RS[rs_storeReady].A);
                RS[rs_storeReady].ins.memAddress = memAddress;
                ROB.buffer[robIndex].address = memAddress;
                storeAddressReady = true;
                int i = finishedInsts.indexOf(RS[rs_storeReady].ins);
                finishedInsts.set(i,RS[rs_storeReady].ins);
            }
            if(rs_otherReady > -1 && !branchTaken) {
                ReservationStation rs_execute = RS[rs_otherReady];
                rs_execute.ins.data1 = rs_execute.V1;
                rs_execute.ins.data2 = rs_execute.V2;
                executing = rs_execute.ins;
                if(executing.opcode.equals(Opcode.HALT)) {
                    if(!alu0.busy && !alu1.busy && !lsu0.busy && executionResults.isEmpty() && beforeWriteBack.isEmpty()) {
                        RS[executing.rsIndex].executing = true;
                        finished = true;
                        executing.executeComplete = cycle;
                        executing.memoryComplete = cycle + 1;
                        executing.writeBackComplete = cycle + 2;
//                        finishedInsts.add(executing);
                        RS[rs_otherReady] = new ReservationStation();

                        int i = finishedInsts.indexOf(executing);
                        finishedInsts.set(i,executing);
                    }
                }
                else {
                    RS[executing.rsIndex].executing = true;
                    executing.executeComplete = cycle;
                    executing.memoryComplete = cycle + 1;
                    executing.writeBackComplete = cycle + 2;
//                    finishedInsts.add(executing);
                    RS[rs_otherReady] = new ReservationStation();

                    int i = finishedInsts.indexOf(executing);
                    finishedInsts.set(i,executing);
                }
            }
        }
        // ALUs and LSU works at here
        Instruction alu0_result = alu0.execute();
        Instruction alu1_result = alu1.execute();
//        Instruction lsu0_result = lsu0.execute();
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
        if(loadAddressReady) {
            executionResults.add(RS[rs_loadReady].ins);
            executedInsts++;
        }
        if(storeAddressReady) {
            executionResults.add(RS[rs_storeReady].ins);
            executedInsts++;
        }
//        if(lsu0_result != null && lsu0_result.memAddress != null) {
//            executionResults.add(lsu0_result);
//            RS[lsu0_result.rsIndex].A = lsu0_result.memAddress;
//            lsu0.reset();
//            executedInsts++;
//        }
//        if(executeBlocked) { // stall: buffer is full
//
//        }
//        if(euAllBusy) { // stall: all EUs are busy
//
//        }
    }

    private void Memory() {
        while(!executionResults.isEmpty()) {
            Instruction executed = executionResults.remove();
            switch (executed.opcode) {
                case LD:
                case LDI:
                    executed.result = mem[executed.memAddress];
                    resultForwarding2(executed);
                    break;
                default: // non memory instructions, only do result forwarding
                    resultForwarding2(executed);
                    break;
            }
            executed.memoryComplete = cycle; // save cycle number of memory stage
            int i = finishedInsts.indexOf(executed);
            finishedInsts.set(i,executed);
            beforeWriteBack.add(executed);
        }
    }

    private void WriteBack() {
        while(!beforeWriteBack.isEmpty()) {
            Instruction writeBack = beforeWriteBack.remove();
            writeBack.writeBackComplete = cycle;
            if(writeBack.Rd != 0) {
                int rsIndex = writeBack.rsIndex;
                int robIndex = RS[rsIndex].robIndex;
                if(writeBack.opType.equals(OpType.STORE)) { // store instructions
                    ROB.buffer[ROB.head].value = RS[rsIndex].Vs;
                }
                else {
                    ROB.buffer[robIndex].value = writeBack.result;
                }
                RS[rsIndex] = new ReservationStation(); // clear RS entry
                resultForwarding2(writeBack);
                ROB.buffer[robIndex].ready = true;
            }
//            finishedInsts.add(writeBack);
            int i = finishedInsts.indexOf(writeBack);
            finishedInsts.set(i,writeBack);
        }
    }

    private void Commit() {
        while(!ROB.isEmpty()) {
            int h = ROB.head;
            ReorderBuffer robHead = ROB.peak();
            if(!robHead.ready) { // head is not ready to commit
                break; // abort committing
            }

            if(robHead.ins.opType.equals(OpType.STORE)) {
                mem[robHead.address] = robHead.value; // update memory here
            }
            else { // update registers with result
                int Rd = robHead.destination;
                rf[Rd] = robHead.value;
                    if(regStats[Rd].busy && regStats[Rd].robIndex == h) {
                        regStats[Rd] = new RegisterStatus(); // free up register status entry
                    }
            }
            ROB.pop(); // free up ROB entry, new head
        }
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
        for(int i=0; i < RS.length; i++) {
            RS[i] = new ReservationStation();
        }
        for(int i=0; i < regStats.length; i++) {
            regStats[i] = new RegisterStatus();
        }
        int cycleLimit = 10000;
        while(!finished && pc < instructions.length && cycle < cycleLimit) {
            Commit();
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
            System.out.println("PC: "+ pc);
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
        System.out.println("Scalar Out of Order 8-stage pipeline processor Terminated");
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
    }
}
