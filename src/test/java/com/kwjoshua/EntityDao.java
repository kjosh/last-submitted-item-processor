
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityDao {
    private final Map<Object, List<Object>> repository = new ConcurrentHashMap<>();
    private final Map<Object, Object> dbLock = new HashMap<>();

    public void save(Entity entity, Object entityId) {
        synchronized (dbLock) {
            if (dbLock.containsKey(entityId)) {
                throw new ConcurrentModificationException();
            }
            dbLock.put(entityId, new Object());
        }
        try {
            Thread.sleep(new Random().nextInt(10, 50));
            repository.computeIfAbsent(entityId, id -> new ArrayList<>()).add(entity.data());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (dbLock) {
                dbLock.remove(entityId);
            }
        }
    }

    public Map<Object, List<Object>> getRepository() {
        return repository;
    }
}
