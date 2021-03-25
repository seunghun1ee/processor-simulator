import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TraceEncoder {

    private final List<Instruction> instructions;

    public TraceEncoder(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public void createTrace(String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        for(Instruction ins : instructions) {
            printWriter.printf("%d:%d:%d:%d:%d:%d:0x%08x:%d:%d:%s %d, %d, %d, %d\n",
                    ins.fetchComplete,
                    ins.decodeComplete,
                    ins.issueComplete,
                    ins.executeComplete,
                    ins.memoryComplete,
                    ins.writeBackComplete,
                    ins.insAddress,
                    0,
                    ins.id,
                    ins.opcode.toString(),
                    ins.Rd,
                    ins.Rs1,
                    ins.Rs2,
                    ins.Const
            );
        }
        printWriter.close();
    }
}
