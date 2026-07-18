package com.contactfront.engine;

import java.util.concurrent.*;
import java.util.logging.Logger;

public final class TickProcessor {
    private static final Logger LOGGER = Logger.getLogger(TickProcessor.class.getName());
    
    private final TacticalEngine engine;
    private final long tickMs;
    private volatile boolean running = false;
    private volatile long currentTick = 0;
    private volatile long lastTickTime = 0;
    
    private ScheduledExecutorService scheduler;
    private Future<?> tickTask;
    
    private final TickListener listener;
    
    public interface TickListener {
        void onTick(long tickNumber, long elapsedMs);
        void onRunning();
        void onStopped();
    }
    
    public TickProcessor(TacticalEngine engine, long tickMs, TickListener listener) {
        this.engine = engine;
        this.tickMs = tickMs;
        this.listener = listener;
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        currentTick = 0;
        lastTickTime = System.currentTimeMillis();
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TickProcessor-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        
        tickTask = scheduler.scheduleAtFixedRate(this::processTick, 
            0, tickMs, TimeUnit.MILLISECONDS);
        
        if (listener != null) {
            listener.onRunning();
        }
    }
    
    public void stop() {
        running = false;
        
        if (tickTask != null && !tickTask.isDone()) {
            tickTask.cancel(false);
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (listener != null) {
            listener.onStopped();
        }
    }
    
    private void processTick() {
        if (!running) return;
        
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastTickTime;
        
        if (elapsedMs < tickMs) {
            return;
        }
        
        lastTickTime = now;
        currentTick++;
        
        engine.tick();
        
        if (listener != null) {
            listener.onTick(currentTick, now);
        }
    }
    
    public void pause() {
        running = false;
    }
    
    public void resume() {
        if (running) return;
        running = true;
        lastTickTime = System.currentTimeMillis();
        
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TickProcessor-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });
        }
        
        tickTask = scheduler.scheduleAtFixedRate(this::processTick,
            0, tickMs, TimeUnit.MILLISECONDS);
        
        if (listener != null) {
            listener.onRunning();
        }
    }
    
    public long getCurrentTick() {
        return currentTick;
    }
    
    public long getElapsedMs() {
        return lastTickTime;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public long getTickRate() {
        return tickMs;
    }
}