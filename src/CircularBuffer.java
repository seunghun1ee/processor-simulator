public class CircularBuffer<T> {

    public int capacity;
    public T[] buffer;
    private int size = 0;
    public int head = 0;
    public int tail = 0;

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

    public int push(T element) {
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

    public T peek() {
        return buffer[head];
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
