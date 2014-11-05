/*
 * Copyright 2014 Click Travel Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.clicktravel.infrastructure.persistence.aws.elasticache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;

import org.junit.Before;
import org.junit.Test;

import com.clicktravel.common.random.Randoms;

public class MemcachedItemCacheTest {

    private final MemcachedClient client = mock(MemcachedClient.class);
    private MemcachedItemCache memcachedCacheStore;

    @Before
    public void before() {
        memcachedCacheStore = new MemcachedItemCache(client);
    }

    @Test
    public void shoudlGetItem() throws InterruptedException, TimeoutException, ExecutionException {

        final String key = Randoms.randomString();
        final String item = Randoms.randomString();
        final int timeout = Randoms.randomInt(5) + 1;

        final GetFuture<Object> f = mock(GetFuture.class);
        when(client.asyncGet(key)).thenReturn(f);
        when(f.get(timeout, TimeUnit.SECONDS)).thenReturn(item);
        final Object obj = memcachedCacheStore.getItem(key, timeout);

        assertEquals(item, obj);

    }

    @Test
    public void shoudlNotGetItem_throwsTimout() throws InterruptedException, ExecutionException, TimeoutException {

        final String key = Randoms.randomString();
        final int timeout = Randoms.randomInt(5) + 1;
        final GetFuture<Object> f = mock(GetFuture.class);
        when(client.asyncGet(key)).thenReturn(f);
        when(f.get(timeout, TimeUnit.SECONDS)).thenThrow(TimeoutException.class);
        final Object item = memcachedCacheStore.getItem(key, timeout);
        assertNull(item);
    }

    @Test
    public void shouldPutItem() {
        final String key = Randoms.randomString();
        final String item = Randoms.randomString();
        final long expire = (long) Randoms.randomInt(5) + 1;
        memcachedCacheStore.putItem(key, item, expire);
        verify(client).set(key, (int) expire, item);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotPutItem_failExpireToLarge() {
        final String key = Randoms.randomString();
        final String item = Randoms.randomString();
        final long expire = (long) Integer.MAX_VALUE + 1;
        memcachedCacheStore.putItem(key, item, expire);
    }
}