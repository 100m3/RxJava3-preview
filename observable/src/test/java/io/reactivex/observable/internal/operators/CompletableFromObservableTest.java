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

import io.reactivex.Completable;
import io.reactivex.Observable;
import org.junit.Test;

public class CompletableFromObservableTest {
    @Test(expected = NullPointerException.class)
    public void fromObservableNull() {
        Completable.fromObservable(null);
    }

    @Test
    public void fromObservable() {
        Completable.fromObservable(Observable.just(1))
            .test()
            .assertResult();
    }

    @Test
    public void fromObservableEmpty() {
        Completable.fromObservable(Observable.empty())
            .test()
            .assertResult();
    }

    @Test
    public void fromObservableError() {
        Completable.fromObservable(Observable.error(new UnsupportedOperationException()))
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }
}
