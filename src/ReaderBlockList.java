import java.util.ArrayList;
import java.util.List;

public class ReaderBlockList {
        private final List<Orange> oranges;

        ReaderBlockList(){
                oranges = new ArrayList<Orange>();
        }

        public void add(Orange o) {
                synchronized (oranges){
                        oranges.add(o);
                        oranges.notify();
                }
        }

        public Orange get() {
                synchronized (oranges) {
                        while (oranges.size() == 0) {
                                try {
                                        oranges.wait();
                                } catch (InterruptedException ignored) {}
                        }
                       return oranges.remove(0);
                }
        }

        public synchronized boolean isEmpty(){
                return oranges.size() == 0;
        }
}

