public class CircularBuffer<T> {

    public int capacity;
    private T[] buffer;
    private int size = 0;
    private int head = 0;
    private int tail = 0;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = (T[]) new Object[capacity];
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

    public boolean push(T element) {
        // full
        if(size == capacity) {
            return false;
        }
        buffer[tail] = element;
        // when tail reach the end
        if(tail == capacity - 1) {
            tail = 0;
        }
        else {
            tail++;
        }
        size++;
        return true;
    }

    public T peak() {
        return buffer[head];
    }

    public T peak(int index) {
        return buffer[index];
    }

    public T pop() {
        // empty
        if(size == 0) {
            return null;
        }
        T elem = buffer[head];
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
