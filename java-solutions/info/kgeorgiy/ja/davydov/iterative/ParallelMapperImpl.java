package info.kgeorgiy.ja.davydov.iterative;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

/**
 * Thread-pool based implementation of {@link ParallelMapper}.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads;
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private boolean closed = false;

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

        public void setValue(int index, R value) {
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
            while (true) {
                final Runnable request;
                synchronized (queue) {
                    while (queue.isEmpty() && !closed) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    if (queue.isEmpty()) {
                        return;
                    }
                    request = queue.remove();
                }
                request.run();
            }
        }));
        threads.get(index).start();
    }

    /**
     * Creates mapper with a fixed number of worker threads.
     *
     * @param threads number of worker threads.
     */
    public ParallelMapperImpl(int threads) {
        if (threads > 1) {
            this.threads = new ArrayList<>(Collections.nCopies(threads, null));
            for (int i = 0; i < threads; i++) {
                createAndRunThread(i);
            }
        } else {
            this.threads = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see ParallelMapper#close()
     */
    @Override
    public void close() {
        boolean interrupted = false;
        synchronized (queue) {
            closed = true;
            queue.notifyAll();
        }
        int index = 0;
        if (threads == null) {
            return;
        }
        while (index < threads.size()) {
            if (!threads.get(index).isAlive()) {
                index++;
                continue;
            }
            try {
                threads.get(index).join();
                index++;
            } catch (InterruptedException e) {
                if (!interrupted) {
                    interrupted = true;
                    for (Thread t : threads) {
                        t.interrupt();
                    }
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see ParallelMapper#map(Function, List)
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (threads == null) {
            synchronized (queue) {
                if (closed) {
                    throw new IllegalStateException("Mapper is closed");
                }
            }
            List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
            RuntimeException error = null;
            for (int i = 0; i < args.size(); i++) {
                try {
                    result.set(i, f.apply(args.get(i)));
                } catch (RuntimeException e) {
                    if (error == null) {
                        error = e;
                    } else {
                        error.addSuppressed(e);
                    }
                }
            }
            if (error != null) {
                throw error;
            }
            return result;
        }
        int countTasks = Math.min(args.size(), 2 * threads.size());
        ResultCollector<R> rc = new ResultCollector<>(args.size(), countTasks);
        int pointer = 0;
        synchronized (queue) {
            if (closed) {
                throw new IllegalStateException("Mapper is closed");
            }
            for (int i = 0; i < countTasks; i++) {
                int end = pointer + (args.size() / countTasks) + (i < args.size() % countTasks ? 1 : 0);
                int begin = pointer;
                queue.add(new Request<T, R>(begin, end, args, f, rc));
                pointer = end;
            }
            queue.notifyAll();
        }
        return rc.await();
    }
}
