import java.io.IOException;
import java.util.*;

public class Processor8 {

    public Processor8(int[] mem, Instruction[] instructions) {
        this.mem = mem;
        this.instructions = instructions;
    }

    int cycle = 0;
    int pc = 0; // Program counter
    int executedInsts = 0; // Number of instructions executed
    int stalledCycle = 0;
    int waitingCycle = 0;
    int insIdCount = 1; // for assigning id to instructions
    int[] mem; // memory from user
    int[] rf = new int[64]; //Register file (physical)
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
    // final result registers before write back
    Queue<Instruction> beforeWriteBack = new LinkedList<>();

    int rs_aluReady = -1;
    int rs_loadReady = -1;
    int rs_storeReady = -1;
    int rs_bruReady = -1;
    int rs_otherReady = -1;

    // state of pipeline stages
    // fetch states
    boolean fetchBlocked = false;
    // decode states
    boolean nothingToDecode = false;
    boolean decodeBlocked = false;
    // issue states
    boolean nothingToIssue = false;
    boolean issueBlocked = false;
    // dispatch states
    boolean nothingToDispatch = false;
    boolean dispatchBlocked = false;
    // execute states
    boolean nothingToExecute = false;
    boolean executeBlocked = false;
    boolean euAllBusy = false;
    // memory states
    boolean nothingToMemory = false;
    // write back states
    boolean nothingToWriteBack = false;
    // commit states
    boolean robEmpty = false;
    boolean commitUnavailable = false;

    // execution units
    // Arithmetic Logic Unit
    ALU alu0 = new ALU();
    ALU alu1 = new ALU();
    // Address Generation Unit
    ALU agu = new ALU();
    // Branch Unit
    BRU bru0 = new BRU();

    //For visualisation
    List<Instruction> finishedInsts = new ArrayList<>();
    List<Probe> probes = new ArrayList<>();

    private void Fetch() {
        fetchBlocked = fetchedQueue.size() >= QUEUE_SIZE;
        if(!fetchBlocked && pc < instructions.length) {
            Instruction fetch = instructions[pc];
            if(fetch == null) {
                return;
            }
            Instruction ins; // = new Instruction(); // NOOP
            ins = new Instruction(fetch);
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
        nothingToDecode = fetchedQueue.isEmpty();
        if(!decodeBlocked && !nothingToDecode) {
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
//        if(nothingToDecode) {
//            probes.add(new Probe(cycle, ,0));
//        }
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
        nothingToDecode = decodedQueue.isEmpty();

        if(!issueBlocked && !nothingToDecode) {
            Instruction issuing = decodedQueue.remove();
            ReorderBuffer allocatedROB = new ReorderBuffer();
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
            int robIndex = ROB.push(allocatedROB);
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
                    if(RS[rsIndex].ins.opcode.equals(Opcode.LDI)) {
                        RS[rsIndex].V2 = issuing.Const; // for LDI
                    }
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
                    if(RS[rsIndex].ins.opcode.equals(Opcode.STI)) {
                        RS[rsIndex].V2 = issuing.Const; // for STI
                    }
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

    private void Dispatch() {
        rs_aluReady = getReadyRSIndex(OpType.ALU);
        rs_loadReady = getReadyLoadIndex();
        rs_storeReady = getReadyStoreIndex();
        rs_bruReady = getReadyRSIndex(OpType.BRU);
        rs_otherReady = getReadyOtherIndex();
        if(rs_aluReady > -1) {
            dispatchOperands(rs_aluReady);
        }
        if(rs_loadReady > -1) {
            dispatchOperands(rs_loadReady);
        }
        if(rs_storeReady > -1) {
            dispatchOperands(rs_storeReady);
        }
        if(rs_bruReady > -1) {
            dispatchOperands(rs_bruReady);
        }
        if(rs_otherReady > -1) {
            // no operand dispatch
            RS[rs_otherReady].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
            Instruction dispatched = RS[rs_otherReady].ins;
            int i = finishedInsts.indexOf(dispatched);
            finishedInsts.set(i,dispatched);
        }
        dispatchBlocked = (rs_aluReady == -1) && (rs_loadReady == -1) && (rs_storeReady == -1) && (rs_bruReady == -1) && (rs_otherReady == -1);
    }

    private void dispatchOperands(int rs_index) {
        //dispatch operands
        RS[rs_index].ins.data1 = RS[rs_index].V1;
        RS[rs_index].ins.data2 = RS[rs_index].V2;
        RS[rs_index].ins.dispatchComplete = cycle; // save cycle number of dispatch stage
        Instruction dispatched = RS[rs_index].ins;
        int i = finishedInsts.indexOf(dispatched);
        finishedInsts.set(i,dispatched);
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

    private int getReadyOtherIndex() {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i=0; i < RS.length; i++) {
            if(
                    RS[i].busy
                            && !RS[i].executing
                            && RS[i].type.equals(OpType.OTHER)
                            && RS[i].Q1 == -1
                            && RS[i].Q2 == -1
                            && RS[i].Qs == -1
            ) {
                if(RS[i].ins.opcode.equals(Opcode.HALT) && !ROB.peak().ins.equals(RS[i].ins)) {
                    continue; // when it's HALT but if it's not the head of ROB, don't dispatch it
                }
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
        nothingToExecute = rs_aluReady == -1 && rs_loadReady == -1 && rs_storeReady == -1 && rs_bruReady == -1 && rs_otherReady == -1;
        euAllBusy = (alu0.busy && alu1.busy);

        boolean loadAddressReady = false;
        boolean storeAddressReady = false;
        if(!executeBlocked && !nothingToExecute) {
            if(rs_bruReady > -1) {
                // verify branch
            }
            if(rs_aluReady > -1) {
                Instruction executing = RS[rs_aluReady].ins;
                if(!alu0.busy) {
                    alu0.update(executing.opcode,executing.data1,executing.data2);
                    alu0.executing = executing;
                    executing.executeComplete = cycle;
                    RS[executing.rsIndex].executing = true;
                }
                else if(!alu1.busy) {
                    alu1.update(executing.opcode, executing.data1, executing.data2);
                    alu1.executing = executing;
                    executing.executeComplete = cycle;
                    RS[executing.rsIndex].executing = true;
                }
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
            }
            if(rs_loadReady > -1) {
                Instruction executing = RS[rs_loadReady].ins;
                RS[rs_loadReady].executing = true;
                executing.memAddress = agu.evaluate(Opcode.ADD,executing.data1,executing.data2);
                executing.executeComplete = cycle;
                RS[rs_loadReady].ins = executing;
                loadAddressReady = true;
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
            }
            if(rs_storeReady > -1) {
                Instruction executing = RS[rs_storeReady].ins;
                RS[rs_storeReady].executing = true;
                int memAddress = agu.evaluate(Opcode.ADD,executing.data1,executing.data2);
                executing.memAddress = memAddress;
                executing.executeComplete = cycle;
                RS[rs_storeReady].A = memAddress;
                ROB.buffer[RS[rs_storeReady].robIndex].address = memAddress;
                storeAddressReady = true;
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
            }
            if(rs_otherReady > -1) {
                Instruction executing = RS[rs_otherReady].ins;
                RS[rs_otherReady].executing = true;
                executing.executeComplete = cycle;
                int i = finishedInsts.indexOf(executing);
                finishedInsts.set(i,executing);
                executionResults.add(executing);
                // do nothing here
            }
        }
        Instruction alu0_result = alu0.execute();
        Instruction alu1_result = alu1.execute();
        if(alu0_result != null && alu0_result.result != null) {
            executionResults.add(alu0_result);
//            resultForwarding2(alu0_result);
            resultForwardingFromRS(alu0_result);
            alu0.reset();
            executedInsts++;
        }
        if(alu1_result != null && alu1_result.result != null) {
            executionResults.add(alu1_result);
//            resultForwarding2(alu1_result);
            resultForwardingFromRS(alu1_result);
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

    }

    private void Memory() {
        while(!executionResults.isEmpty()) {
            Instruction executed = executionResults.remove();
            switch (executed.opcode) {
                case LD:
                case LDI:
                    executed.result = mem[executed.memAddress];
                    resultForwardingFromRS(executed);
                    break;
                default: // non memory instructions, only do result forwarding
                    resultForwardingFromRS(executed);
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
            int rsIndex = writeBack.rsIndex;
            int robIndex = RS[rsIndex].robIndex;

            if(writeBack.Rd != 0) {
                if(writeBack.opType.equals(OpType.STORE)) { // store instructions
                    ROB.buffer[ROB.head].value = RS[rsIndex].Vs;
                }
                else {
                    ROB.buffer[robIndex].value = writeBack.result;
                }
                RS[rsIndex] = new ReservationStation(); // clear RS entry
                resultForwardingFromRS(writeBack);
                ROB.buffer[robIndex].ready = true;
            }
            if(writeBack.opType.equals(OpType.OTHER)) {
                ROB.buffer[robIndex].ready = true;
            }
            int i = finishedInsts.indexOf(writeBack);
            finishedInsts.set(i,writeBack);
        }
    }

    private void resultForwardingFromRS(Instruction forwarding) {
        int b = RS[forwarding.rsIndex].robIndex;
        if(b == -1) {
            return;
        }
        for(ReservationStation rs : RS) {
            if(rs.Q1 == b) {
                rs.V1 = forwarding.result;
                rs.Q1 = -1;
            }
            if(rs.Q2 == b) {
                rs.V2 = forwarding.result;
                rs.Q2 = -1;
            }
            if(rs.Qs == b) {
                rs.Vs = forwarding.result;
                rs.Qs = -1;
            }
        }
    }

    private void resultForwardingFromROB(int robIndex, int value) {
        for(ReservationStation rs : RS) {
            if(rs.Q1 == robIndex) {
                rs.V1 = value;
                rs.Q1 = -1;
            }
            if(rs.Q2 == robIndex) {
                rs.V2 = value;
                rs.Q2 = -1;
            }
            if(rs.Qs == robIndex) {
                rs.Vs = value;
                rs.Qs = -1;
            }
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
            else if(robHead.ins.opcode.equals(Opcode.HALT)) {
                finished = true;
            }
            else { // update registers with result
                int Rd = robHead.destination;
                rf[Rd] = robHead.value;
                resultForwardingFromROB(h,robHead.value);
                if(regStats[Rd].busy && regStats[Rd].robIndex == h) {
                    regStats[Rd] = new RegisterStatus(); // free up register status entry
                }
            }
            ROB.pop(); // free up ROB entry, new head
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
            if(fetchBlocked || decodeBlocked || issueBlocked || dispatchBlocked || executeBlocked || euAllBusy) {
                stalledCycle++;
            }
            else if(nothingToDecode || nothingToIssue || nothingToDispatch || nothingToExecute || nothingToMemory || nothingToWriteBack) {
                waitingCycle++;
            }
            System.out.println("PC: " + pc);
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
        System.out.println(waitingCycle + " Waiting cycles");
        System.out.println("cycles/instruction ratio: " + ((float) cycle) / (float) executedInsts);
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
        System.out.println("wasted_cycle/cycle ratio: " + ((float) (stalledCycle + waitingCycle) / (float) cycle));
    }
}
