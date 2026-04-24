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
    private interface RunnableAndStoppable extends Runnable {
        void cancel();
    }

    private final List<Thread> threads;
    private final State state = new State();

    private static class State {
        private final Queue<RunnableAndStoppable> queue = new ArrayDeque<>();
        private boolean closed = false;
    }

    private class ResultCollector<R> {
        private List<R> result;
        private int remainingTasks;
        private boolean cancel = false;
        private RuntimeException error;

        public ResultCollector(int size, int remainingTasks) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            this.remainingTasks = remainingTasks;
            error = null;
        }

        public synchronized void cancelTask() {
            cancel = true;
            remainingTasks = 0;
            notify();
        }

        public synchronized void addError(RuntimeException e) {
            if (cancel) {
                return;
            }
            if (error == null) {
                error = e;
            } else {
                error.addSuppressed(e);
            }
        }

        public synchronized void decreaseCounter() {
            if (cancel) {
                return;
            }
            if (--remainingTasks == 0) {
                notify();
            }
        }

        public void setValue(int index, R value) {
            if (cancel) {
                return;
            }
            result.set(index, value);
        }

        synchronized public List<R> await() throws InterruptedException {
            while (remainingTasks != 0) {
                wait();
            }
            if (error != null) {
                throw error;
            }
            if (cancel) {
                return null;
            }
            return result;
        }
    }

    private static <T, R> void processRange(int begin, int end, List<? extends T> list,
            Function<? super T, ? extends R> f, ResultCollector<R> rc) {
        for (int i = begin; i < end; i++) {
            try {
                rc.setValue(i, f.apply(list.get(i)));
            } catch (RuntimeException e) {
                rc.addError(e);
            }
        }
    }

    private static class Request<T, R> implements RunnableAndStoppable {
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

        public void cancel() {
            rc.cancelTask();
        }

        @Override
        public void run() {
            processRange(begin, end, list, f, rc);
            rc.decreaseCounter();
        }
    }

    private final Runnable THREAD_WORKER = () -> {
        while (true) {
            final RunnableAndStoppable request;
            synchronized (state) {
                while (state.queue.isEmpty() && !state.closed) {
                    try {
                        state.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (state.queue.isEmpty()) {
                    return;
                }
                request = state.queue.remove();
                if (state.closed) {
                    request.cancel();
                    continue;
                }
            }
            request.run();
        }
    };

    private void createAndRunThread(int index) {
        threads.set(index, new Thread(THREAD_WORKER));
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
        synchronized (state) {
            state.closed = true;
            state.notifyAll();
        }
        if (threads == null) {
            return;
        }
        int index = 0;
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
            synchronized (state) {
                if (state.closed) {
                    throw new IllegalStateException("Mapper is closed");
                }
            }
            ResultCollector<R> rc = new ResultCollector<>(args.size(), 0);
            processRange(0, args.size(), args, f, rc);
            return rc.await();
        }
        ResultCollector<R> rc = new ResultCollector<>(args.size(), args.size());
        List<RunnableAndStoppable> requests = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            requests.add(new Request<T, R>(i, i + 1, args, f, rc));
        }
        synchronized (state) {
            if (state.closed) {
                throw new IllegalStateException("Mapper is closed");
            }
            state.queue.addAll(requests);
            state.notifyAll();
        }
        return rc.await();
    }
}
