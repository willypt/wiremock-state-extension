/*
 * Copyright (C) 2023 Dirk Bolte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.extensions.state;

/**
 * Factory to register all extensions for handling state for standalone service.
 * <p>
 * Uses {@link org.wiremock.extensions.state.CaffeineStore} as store or
 * Uses {@link org.wiremock.extensions.state.SqliteStore} as store.
 *
 * @see CaffeineStore
 * @see SqliteStore
 */
public class StandaloneStateExtension extends StateExtension {

    public StandaloneStateExtension() {
//        super(new CaffeineStore());
        super(new SqliteStore());
    }
}
