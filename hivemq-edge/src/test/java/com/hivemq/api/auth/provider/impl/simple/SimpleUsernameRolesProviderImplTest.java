package com.hivemq.api.auth.provider.impl.simple;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.http.core.UsernamePasswordRoles;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleUsernameRolesProviderImplTest {

    public static final Set<@NotNull String> TEST_ROLES = Set.of("TEST_ROLE");
    public static final String USER_NAME = "user1";

    @Test
    public void testUsingCorrectPasswordSucceeds() {
        final var simpleProvider = new SimpleUsernameRolesProviderImpl();
        simpleProvider.add(new UsernamePasswordRoles(USER_NAME, "pass1".getBytes(), TEST_ROLES));
        assertThat(simpleProvider.findByUsernameAndPassword(USER_NAME, "pass1".getBytes()))
                .isNotEmpty()
                .get()
                .satisfies(usernamePasswordRoles -> {
                   assertThat(usernamePasswordRoles.roles())
                            .containsExactly("TEST_ROLE");
                   assertThat(usernamePasswordRoles.username())
                           .isEqualTo(USER_NAME);
                   assertThat(usernamePasswordRoles.toPrincipal())
                           .isEqualTo(new ApiPrincipal(USER_NAME, TEST_ROLES));
                });
    }

    @Test
    public void testUsingIncorrectPasswordFails() {
        final var simpleProvider = new SimpleUsernameRolesProviderImpl();
        simpleProvider.add(new UsernamePasswordRoles(USER_NAME, "pass1".getBytes(), TEST_ROLES));
        assertThat(simpleProvider.findByUsernameAndPassword(USER_NAME, "incorrect".getBytes()))
                .isEmpty();
    }
}
