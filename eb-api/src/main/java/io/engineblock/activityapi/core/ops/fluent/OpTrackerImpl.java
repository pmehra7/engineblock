package io.engineblock.activityapi.core.ops.fluent;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import io.engineblock.activityapi.core.Activity;
import io.engineblock.activityapi.core.ActivityDefObserver;
import io.engineblock.activityapi.core.ops.fluent.opfacets.*;
import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;

/**
 * This tracker keeps track of the state of operations associated with it.
 *
 * @param <D> The payload data type of the associated Op, based on OpImpl
 */
public class OpTrackerImpl<D> implements OpTracker<D>, ActivityDefObserver {
    private final AtomicInteger pendingOps = new AtomicInteger(0);
    private final String label;
    private final long slot;
    private final Timer cycleServiceTimer;
    private final Timer cycleResponseTimer;
    private final Counter pendingOpsCounter;

    private int maxPendingOps =1;
    private LongFunction<D> cycleOpFunction;


    public OpTrackerImpl(Activity activity, long slot) {
        this.slot = slot;
        this.label = "tracker-" + slot + "_" + activity.getAlias();

        this.pendingOpsCounter = activity.getInstrumentation().getOrCreatePendingOpCounter();
        this.cycleServiceTimer = activity.getInstrumentation().getOrCreateCyclesServiceTimer();
        this.cycleResponseTimer = activity.getInstrumentation().getCyclesResponseTimerOrNull();
    }

    // for testing
    public OpTrackerImpl(String name, int slot, Timer cycleServiceTimer, Timer cycleResponseTimer, Counter pendingOpsCounter) {
        this.label = name;
        this.slot = slot;
        this.cycleResponseTimer = cycleResponseTimer;
        this.cycleServiceTimer = cycleServiceTimer;
        this.pendingOpsCounter = pendingOpsCounter;
    }

    @Override
    public void onOpStarted(StartedOp<D> op) {
        pendingOps.incrementAndGet();
        pendingOpsCounter.inc();
    }

    @Override
    public void onOpCompleted(CompletedOp<D> op) {
        pendingOpsCounter.dec();
        int pending = this.pendingOps.decrementAndGet();

        cycleServiceTimer.update(op.getServiceTimeNanos(), TimeUnit.NANOSECONDS);
        if (cycleResponseTimer !=null) { cycleResponseTimer.update(op.getResponseTimeNanos(), TimeUnit.NANOSECONDS); }

        if (pending< maxPendingOps) {
            synchronized (this) {
                notify();
            }
        }
    }

    @Override
    public void setMaxPendingOps(int maxPendingOps) {
        this.maxPendingOps =maxPendingOps;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public boolean isFull() {
        return this.pendingOps.intValue()>=maxPendingOps;
    }

    @Override
    public int getPendingOps() {
        return pendingOps.intValue();
    }

    @Override
    public void setCycleOpFunction(LongFunction<D> newOpFunction) {
        this.cycleOpFunction = newOpFunction;
    }

    @Override
    public TrackedOp<D> newOp(long cycle, OpEvents<D> strideTracker) {
        D opstate = cycleOpFunction.apply(cycle);
        OpImpl<D> op = new EventedOpImpl<>(this,strideTracker);
        op.setCycle(cycle);
        op.setData(opstate);
        return op;
    }

    public int getMaxPendingOps() {
        return maxPendingOps;
    }

    @Override
    public synchronized boolean awaitCompletion(long timeout) {
        long endAt = System.currentTimeMillis() + timeout;
        while (getPendingOps() > 0 && System.currentTimeMillis() < endAt) {
            try {
                long waitfor = Math.max(0, endAt - System.currentTimeMillis());
                wait(waitfor);
            } catch (InterruptedException ignored) {
            }
        }
        return getPendingOps() == 0;
    }


    @Override
    public String toString() {
        return "OpTracker-" + label + ":" + this.slot + " " + this.pendingOps.get() + "/" + maxPendingOps + " ops ";
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        this.maxPendingOps=getMaxPendingOpsForThisThread(activityDef);
    }

    private int getMaxPendingOpsForThisThread(ActivityDef def) {
        int maxTotalOpsInFlight = def.getParams().getOptionalInteger("async").orElse(1);
        int threads = def.getThreads();
        return (maxTotalOpsInFlight / threads) + (slot < (maxTotalOpsInFlight % threads) ? 1 : 0);
    }


}
