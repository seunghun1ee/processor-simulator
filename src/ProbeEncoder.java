import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ProbeEncoder {

    private final List<Probe> probes;
    private final int cycles;

    public ProbeEncoder(List<Probe> probes, int cycles) {
        this.probes = probes;
        this.cycles = cycles;
    }

    public void createProbe(String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        for(int i = 0; i < cycles; i++) {
            boolean firstProbe = true;
            for(Probe probe : probes) {
                if(probe.cycle == i) {
                    if(firstProbe) {
                        printWriter.printf("%d,%d",probe.probe_event,probe.inst_id);
                        firstProbe = false;
                    }
                    else {
                        printWriter.printf(":%d,%d",probe.probe_event,probe.inst_id);
                    }
                }
            }
            if(firstProbe) {
                printWriter.printf("-\n");
            }
            else {
                printWriter.printf("\n");
            }
        }
        printWriter.close();
    }
}
