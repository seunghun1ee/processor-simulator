public class LSU2 extends ExecutionUnit2{

    public LSU2() {

    }

    public Integer agu() {
        if(this.executing.data1 == null || this.executing.data2 == null) {
            return null;
        }
        return this.executing.data1 + this.executing.data2;
    }
}
