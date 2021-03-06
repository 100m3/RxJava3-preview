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

import org.junit.Test;

import io.reactivex.common.functions.Function;
import io.reactivex.observable.*;
import io.reactivex.observable.extensions.HasUpstreamMaybeSource;
import io.reactivex.observable.subjects.PublishSubject;

public class MaybeToSingleTest {

    @Test
    public void source() {
        Maybe<Integer> m = Maybe.just(1);
        Single<Integer> s = m.toSingle();

        assertTrue(s.getClass().toString(), s instanceof HasUpstreamMaybeSource);

        assertSame(m, (((HasUpstreamMaybeSource<?>)s).source()));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().singleElement().toSingle());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function<Maybe<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Maybe<Object> m) throws Exception {
                return m.toSingle();
            }
        });
    }
}
