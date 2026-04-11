package info.kgeorgiy.ja.davydov.iterative;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> threads;
    private Queue<Runnable> queue = new ArrayDeque<>();

    private class ResultCollector<R> {
        private List<R> result;
        private int remainingTasks;
        private RuntimeException error;

        public ResultCollector(int size, int remainingTasks) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            this.remainingTasks = remainingTasks;
            error = null;
        }

        synchronized public void addError(RuntimeException e) {
            if (error == null) {
                error = e;
            } else {
                error.addSuppressed(e);
            }
        }

        synchronized public void dicreaseCounter() {
            if (--remainingTasks == 0) {
                notify();
            }
        }

        synchronized public void setValue(int index, R value) {
            result.set(index, value);
        }

        synchronized public List<R> await() throws InterruptedException {
            while (remainingTasks != 0) {
                wait();
            }
            if (error != null) {
                throw error;
            }
            return result;
        }
    }

    private class Request<T, R> implements Runnable {
        private int begin, end;
        private List<? extends T> list;
        private Function<? super T, ? extends R> f;
        ResultCollector<R> rc;

        public Request(int begin, int end, List<? extends T> list, Function<? super T, ? extends R> f,
                ResultCollector<R> rc) {
            this.begin = begin;
            this.end = end;
            this.list = list;
            this.f = f;
            this.rc = rc;
        }

        @Override
        public void run() {
            for (int i = begin; i < end; i++) {
                try {
                    rc.setValue(i, f.apply(list.get(i)));
                } catch (RuntimeException e) {
                    rc.addError(e);
                }
            }
            rc.dicreaseCounter();
        }
    }

    private void createAndRunThread(int index) {
        threads.set(index, new Thread(() -> {
            while (!Thread.interrupted()) {
                Runnable request = null;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    request = queue.poll();
                }
                if (request != null) {
                    request.run();
                } else {
                    break;
                }
            }
        }));
        threads.get(index).start();
    }

    public ParallelMapperImpl(int threads) {
        this.threads = new ArrayList<>(Collections.nCopies(threads, null));
        for (int i = 0; i < threads; i++) {
            createAndRunThread(i);
        }
    }

    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        int countTasks = Math.min(args.size(), 2 * threads.size());
        ResultCollector<R> rc = new ResultCollector<>(args.size(), countTasks);
        int pointer = 0;
        for (int i = 0; i < countTasks; i++) {
            int end = pointer + (args.size() / countTasks) + (i < args.size() % countTasks ? 1 : 0);
            int begin = pointer;
            synchronized (queue) {
                queue.add(new Request<T, R>(begin, end, args, f, rc));
                queue.notify();
            }
            pointer = end;
        }
        return rc.await();
    }
}
