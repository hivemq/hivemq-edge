/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.api.auth.provider.impl.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.http.core.UsernamePasswordRoles;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class SimpleUsernameRolesProviderImplTest {

    public static final Set<@NotNull String> TEST_ROLES = Set.of("TEST_ROLE");
    public static final String USER_NAME = "user1";

    @Test
    public void testUsingCorrectPasswordSucceeds() {
        final var simpleProvider = new SimpleUsernameRolesProviderImpl();
        simpleProvider.add(new UsernamePasswordRoles(USER_NAME, "pass1".getBytes(UTF_8), TEST_ROLES));
        assertThat(simpleProvider.findByUsernameAndPassword(USER_NAME, "pass1".getBytes(UTF_8)))
                .isNotEmpty()
                .get()
                .satisfies(usernamePasswordRoles -> {
                    assertThat(usernamePasswordRoles.roles()).containsExactly("TEST_ROLE");
                    assertThat(usernamePasswordRoles.username()).isEqualTo(USER_NAME);
                    assertThat(usernamePasswordRoles.toPrincipal()).isEqualTo(new ApiPrincipal(USER_NAME, TEST_ROLES));
                });
    }

    @Test
    public void testUsingIncorrectPasswordFails() {
        final var simpleProvider = new SimpleUsernameRolesProviderImpl();
        simpleProvider.add(new UsernamePasswordRoles(USER_NAME, "pass1".getBytes(UTF_8), TEST_ROLES));
        assertThat(simpleProvider.findByUsernameAndPassword(USER_NAME, "incorrect".getBytes(UTF_8)))
                .isEmpty();
    }
}
