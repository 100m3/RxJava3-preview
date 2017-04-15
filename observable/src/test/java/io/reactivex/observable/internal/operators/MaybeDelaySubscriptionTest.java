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

package io.reactivex.observable.internal.operators;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.common.*;
import io.reactivex.common.exceptions.TestException;
import io.reactivex.common.functions.Function;
import io.reactivex.observable.*;
import io.reactivex.observable.observers.TestObserver;
import io.reactivex.observable.subjects.PublishSubject;

public class MaybeDelaySubscriptionTest {

    @Test
    public void normal() {
        PublishSubject<Object> pp = PublishSubject.create();

        TestObserver<Integer> ts = Maybe.just(1).delaySubscription(pp)
        .test();

        assertTrue(pp.hasObservers());

        ts.assertEmpty();

        pp.onNext("one");

        assertFalse(pp.hasObservers());

        ts.assertResult(1);
    }

    @Test
    public void timed() {
        Maybe.just(1).delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void timedEmpty() {
        Maybe.<Integer>empty().delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void timedTestScheduler() {
        TestScheduler scheduler = new TestScheduler();

        TestObserver<Integer> ts = Maybe.just(1)
        .delaySubscription(100, TimeUnit.MILLISECONDS, scheduler)
        .test();

        ts.assertEmpty();

        scheduler.advanceTimeBy(99, TimeUnit.MILLISECONDS);

        ts.assertEmpty();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(1);
    }

    @Test
    public void otherError() {
        Maybe.just(1).delaySubscription(Observable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainError() {
        Maybe.error(new TestException())
        .delaySubscription(Observable.empty())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void withObservableDispose() {
        TestHelper.checkDisposed(Maybe.just(1).delaySubscription(Observable.never()));
    }

    @Test
    public void withObservableDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Maybe<Object> m) throws Exception {
                return m.delaySubscription(Observable.just(1));
            }
        });
    }

    @Test
    public void withObservableCallAfterTerminalEvent() {
        List<Throwable> errors = TestCommonHelper.trackPluginErrors();

        try {
            Observable<Integer> f = new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                    observer.onNext(2);
                }
            };

            Maybe.just(1).delaySubscription(f)
            .test()
            .assertResult(1);

            TestCommonHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaCommonPlugins.reset();
        }
    }
}
