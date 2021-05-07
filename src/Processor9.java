import java.io.IOException;
import java.util.*;

public class Processor9 {

    // Configures
    int superScalarWidth = 4;
    BranchMode branchMode = BranchMode.DYNAMIC_2BIT;
    int numOfALU = 4;
    int numOfLOAD = 2;
    int numOfSTORE = 1;
    int numOfBRU = 1;

    int cycle = 0;
    int pc = 0; // Program counter
    int executedInsts = 0; // Number of instructions executed
    int stalledCycle = 0;
    int waitingCycle = 0;
    int correctPrediction = 0;
    int misprediction = 0;
    int insIdCount = 1; // for assigning id to instructions
    int[] mem; // memory from user
    int[] rf = new int[32]; //Register file (physical)
    RegisterStatus[] regStats = new RegisterStatus[rf.length];
    Instruction[] instructions; // instructions from user
    boolean beforeFinish = false;
    boolean finished = false;
    int RS_SIZE = 64;
    int QUEUE_SIZE = 4;
    int cycleBeforeFinish = 0;
    int executedInstsBeforeFinish = 0;

    // components
    Queue<Instruction> fetchedQueue = new LinkedList<>();
    Queue<Instruction> decodedQueue = new LinkedList<>();
    ReservationStation[] RS; // unified reservation station
    CircularBufferROB ROB; // Reorder buffer
    Set<Integer> dispatchedIndexSet = new HashSet<>();
    Queue<Instruction> loadBuffer = new LinkedList<>();
    // final result registers before write back
    Queue<Instruction> beforeWriteBack = new LinkedList<>();
    Map<Integer,BTBstatus> BTB_2BIT = new HashMap<>(); // 2-bit Branch Target Buffer, Key: insAddress, Value: BTB status
    Map<Integer,Boolean> BTB_1BIT = new HashMap<>(); // 1-bit Branch Target Buffer, Key: insAddress, Value: Boolean
    ALU[] ALUs;
    LSU[] LOADs;
    LSU[] STOREs;
    BRU[] BRUs;
    int otherReadyIndex = -1;

    // state of pipeline stages
    // fetch states
    boolean fetchBlocked = false;
    // decode states
    boolean nothingToDecode = false;
    boolean decodeBlocked = false;
    boolean branchTaken = false;
    // issue states
    boolean nothingToIssue = false;
    boolean issueBlocked = false;
    // dispatch states
    boolean noReadyInstruction = false;
    boolean loadStage1Blocked = false;
    // execute states
    boolean nothingToExecute = false;
    boolean euAllBusy = false;
    // memory states
    boolean loadBufferFull = false;
    boolean loadStage2Blocked = false;
    // commit states
    boolean robEmpty = false;

    //For visualisation
    List<Instruction> finishedInsts = new ArrayList<>();
    List<Probe> probes = new ArrayList<>();

    public Processor9(int[] mem, Instruction[] instructions) {
        this.mem = mem;
        this.instructions = instructions;
    }

    public Processor9(int[] mem, Instruction[] instructions, int superScalarWidth, BranchMode branchMode, int numOfALU, int numOfLOAD, int numOfSTORE, int numOfBRU, int RS_SIZE) {
        this.mem = mem;
        this.instructions = instructions;
        this.superScalarWidth = superScalarWidth;
        this.branchMode = branchMode;
        this.numOfALU = numOfALU;
        this.numOfLOAD = numOfLOAD;
        this.numOfSTORE = numOfSTORE;
        this.numOfBRU = numOfBRU;
        this.RS_SIZE = RS_SIZE;
    }

    private void Fetch() {
        fetchBlocked = !fetchedQueue.isEmpty();
        // fetching is not blocked, not finished, pc is within instruction memory range
        if(!fetchBlocked && !finished && pc < instructions.length) {
            for(int i = 0; i < superScalarWidth; i++) {
                Instruction fetching = fetchLogic();
                fetchedQueue.add(fetching);

                // add instruction to debugging list
                fetching.fetchComplete = cycle;
                finishedInsts.add(fetching);
            }
        }
        if(fetchBlocked) {
            probes.add(new Probe(cycle,0,0));
        }
    }

    private Instruction fetchLogic() {
        Instruction fetching = instructions[pc];
        if(fetching == null) {
            fetching = new Instruction(); // null fetched as NOOP
        }
        Instruction ins = new Instruction(fetching);
        ins.id = insIdCount; // assign id
        insIdCount++;
        ins.insAddress = pc; // assign instruction address
        pc++;
        return ins;
    }

    private void Decode() {
        nothingToDecode = fetchedQueue.isEmpty();
        decodeBlocked = !decodedQueue.isEmpty();
        if(decodeBlocked && !nothingToDecode) {
            Instruction ins = fetchedQueue.peek();
            probes.add(new Probe(cycle,1,ins.id));
        }
        branchTaken = false;
        if(!nothingToDecode && !decodeBlocked) {
            for(int i = 0; i < superScalarWidth; i++) {
                if(!branchTaken) {
                    Instruction decoding = fetchedQueue.remove();
                    decodeLogic(decoding);
                }
            }
            branchTaken = false;
        }
    }

    private void decodeLogic(Instruction decoding) {
        OpType opType = assignOpType(decoding.opcode);
        if(opType.equals(OpType.UNDEFINED)) {
            System.out.println("Undefined instruction detected at decode stage at cycle: " + cycle);
            finished = true;
            return;
        }
        decoding.opType = opType;
        // handle branches except BRR
        switch (decoding.opcode) {
            case BR:
                pc = decoding.Const;
                fetchedQueue.clear();
                branchTaken = true;
                return;
            case JMP:
                pc += decoding.Const;
                fetchedQueue.clear();
                branchTaken = true;
                return;
            case BRZ:
            case BRN:
                decoding = branchPrediction(decoding);
                branchTaken = decoding.taken;
                break;
            default:
                break;
        }

        decodedQueue.add(decoding);
        saveDecodeCycle(decoding,cycle);
    }

    //return value: updated instruction object
    private Instruction branchPrediction(Instruction ins) {
        switch (branchMode) {
            case NONE:
                ins.predicted = false;
                ins.taken = false;
                break;
            case FIXED_NOT_TAKEN:
                ins.predicted = true;
                ins.taken = false;
                break;
            case FIXED_TAKEN:
                ins.predicted = true;
                ins.taken = true;
                pc = ins.Const;
                fetchedQueue.clear();
                break;
            case STATIC:
                ins.predicted = true;
                ins.taken = staticBranchPredictor(ins);
                if(ins.taken) {
                    pc = ins.Const;
                    fetchedQueue.clear();
                }
                break;
            case DYNAMIC_1BIT:
                ins.predicted = true;
                ins.taken = dynamicPredictor1Bit(ins);
                if(ins.taken) {
                    pc = ins.Const;
                    fetchedQueue.clear();
                }
                break;
            case DYNAMIC_2BIT:
                ins.predicted = true;
                ins.taken = dynamicPredictor2Bit(ins);
                if(ins.taken) {
                    pc = ins.Const;
                    fetchedQueue.clear();
                }
                break;
        }
        return new Instruction(ins);
    }

    // return values of predictors = true if taken, false if not taken
    private boolean staticBranchPredictor(Instruction ins) {
        // take backward branch, not take forward branch
        return ins.Const <= pc;
    }

    private boolean dynamicPredictor1Bit(Instruction ins) {
        int btbAddress = ins.insAddress & 0x0000FFFF;
        Boolean condition = BTB_1BIT.get(btbAddress);
        probes.add(new Probe(cycle,14,ins.id));
        if(condition == null) {
            boolean newCondition = staticBranchPredictor(ins);
            BTB_1BIT.put(btbAddress,newCondition);
            return newCondition;
        }
        return condition;
    }

    private boolean dynamicPredictor2Bit(Instruction ins) {
        int btbAddress = ins.insAddress & 0x0000FFFF; // using 16bit address
        BTBstatus btbCondition = BTB_2BIT.get(btbAddress); // get saved condition for the insAddress
        probes.add(new Probe(cycle,14,ins.id));
        if(btbCondition == null) { // when there's no saved condition
            if(staticBranchPredictor(ins)) {
                BTB_2BIT.put(btbAddress,BTBstatus.YES);
                return true;
            }
            else {
                BTB_2BIT.put(btbAddress,BTBstatus.NO);
                return false;
            }
        }
        return btbCondition.equals(BTBstatus.YES) || btbCondition.equals(BTBstatus.STRONG_YES);
    }


    private void saveDecodeCycle(Instruction decoding, int cycle) {
        decoding.decodeComplete = cycle; // save cycle number of decode stage
        int i = finishedInsts.indexOf(decoding);
        finishedInsts.set(i,decoding);
    }

    private OpType assignOpType(Opcode opcode) {
        switch (opcode) {
            case HALT:
                return OpType.OTHER;
            case NOOP:
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
                return OpType.ALU;
            case LD:
            case LDI:
                return OpType.LOAD;
            case ST:
            case STI:
                return OpType.STORE;
            case BR: // unconditional branches
            case JMP:
            case BRZ: // conditional branches
            case BRN:
            case BRR: // unconditional branch that is dependent on Rs1
                return OpType.BRU;
            default:
                return OpType.UNDEFINED;
        }
    }

    private void Issue() {
        //in case of decode queue is not full
        int queueSize = Integer.min(superScalarWidth,decodedQueue.size());
        for(int i = 0; i < queueSize; i++) {
            int rsIndex = getRsIndex();
            boolean rsBlocked = rsIndex == -1;
            boolean robBlocked = ROB.size() >= ROB.capacity; // ROB full
            issueBlocked = rsBlocked || robBlocked;
            nothingToIssue = decodedQueue.isEmpty();
            if(!nothingToIssue) {
                Instruction ins = decodedQueue.peek();
                if(rsBlocked) {
                    probes.add(new Probe(cycle,4,ins.id));
                }
                if(robBlocked) {
                    probes.add(new Probe(cycle,5,ins.id));
                }
            }
            if(!issueBlocked && !nothingToIssue) {
                issueLogic(decodedQueue.remove(), rsIndex);
            }
        }
    }

    private int getRsIndex() {
        int rsIndex = -1;
        for(int i = 0; i < RS.length; i++) {
            if(!RS[i].busy) { // there is available rs
                rsIndex = i; // get available rs index
                break;
            }
        }
        return rsIndex;
    }

    private void issueLogic(Instruction ins, int rsIndex) {
        ReorderBuffer allocatedROB = new ReorderBuffer();
        // for all ins
        ins.issueComplete = cycle;
        ins.rsIndex = rsIndex;
        if(ins.Rs1 != 0 && regStats[ins.Rs1].busy) { // there is in-flight ins that writes Rs1
            int Rs1robIndex = regStats[ins.Rs1].robIndex;
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
            RS[rsIndex].V1 = rf[ins.Rs1]; // 0 if Rs1 = 0
            RS[rsIndex].Q1 = -1;
        }
        if(ins.Rs2 != 0 && regStats[ins.Rs2].busy) { // there is in-flight ins that writes Rs2
            int Rs2robIndex = regStats[ins.Rs2].robIndex;
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
            RS[rsIndex].V2 = rf[ins.Rs2]; // 0 if Rs2 = 0
            RS[rsIndex].Q2 = -1;
        }
        // set Reorder Buffer
        allocatedROB.ins = ins;
        allocatedROB.destination = ins.Rd;
        allocatedROB.busy = true;
        allocatedROB.ready = false;

        int robIndex = ROB.push(allocatedROB);
        // set Reservation Station
        RS[rsIndex].op = ins.opcode;
        RS[rsIndex].ins = ins;
        RS[rsIndex].busy = true;
        RS[rsIndex].robIndex = robIndex;
        RS[rsIndex].type = ins.opType;

        switch (ins.opType) {
            case ALU:
                // for ins that only use Const
                if(RS[rsIndex].Q1 == -1 && ins.opcode.equals(Opcode.MOVC)) {
                    RS[rsIndex].V1 += ins.Const;
                }
                // when second operand is ready
                else if(RS[rsIndex].Q2 == -1) {

                    RS[rsIndex].V2 += ins.Const; // for imm instructions
                }
                // set regStats
                if(ins.Rd != 0) {
                    regStats[ins.Rd].robIndex = robIndex;
                    regStats[ins.Rd].busy = true;
                }
                break;
            case LOAD:
                if(RS[rsIndex].ins.opcode.equals(Opcode.LDI)) {
                    RS[rsIndex].V2 = ins.Const; // for LDI
                }
                // set regStats
                if(ins.Rd != 0) {
                    regStats[ins.Rd].robIndex = robIndex;
                    regStats[ins.Rd].busy = true;
                }
                break;
            case STORE:
                if(ins.Rd != 0 && regStats[ins.Rd].busy) { // there is in-flight ins that writes at Rd
                    int storeRobIndex = regStats[ins.Rd].robIndex;
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
                    RS[rsIndex].Vs = rf[ins.Rd]; // 0 if Rd = 0
                    RS[rsIndex].Qs = -1;
                }
                if(RS[rsIndex].ins.opcode.equals(Opcode.STI)) {
                    RS[rsIndex].V2 = ins.Const; // for STI
                }
                // no regStats set for stores
                break;
            case BRU:
                // for ins that only use Const
                if(RS[rsIndex].Q1 == -1 && ins.opcode.equals(Opcode.JMP)) {
                    RS[rsIndex].V1 += ins.Const;
                }
                // when second operand is ready
                else if(RS[rsIndex].Q2 == -1) {
                    RS[rsIndex].V2 += ins.Const; // for imm instructions
                }
                // no regStats set for branch operations
                break;
            case OTHER:
                beforeFinish = true;
                cycleBeforeFinish = cycle;
                executedInstsBeforeFinish = executedInsts;
                break;
            default:
                System.out.println("invalid instruction detected at issue stage");
                finished = true;
                break;
        }

        int i = finishedInsts.indexOf(ins);
        finishedInsts.set(i,ins);
    }

    private void Dispatch() {
        dispatchedIndexSet.clear();
        for(int i=0; i < numOfALU; i++) {
            int readyIndex = getReadyRSIndex(OpType.ALU);
            if(!ALUs[i].busy && readyIndex != -1) {
                dispatchedIndexSet.add(readyIndex);
                dispatchOperands(readyIndex);
                // put it to ALU
                ALUs[i].update(RS[readyIndex].ins);
            }
        }
        for(int i=0; i < numOfLOAD; i++) {
            int readyIndex = getReadyLoadRSIndex();
            loadBufferFull = QUEUE_SIZE - loadBuffer.size() <= i;
            // the load unit is not busy, there's available op, load buffer has space for this op
            if(!LOADs[i].busy && readyIndex != -1 && !loadBufferFull) {
                dispatchedIndexSet.add(readyIndex);
                dispatchOperands(readyIndex);
                // put it to LOAD
                LOADs[i].update(RS[readyIndex].ins);
            }
            if(loadBufferFull && readyIndex != -1) {
                probes.add(new Probe(cycle,10,RS[readyIndex].ins.id));
            }
        }
        for(int i=0; i < numOfSTORE; i++) {
            int readyIndex = getReadyRSIndex(OpType.STORE);
            if(!STOREs[i].busy && readyIndex != -1) {
                dispatchedIndexSet.add(readyIndex);
                dispatchOperands(readyIndex);
                // put it to STORE
                STOREs[i].update(RS[readyIndex].ins);
            }
        }
        for(int i=0; i < numOfBRU; i++) {
            int readyIndex = getReadyRSIndex(OpType.BRU);
            if(!BRUs[i].busy && readyIndex != -1) {
                dispatchedIndexSet.add(readyIndex);
                dispatchOperands(readyIndex);
                // put it to BRU
                BRUs[i].update(RS[readyIndex].ins);
            }
        }
        otherReadyIndex = getReadyOtherIndex();
        if(otherReadyIndex != -1) {
            dispatchedIndexSet.add(otherReadyIndex);
            saveDispatchCycle(RS[otherReadyIndex].ins,cycle);
        }
        noReadyInstruction = dispatchedIndexSet.isEmpty() && otherReadyIndex == -1;
    }

    private int getReadyRSIndex(OpType opType) {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i=0; i < RS.length; i++) {
            if(
                    RS[i].busy
                    && !RS[i].executing
                    && RS[i].type.equals(opType)
                    && RS[i].Q1 == -1
                    && RS[i].Q2 == -1
                    && RS[i].Qs == -1
                    && !dispatchedIndexSet.contains(i)
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

    private int getReadyLoadRSIndex() {
        int priority = Integer.MAX_VALUE;
        int readyIndex = -1;
        for(int i=0; i < RS.length; i++) {
            if(
                    RS[i].busy
                    && !RS[i].executing
                    && RS[i].type.equals(OpType.LOAD)
                    && RS[i].Q1 == -1
                    && RS[i].Q2 == -1
                    && RS[i].Qs == -1
                    && !dispatchedIndexSet.contains(i)
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
                    && !dispatchedIndexSet.contains(i)
            ) {
                if(RS[i].ins.opcode.equals(Opcode.HALT) && !ROB.peek().ins.equals(RS[i].ins)) {
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

    private boolean checkRobForLoadStage1(int currentRobIndex) {
        int j = currentRobIndex;
        while (j != ROB.head) {
            if(j == 0) {
                j = ROB.capacity -1;
            }
            else {
                j--;
            }
            if(ROB.buffer[j] != null && ROB.buffer[j].ins.opType.equals(OpType.STORE)) {
                loadStage1Blocked = true;
                probes.add(new Probe(cycle,11,ROB.buffer[currentRobIndex].ins.id));
                return false;
            }
        }
        loadStage1Blocked = false;
        return true;
    }

    private boolean checkRobForLoadStage2(int currentRobIndex, int loadAddress) {
        int j = currentRobIndex;
        while (j != ROB.head) {
            if(j == 0) {
                j = ROB.capacity -1;
            }
            else {
                j--;
            }
            if(ROB.buffer[j] != null && ROB.buffer[j].busy && ROB.buffer[j].ins.opType.equals(OpType.STORE) && ROB.buffer[j].address == loadAddress) {
                return false;
            }
        }
        return true;
    }

    private void dispatchOperands(int rs_index) {
        //dispatch operands
        RS[rs_index].ins.data1 = RS[rs_index].V1;
        RS[rs_index].ins.data2 = RS[rs_index].V2;
        saveDispatchCycle(RS[rs_index].ins, cycle);
    }

    private void saveDispatchCycle(Instruction dispatched, int cycle) {
        dispatched.dispatchComplete = cycle; // save cycle number of dispatch stage
        int i = finishedInsts.indexOf(dispatched);
        finishedInsts.set(i,dispatched);
    }

    private void Execute() {
        nothingToExecute = getNumberOfReadyEUs() == 0;
        euAllBusy = getNumberOfBusyEUs() == ALUs.length + LOADs.length + STOREs.length + BRUs.length;
        if(euAllBusy) {
            probes.add(new Probe(cycle,9,0));
        }
        bruExecution();
        aluExecution();
        loadExecution();
        storeExecution();
        if(otherReadyIndex != -1) {
            RS[otherReadyIndex].executing = true;
            Instruction ins = RS[otherReadyIndex].ins;
            saveExecuteCycle(ins);
            ROB.buffer[RS[otherReadyIndex].robIndex].ready = true;
            executedInsts++;
        }
        otherReadyIndex = -1;
    }

    private int getNumberOfReadyEUs() {
        int actives = 0;
        for(ALU alu : ALUs) {
            if(alu.ready) actives++;
        }
        for(LSU load : LOADs) {
            if(load.ready) actives++;
        }
        for(LSU store : STOREs) {
            if(store.ready) actives++;
        }
        for(BRU bru : BRUs) {
            if(bru.ready) actives++;
        }
        return actives;
    }

    private int getNumberOfBusyEUs() {
        int actives = 0;
        for(ALU alu : ALUs) {
            if(alu.busy) actives++;
        }
        for(LSU load : LOADs) {
            if(load.busy) actives++;
        }
        for(LSU store : STOREs) {
            if(store.busy) actives++;
        }
        for(BRU bru : BRUs) {
            if(bru.busy) actives++;
        }
        return actives;
    }

    private void aluExecution() {
        for(int i=0; i < numOfALU; i++) {
            int rsIndex = ALUs[i].destination;
            Integer result = ALUs[i].execute();
            if(rsIndex != -1) {
                RS[rsIndex].executing = true;
                saveExecuteCycle(RS[rsIndex].ins);
            }
            if(result != null) {
                // update RS and ROB with result
                int robIndex = RS[rsIndex].robIndex;
                if(RS[rsIndex].ins.Rd != 0) { // if Rd is 0, result is ignored
                    RS[rsIndex].ins.result = result;
                    ROB.buffer[robIndex].value = result;
                }
                ROB.buffer[robIndex].ready = true;
                // result forwarding
                resultForwardingFromRS(RS[rsIndex].ins);
                // clear used ALU
                ALUs[i].reset();
                executedInsts++;
            }
        }
    }

    private void loadExecution() {
        for(int i=0; i < numOfLOAD; i++) {
            int rsIndex = LOADs[i].destination;
            Integer memAddress = LOADs[i].agu();
            if(rsIndex != -1) {
                RS[rsIndex].executing = true;
                saveExecuteCycle(RS[rsIndex].ins);
            }
            if(memAddress != null) {
                // update RS
                RS[rsIndex].ins.memAddress = memAddress;
                // push to load buffer
                loadBuffer.add(RS[rsIndex].ins);
                // clear used LOAD
                LOADs[i].reset();
                executedInsts++;
            }
        }
    }

    private void storeExecution() {
        for(int i=0; i < numOfSTORE; i++) {
            int rsIndex = STOREs[i].destination;
            Integer memAddress = STOREs[i].agu();
            if(rsIndex != -1) {
                RS[rsIndex].executing = true;
                saveExecuteCycle(RS[rsIndex].ins);
            }
            if(memAddress != null) {
                // update RS and ROB
                RS[rsIndex].ins.memAddress = memAddress;
                RS[rsIndex].A = memAddress;
                int robIndex = RS[rsIndex].robIndex;
                ROB.buffer[robIndex].value = RS[rsIndex].Vs;
                ROB.buffer[robIndex].address = memAddress;
                ROB.buffer[robIndex].ready = true;
                // clear used STORE
                STOREs[i].reset();
                executedInsts++;
            }
        }
    }

    private void bruExecution() {
        for(int i=0; i < numOfBRU; i++) {
            int rsIndex = BRUs[i].destination;
            if(BRUs[i].executing.opType.equals(OpType.BRU)) {
                RS[rsIndex].executing = true;
                saveExecuteCycle(RS[rsIndex].ins);
                Instruction executing = BRUs[i].executing;
                boolean realCondition = BRUs[i].evaluateCondition();
                branchEvaluation(executing,realCondition);
                if(executing.opcode.equals(Opcode.BRR)) {
                    ROB.buffer[RS[executing.rsIndex].robIndex].value = executing.data1;
                }
                ROB.buffer[RS[executing.rsIndex].robIndex].ready = true;
                BRUs[i].reset();
            }
            // branch prediction evaluation
        }
    }

    private void saveExecuteCycle(Instruction executing) {
        if(executing.executeComplete > 0) {
            return;
        }
        executing.executeComplete = cycle;
        int i = finishedInsts.indexOf(executing);
        finishedInsts.set(i,executing);
    }

    private void branchEvaluation(Instruction executing, boolean realCondition) {
        if(realCondition == executing.taken) {
            // well predicted
            correctPrediction++;
            if(branchMode.equals(BranchMode.DYNAMIC_2BIT)) {
                updateWellPredicted2BitBTB(executing, executing.taken);
            }
        }
        else {
//            // wrong prediction
//            if(!executing.opcode.equals(Opcode.BRR)) {
//                misprediction++;
//            }
            ROB.buffer[RS[executing.rsIndex].robIndex].mispredicted = true;
            probes.add(new Probe(cycle,15,executing.id));
        }
    }

    private void updateWellPredicted2BitBTB(Instruction ins, boolean taken) {
        int btbAddress = ins.insAddress & 0x0000FFFF;
        BTBstatus oldStatus = BTB_2BIT.get(btbAddress);
        if(taken) {
            switch (oldStatus) {
                case STRONG_YES:
                case YES:
                    BTB_2BIT.put(btbAddress,BTBstatus.STRONG_YES);
                    break;
                default:
                    System.out.println("Illegal BTB status at execute stage at cycle: " + cycle);
                    break;
            }
        }
        else {
            switch (oldStatus) {
                case NO:
                case STRONG_NO:
                    BTB_2BIT.put(btbAddress,BTBstatus.STRONG_NO);
                    break;
                default:
                    System.out.println("Illegal BTB status at execute stage at cycle: " + cycle);
                    break;
            }
        }
    }

    private void updateMispredicted2BitBTB(int btbAddress) {
        BTBstatus oldStatus = BTB_2BIT.get(btbAddress);
        if(oldStatus == null) {
            oldStatus = BTBstatus.NO;
        }
        BTBstatus newStatus = oldStatus;
        switch (oldStatus) {
            case STRONG_YES:
            case NO:
                newStatus = BTBstatus.YES;
                break;
            case YES:
            case STRONG_NO:
                newStatus = BTBstatus.NO;
                break;
        }
        BTB_2BIT.put(btbAddress,newStatus);
    }

    private void updateMispredicted1BitBTB(int btbAddress) {
        Boolean oldBool = BTB_1BIT.get(btbAddress);
        if(oldBool == null) {
            BTB_1BIT.put(btbAddress,false);
            return;
        }
        BTB_1BIT.put(btbAddress,!oldBool);
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

    private void Memory() {
        for(int i=0; i < numOfLOAD; i++) {
            Instruction loading = loadBuffer.peek();
            if(loading == null) {
                break;
            }
            if(!loading.opType.equals(OpType.LOAD)) {
                System.out.println("Illegal instruction stored at load buffer at cycle: " + cycle);
                finished = true;
                break;
            }
            if(!checkRobForLoadStage2(RS[loading.rsIndex].robIndex,loading.memAddress)) {
                loadStage2Blocked = true;
                probes.add(new Probe(cycle,11,loading.id));
                break;
            }
            loadStage2Blocked = false;
            // access memory
            loading.result = mem[loading.memAddress];
            // result forwarding
            resultForwardingFromRS(loading);
            // update ROB
            if(loading.Rd != 0) { // if Rd is 0, value is ignored
                ROB.buffer[RS[loading.rsIndex].robIndex].value = mem[loading.memAddress];
            }
            ROB.buffer[RS[loading.rsIndex].robIndex].ready = true;
            loadBuffer.remove();

            loading.memoryComplete = cycle; // save cycle number of memory stage
            int j = finishedInsts.indexOf(loading);
            finishedInsts.set(j,loading);
            beforeWriteBack.add(loading);
        }
    }

    private void Commit() {
        robEmpty = ROB.isEmpty();
        // flushing all ready ops
        while(!ROB.isEmpty()) {
            int headIndex = ROB.head;
            ReorderBuffer robHead = ROB.peek();
            if(!robHead.ready) { // nothing to pop
                return;
            }
            else if(robHead.ins.opType.equals(OpType.BRU)) {
                // branch commit
                if(robHead.mispredicted) {
                    handleMisprediction(robHead);
                }
                else {
                    // save commit cycle
                    int i = finishedInsts.indexOf(robHead.ins);
                    Instruction committing = finishedInsts.get(i);
                    committing.memoryComplete = cycle;
                    committing.commitComplete = cycle;
                    finishedInsts.set(i,committing);

                    ROB.buffer[ROB.head].busy = false;
                    RS[robHead.ins.rsIndex] = new ReservationStation();
                    ROB.pop(); // free up ROB entry, new head
                    return;
                }
            }
            else if(robHead.ins.opType.equals(OpType.STORE)) {
                // store commit
                storeToMemory(robHead);
            }
            else if(robHead.ins.opcode.equals(Opcode.HALT)) {
                finished = true;
            }
            else {
                // normal commits
                int Rd = robHead.destination;
                if(Rd != 0) { // if destination is not zero
                    rf[Rd] = robHead.value; // update register file
                }
                resultForwardingFromROB(headIndex,robHead.value);
                if(regStats[Rd].busy && regStats[Rd].robIndex == headIndex) {
                    regStats[Rd] = new RegisterStatus(); // free up register status entry
                }
            }

            // save commit cycle
            int i = finishedInsts.indexOf(robHead.ins);
            Instruction committing = finishedInsts.get(i);
            if(!robHead.ins.opType.equals(OpType.LOAD)) {
                committing.memoryComplete = cycle;
            }
            committing.commitComplete = cycle;
            finishedInsts.set(i,committing);

            ROB.buffer[ROB.head].busy = false;
            RS[robHead.ins.rsIndex] = new ReservationStation();
            ROB.pop(); // free up ROB entry, new head
        }
    }

    private void storeToMemory(ReorderBuffer robHead) {
        if(robHead.address >= mem.length || robHead.address < 0) {
            finished = true;
            System.out.println("memory index out of range at commit at cycle: " + cycle);
            return;
        }
        mem[robHead.address] = robHead.value; // update memory here
    }

    private void handleMisprediction(ReorderBuffer robHead) {
        int btbAddress = robHead.ins.insAddress & 0x0000FFFF;
        switch (branchMode) {
            case DYNAMIC_1BIT:
                updateMispredicted1BitBTB(btbAddress);
                break;
            case DYNAMIC_2BIT:
                updateMispredicted2BitBTB(btbAddress);
                break;
            default:
                break;
        }
        flushAll();
        // change to correct pc
        if(robHead.ins.taken) {
            pc = robHead.ins.insAddress + 1;
        }
        else if(robHead.ins.opcode.equals(Opcode.BRR)) {
            pc = robHead.value;
        }
        else {
            pc = robHead.ins.Const;
        }
        probes.add(new Probe(cycle,16,robHead.ins.id));
    }

    private void flushAll() {
        // clear reorder buffer
        ROB.clear();
        // clear register status
        for(int i = 0; i < regStats.length; i++) {
            regStats[i] = new RegisterStatus();
        }
        // clear reservation station entries that is later than this branch
        for(int i = 0; i < RS.length; i++) {
            RS[i] = new ReservationStation();
        }
        // flush queues
        fetchedQueue.clear();
        decodedQueue.clear();
        loadBuffer.clear();
        // flush EUs
        for(int i = 0; i < numOfALU; i++) {
            ALUs[i].reset();
        }
        for(int i = 0; i < numOfLOAD; i++) {
            LOADs[i].reset();
        }
        for(int i = 0; i < numOfSTORE; i++) {
            STOREs[i].reset();
        }
        for(int i = 0; i < numOfBRU; i++) {
            BRUs[i].reset();
        }
        beforeFinish = false;
        misprediction++;
    }

    private void init() {
        RS = new ReservationStation[RS_SIZE]; // unified reservation station
        ROB = new CircularBufferROB(RS_SIZE); // Reorder buffer
        ALUs = new ALU[numOfALU];
        LOADs = new LSU[numOfLOAD];
        STOREs = new LSU[numOfSTORE];
        BRUs = new BRU[numOfBRU];
        for(int i=0; i < RS.length; i++) {
            RS[i] = new ReservationStation();
        }
        for(int i=0; i < regStats.length; i++) {
            regStats[i] = new RegisterStatus();
        }
        for(int i=0; i < numOfALU; i++) {
            ALUs[i] = new ALU();
        }
        for(int i=0; i < numOfLOAD; i++) {
            LOADs[i] = new LSU();
        }
        for(int i=0; i < numOfSTORE; i++) {
            STOREs[i] = new LSU();
        }
        for(int i=0; i < numOfBRU; i++) {
            BRUs[i] = new BRU();
        }
    }

    public void RunProcessor() {
        init();
        int cycleLimit = 10000;
        while(!finished && pc < instructions.length && cycle < cycleLimit) {
            Commit();
            Memory();
            Execute();
            Dispatch();
            Issue();
            Decode();
            Fetch();
            cycle++;
//            System.out.println("pc: " + pc + " cycle: " + cycle);

            if(!beforeFinish) {
                if(fetchBlocked || decodeBlocked || issueBlocked || euAllBusy || loadBufferFull || loadStage1Blocked || loadStage2Blocked) {
                    stalledCycle++;
                }
                else if(nothingToDecode || nothingToIssue || noReadyInstruction || nothingToExecute) {
                    waitingCycle++;
                }
            }
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


        System.out.println("Processor Configuration");
        System.out.println("Pipeline width: " + superScalarWidth + ", Branch prediction mode: "+ branchMode.toString());
        System.out.println(numOfALU + " ALUs " + numOfLOAD + " LOADs " + numOfSTORE + " STOREs " + numOfBRU + " BRUs");
        System.out.println("RS/ROB size: " + RS_SIZE);
        System.out.println(executedInsts + " instructions executed");
        System.out.println(cycle + " cycles spent");
        System.out.println(stalledCycle + " stalled cycles");
        System.out.println(waitingCycle + " Waiting cycles");
        System.out.println(correctPrediction + " correct predictions");
        System.out.println(misprediction + " incorrect predictions");
        System.out.println("Instructions/cycle ratio: " + ((float) executedInsts / (float) cycle));
        System.out.println("Subset Instructions/cycle ratio: " + ((float) executedInstsBeforeFinish / (float) cycleBeforeFinish));
        System.out.println("stalled_cycle/cycle ratio: " + ((float) stalledCycle / (float) cycle));
        System.out.println("wasted_cycle/cycle ratio: " + ((float) (stalledCycle + waitingCycle) / (float) cycle));
        System.out.println("Correct prediction rate: "+ ((float) correctPrediction / (float) (correctPrediction + misprediction)));
        System.out.println();
    }

}
