public class CircularBufferROB {

    public int capacity;
    public ReorderBuffer[] buffer;
    private int size = 0;
    public int head = 0;
    public int tail = 0;

    public CircularBufferROB(int capacity) {
        this.capacity = capacity;
        this.buffer = new ReorderBuffer[capacity];
    }

    public void clear() {
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int push(ReorderBuffer element) {
        // full
        if(size == capacity) {
            return -1;
        }
        int index = tail;
        buffer[index] = element;
        // when tail reach the end
        if(tail == capacity - 1) {
            tail = 0;
        }
        else {
            tail++;
        }
        size++;
        return index;
    }

    public ReorderBuffer peak() {
        return buffer[head];
    }

    public ReorderBuffer pop() {
        // empty
        if(size == 0) {
            return null;
        }
        ReorderBuffer elem = buffer[head];
        // when head reach the end
        if(head == capacity - 1) {
            head = 0;
        }
        else {
            head++;
        }
        size--;
        return elem;
    }

}

