/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A named and indexed thread factory, making threads within a thread pool
 * have the same name and a unique integer index
 */
public class IndexedThreadFactory implements ThreadFactory {

    private String name = Thread.currentThread().getName() + "-factory";
    private AtomicInteger threadIndexer = new AtomicInteger(0);

    public IndexedThreadFactory(String name) { this.name = name; }

    public class IndexedThread extends Thread {

        private int threadIndex;
        private String metricName = "default-name-" + Thread.currentThread().getName();

        public IndexedThread(int threadIndex, Runnable r) {
            super(r);
            this.threadIndex = threadIndex;
        }

        public int getThreadIndex() {
            return threadIndex;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public String getMetricName() {
            return metricName;
        }
    }

    @Override
    public IndexedThread newThread(Runnable r) {

        int threadIndex = threadIndexer.incrementAndGet();

        IndexedThread thread = new IndexedThread(threadIndex, r);
        thread.setName(name + ":t" + threadIndex + "/" + thread.getName());
        thread.setMetricName(thread.getName().split(":")[0].split("/")[0]);

        return thread;
    }
}
