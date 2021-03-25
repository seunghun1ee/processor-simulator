public class ExecutionResult { // Processor4

    public Integer execData = null;
    public Integer execAddress = null;
    public Opcode memOp = null;
    public Integer memRd = null;
    public Integer memAddress = null;

    public ExecutionResult(Integer execData, Integer execAddress) {
        this.execData = execData;
        this.execAddress = execAddress;
    }

    public ExecutionResult(Opcode memOp, Integer memRd, Integer memAddress) {
        this.memOp = memOp;
        this.memRd = memRd;
        this.memAddress = memAddress;
    }

    public ExecutionResult(Integer execData, Integer memAddress, Opcode memOp) {
        this.execData = execData;
        this.memOp = memOp;
        this.memAddress = memAddress;
    }
}
