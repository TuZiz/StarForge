package ym.starforge.transaction;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.logging.*;

/** Single writer and explicit shutdown; never blocks an entity thread. */
public final class OperationLog implements AutoCloseable {
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("StarForge-log").factory());
    private final Path file; private final Logger logger;
    public OperationLog(Path folder,Logger logger){this.file=folder.resolve("operations.log");this.logger=logger;}
    public void record(String text){io.execute(()->{try{Files.createDirectories(file.getParent());Files.writeString(file,java.time.Instant.now()+" "+text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){logger.log(Level.SEVERE,"StarForge operation log failed: "+text,e);}});}
    public void close(){io.shutdown();}
}
