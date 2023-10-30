/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.hivemq.util;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIFO Rolling list, maintains a list with no more than the defined number of elements. When the element count is exceeded,
 * the index is reset to zero, thus writing the "newest" elements to the "oldest" positions in the backing list.
 *
 * NOTE: This is not as thread-safe data structure, concurrent access must be achieved externally.
 */
@NotThreadSafe
public class RollingList<T> extends ArrayList<T> {
    public static final int DEFAULT_CEILING = 25;
    protected final int ceiling;
    protected volatile AtomicInteger internalIdx
            = new AtomicInteger(0);

    public RollingList() {
        super(DEFAULT_CEILING);
        ceiling = DEFAULT_CEILING;
        ensureCapacity(ceiling);
    }

    public RollingList(int max) {
        super(max);
        ceiling = max;
        ensureCapacity(ceiling);
    }

    @Override
    public void add(int i, T o) {
        throw new UnsupportedOperationException("cannot add arbitrary element to list");
    }

    @Override
    public boolean add(T o) {
        synchronized(internalIdx){
            int i = internalIdx.get();
            if(i > (ceiling - 1)){
                internalIdx.set(0);
                i = 0;
            }
            if(size() > i
                    && get(i) != null){
                remove(i);
            }
            super.add(internalIdx.getAndIncrement(), o);
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> col) {
        synchronized (col){
            Iterator<? extends T> itr = col.iterator();
            while(itr.hasNext()){
                add(itr.next());
            }
        }
        return true;
    }

    @Override
    public boolean addAll(int start, Collection<? extends T> col) {
        throw new UnsupportedOperationException("not supported in impl");
    }
}
