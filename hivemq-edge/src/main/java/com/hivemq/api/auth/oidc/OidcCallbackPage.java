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
package com.hivemq.api.auth.oidc;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the HTML page returned from the OIDC callback.
 * <p>
 * The page is a FreeMarker template rather than a string built in Java, so the markup lives with the
 * other resources and the interpolated values are escaped by the template engine. The values land
 * inside a {@code <script>} block, so the template escapes them with {@code ?js_string}.
 */
final class OidcCallbackPage {

    /** Discriminator the SPA matches on, so it can tell our result from any other same-origin message. */
    static final @NotNull String MESSAGE_TYPE = "oidc-result";

    private static final @NotNull String TEMPLATE_NAME = "oidc-callback-result.ftl";

    private static final @NotNull Configuration CONFIGURATION = createConfiguration();

    private OidcCallbackPage() {}

    private static @NotNull Configuration createConfiguration() {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setClassLoaderForTemplateLoading(OidcCallbackPage.class.getClassLoader(), "templates");
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setLocale(Locale.US);
        // A template failure must not emit a half-rendered page carrying a token; fail loudly instead.
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        return configuration;
    }

    /**
     * Renders the success page, which posts the HiveMQ Edge JWT to the opener and closes the popup.
     */
    static @NotNull String success(final @NotNull String edgeJwt, final @NotNull URI redirectUri) {
        final Map<String, Object> model = baseModel(redirectUri, "Signed in. You may close this window.");
        model.put("token", edgeJwt);
        return render(model);
    }

    /**
     * Renders the failure page, which posts a stable error code to the opener and closes the popup, so
     * the SPA settles rather than waiting. Identity Provider error text is logged, never posted here.
     */
    static @NotNull String failure(final @NotNull OidcErrorCode errorCode, final @NotNull URI redirectUri) {
        final Map<String, Object> model = baseModel(redirectUri, "Login failed. You may close this window.");
        model.put("errorCode", errorCode.getCode());
        return render(model);
    }

    private static @NotNull Map<String, Object> baseModel(
            final @NotNull URI redirectUri, final @NotNull String fallbackText) {
        final Map<String, Object> model = new HashMap<>();
        model.put("messageType", MESSAGE_TYPE);
        model.put("origin", originOf(redirectUri));
        model.put("fallbackText", fallbackText);
        return model;
    }

    private static @NotNull String render(final @NotNull Map<String, Object> model) {
        try (final StringWriter writer = new StringWriter()) {
            final Template template = CONFIGURATION.getTemplate(TEMPLATE_NAME);
            template.process(model, writer);
            return writer.toString();
        } catch (final Exception e) {
            // The template ships with the application, so a failure here is a packaging or code error.
            throw new IllegalStateException("Could not render the OIDC callback page", e);
        }
    }

    /**
     * The {@code postMessage} target origin, derived from the configured redirect URI. The port is
     * omitted when it is the scheme default, because {@code https://host:443} and {@code https://host}
     * are the same origin and {@code postMessage} compares origins as canonical strings.
     */
    private static @NotNull String originOf(final @NotNull URI uri) {
        final String scheme = uri.getScheme();
        final int port = uri.getPort();
        final StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(uri.getHost());
        if (port != -1 && !(("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80))) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
