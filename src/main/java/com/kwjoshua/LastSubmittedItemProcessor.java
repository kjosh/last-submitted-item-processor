/*
 * MIT License
 *
 * Copyright (c) 2023 Joshua Kwiatkowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kwjoshua;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Fuehrt Operationen auf uebergebenen Items asynchron oder synchron durch und stellt dabei folgendes sicher:
 * <ol>
 *     <li>
 *         Fuer jedes Item mit derselben Identitaet wird die Operation nicht mehr als ein mal gleichzeitig ausgefuehrt
 *     </li>
 *     <li>
 *         Es wird, nachdem eine ggf. bereits laufende Operation abgeschlossen ist, nur die <b>zuletzt</b> uebermittelte
 *         Operation fuer dasselbe Item durchgefuehrt.
 *     </li>
 * </ol>
 *
 * @param <T>
 */
public class LastSubmittedItemProcessor<T> implements Closeable {
    private final ExecutorService executorService;
    /**
     * Lock/Count-Objekte eindeutig je identischem Item
     */
    private final Map<Object, AtomicInteger> countingLocks = new HashMap<>();
    private final BiConsumer<T, Object> itemProcessor;

    /**
     * Neues Objekt mit Default-Threadpool (Fixed size = Runtime.getRuntime().availableProcessors()).
     *
     * @see LastSubmittedItemProcessor#LastSubmittedItemProcessor(BiConsumer, ExecutorService)
     */
    public LastSubmittedItemProcessor(BiConsumer<T, Object> itemProcessor) {
        this.itemProcessor = itemProcessor;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * @param itemProcessor   Funktion, die auf den Items durchgefuehrt werden soll.
     *                        Erhaelt das Item und das Identitaetsmerkmal (bspw. ID)
     * @param executorService ExecutorService zum Abarbeiten der itemProcessor-Aufrufe.
     *                        Wird in {@link LastSubmittedItemProcessor#close()} beendet.
     */
    public LastSubmittedItemProcessor(BiConsumer<T, Object> itemProcessor, ExecutorService executorService) {
        this.itemProcessor = itemProcessor;
        this.executorService = executorService;
    }

    public CompletableFuture<Void> processItem(T item, Object itemIdentity) {
        AtomicInteger countingLock;
        int count;
        synchronized (countingLocks) {
            countingLock = countingLocks.computeIfAbsent(itemIdentity, id -> new AtomicInteger(0));
            count = countingLock.incrementAndGet();
        }
        return CompletableFuture.runAsync(() -> {
            synchronized (countingLock) {
                // Wurde waehrend dem Warten auf das Lock noch eine andere Operation fuer dasselbe Item
                // durchgefuehrt, hat sich der countingLock-Zaehler erhoeht und ist nicht mehr identisch zum
                // gespeicherten count, sodass die Operation uebersprungen wird.
                if (countingLock.get() == count) {
                    itemProcessor.accept(item, itemIdentity);
                    synchronized (countingLocks) {
                        if (countingLock.get() == count) {
                            // Es wartet kein anderes identisches Item mehr auf das Lock, das Zaehler/Lock-Objekt
                            // fuer dieses Item kann deshalb entfernt werden
                            countingLocks.remove(itemIdentity);
                        }
                    }
                }
            }
        }, executorService);
    }

    public void processItemBlocking(T item, Object itemIdentity) {
        try {
            processItem(item, itemIdentity).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

}
