public class Probe {
    public int cycle;
    public int probe_event;
    public int inst_id;

    public Probe(int cycle, int probe_event, int inst_id) {
        this.cycle = cycle;
        this.probe_event = probe_event;
        this.inst_id = inst_id;
    }
}
