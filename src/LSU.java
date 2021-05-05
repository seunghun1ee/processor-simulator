public class LSU extends ExecutionUnit {

    public LSU() {

    }

    public Integer agu() {
        if(this.executing.data1 == null || this.executing.data2 == null) {
            return null;
        }
        return this.executing.data1 + this.executing.data2;
    }
}
