/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.observable.internal.observers;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.common.*;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.internal.functions.Functions;

public class FutureObserverTest {
    FutureObserver<Integer> fo;

    @Before
    public void before() {
        fo = new FutureObserver<Integer>();
    }

    @Test
    public void cancel2() {

        fo.dispose();

        assertFalse(fo.isCancelled());
        assertFalse(fo.isDisposed());
        assertFalse(fo.isDone());

        for (int i = 0; i < 2; i++) {
            fo.cancel(i == 0);

            assertTrue(fo.isCancelled());
            assertTrue(fo.isDisposed());
            assertTrue(fo.isDone());
        }

        List<Throwable> errors = TestCommonHelper.trackPluginErrors();
        try {
            fo.onNext(1);
            fo.onError(new TestException("First"));
            fo.onError(new TestException("Second"));
            fo.onComplete();

            assertTrue(fo.isCancelled());
            assertTrue(fo.isDisposed());
            assertTrue(fo.isDone());

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
            TestCommonHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void cancel() throws Exception {
        assertFalse(fo.isDone());

        assertFalse(fo.isCancelled());

        fo.cancel(false);

        assertTrue(fo.isDone());

        assertTrue(fo.isCancelled());

        try {
            fo.get();
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }

        try {
            fo.get(1, TimeUnit.MILLISECONDS);
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }
    }

    @Test
    public void onError() throws Exception {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            fo.onError(new TestException("One"));

            fo.onError(new TestException("Two"));

            try {
                fo.get(5, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ex) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
                assertEquals("One", ex.getCause().getMessage());
            }

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onNext() throws Exception {
        fo.onNext(1);
        fo.onComplete();

        assertEquals(1, fo.get(5, TimeUnit.MILLISECONDS).intValue());
    }

    @Test
    public void onSubscribe() throws Exception {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {

            Disposable s = Disposables.empty();

            fo.onSubscribe(s);

            Disposable s2 = Disposables.empty();

            fo.onSubscribe(s2);

            assertFalse(s.isDisposed());
            assertTrue(s2.isDisposed());

            TestCommonHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void cancelRace() {
        for (int i = 0; i < 500; i++) {
            final FutureObserver<Integer> fo = new FutureObserver<Integer>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    fo.cancel(false);
                }
            };

            TestCommonHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void await() throws Exception {
        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                fo.onNext(1);
                fo.onComplete();
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(1, fo.get(5, TimeUnit.SECONDS).intValue());
    }

    @Test
    public void onErrorCancelRace() {
        RxJavaCommonPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < 500; i++) {
                final FutureObserver<Integer> fo = new FutureObserver<Integer>();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        fo.cancel(false);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        fo.onError(ex);
                    }
                };

                TestCommonHelper.race(r1, r2, Schedulers.single());
            }
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }

    @Test
    public void onCompleteCancelRace() {
        for (int i = 0; i < 500; i++) {
            final FutureObserver<Integer> fo = new FutureObserver<Integer>();

            if (i % 3 == 0) {
                fo.onSubscribe(Disposables.empty());
            }

            if (i % 2 == 0) {
                fo.onNext(1);
            }

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    fo.cancel(false);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    fo.onComplete();
                }
            };

            TestCommonHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void onErrorOnComplete() throws Exception {
        fo.onError(new TestException("One"));
        fo.onComplete();

        try {
            fo.get(5, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            assertEquals("One", ex.getCause().getMessage());
        }
    }

    @Test
    public void onCompleteOnError() throws Exception {
        fo.onComplete();
        fo.onError(new TestException("One"));

        try {
            assertNull(fo.get(5, TimeUnit.MILLISECONDS));
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof NoSuchElementException);
        }
    }

    @Test
    public void cancelOnError() throws Exception {
        fo.cancel(true);
        fo.onError(new TestException("One"));

        try {
            fo.get(5, TimeUnit.MILLISECONDS);
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }
    }

    @Test
    public void cancelOnComplete() throws Exception {
        fo.cancel(true);
        fo.onComplete();

        try {
            fo.get(5, TimeUnit.MILLISECONDS);
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }
    }

    @Test
    public void onNextThenOnCompleteTwice() throws Exception {
        fo.onNext(1);
        fo.onComplete();
        fo.onComplete();

        assertEquals(1, fo.get(5, TimeUnit.MILLISECONDS).intValue());
    }

    @Test(expected = InterruptedException.class)
    public void getInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        fo.get();
    }

    @Test
    public void completeAsync() throws Exception {
        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                fo.onNext(1);
                fo.onComplete();
            }
        }, 500, TimeUnit.MILLISECONDS);

        assertEquals(1, fo.get().intValue());
    }
}
