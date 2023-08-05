
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class LastSubmittedItemProcessorTest {

    @Test
    public void orderingIsMaintained() throws InterruptedException {
        EntityDao entityDao = new EntityDao();
        try (LastSubmittedItemProcessor<Entity, String> saver = new LastSubmittedItemProcessor<>(entityDao::save)) {
            int i = 0;
            for (; i < 100; i++) {
                for (int j = 1; j <= 20; j++) {
                    String itemIdentity = String.valueOf(j);
                    saver.processItem(new Entity(itemIdentity, i), itemIdentity);
                }
                Thread.sleep(3);
            }

            saver.processItemBlocking(new Entity("1", i++), "1");
            saver.processItem(new Entity("1", i++), "1");
            saver.processItemBlocking(new Entity("1", i++), "1");
            saver.processItemBlocking(new Entity("1", i++), "1");
            saver.processItemBlocking(new Entity("1", i), "1");

            Set<Map.Entry<Object, List<Object>>> entries = entityDao.getRepository().entrySet();
            for (Map.Entry<Object, List<Object>> entry : entries) {
                int j = -1;
                for (Object o : entry.getValue()) {
                    System.out.printf("%s: %s\n", entry.getKey(), o);
                    int saveCount = (int) o;
                    Assertions.assertTrue(saveCount > j, saveCount + " after " + j + " for Entity " + entry.getKey());
                    j = saveCount;
                }
            }
        }
    }
}
