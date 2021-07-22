import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadPool {
    private BlockingQueue<Runnable> taskQueue;
    private List<Worker> runnables = new ArrayList<>();
    private boolean isStopped;

    public ThreadPool(int allThreadsCount, int allTasksCount) {
        this.taskQueue = new ArrayBlockingQueue<Runnable>(allTasksCount);
        this.isStopped = false;

        for(int i = 0; i < allThreadsCount; ++i) {
            this.runnables.add(new Worker(this.taskQueue));
        }
        for(Worker runnable : this.runnables) {
            new Thread(runnable).start();
        }
    }

    public synchronized void  execute(Runnable task) throws Exception {
        if(this.isStopped) throw
                new IllegalStateException("ThreadPool has been stopped");

        this.taskQueue.offer(task);
    }

    public synchronized void stop(boolean quiet) {
        this.isStopped = true;
        for(Worker runnable : this.runnables) {
            runnable.doStop(quiet);
        }
    }

    public synchronized void killThreads(){
        for(Worker runnable : this.runnables) {
            runnable.inter();
        }
    }

    public synchronized void waitUntilAllTasksFinished() {
        while(!this.taskQueue.isEmpty()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
