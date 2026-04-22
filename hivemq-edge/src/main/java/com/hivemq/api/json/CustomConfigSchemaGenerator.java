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
package com.hivemq.api.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.annotations.MutuallyExclusiveFields;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomConfigSchemaGenerator {

    public static final String ENUM_NAMES_ATTRIBUTE = "enumNames";

    /** Transient marker attribute embedded during schema generation; stripped in the post-process walk. */
    private static final String MEF_MARKER = "x-hivemq-mutually-exclusive";

    public @NotNull JsonNode generateJsonSchema(final @NotNull Class clazz) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                        SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule(
                        JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                        JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
                        JacksonOption.RESPECT_JSONPROPERTY_ORDER))
                .with(new ModuleConfigSchemaGeneratorModule());
        withEnumDisplayNameProvider(configBuilder);
        withMutuallyExclusiveFieldsMarker(configBuilder);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        final JsonNode schema = generator.generateSchema(clazz);
        rewriteMarkedNodes(schema);
        if (schema instanceof ObjectNode root) {
            inlineDefs(root);
        }
        return schema;
    }

    /**
     * Inlines every {@code $ref: "#/$defs/<name>"} reference by substituting the referenced schema and then
     * removes the top-level {@code $defs} block. This avoids a downstream breakage where the frontend wraps
     * the returned schema ({@code hivemq-edge-frontend/src/modules/Device/hooks/useTagManager.ts}) under
     * {@code definitions.TagSchema}, which moves {@code $defs} off the root and invalidates {@code $ref}
     * pointers. Inlining produces a larger but self-contained schema that survives the wrapping.
     */
    private static void inlineDefs(final @NotNull ObjectNode root) {
        final JsonNode defsNode = root.get("$defs");
        if (!(defsNode instanceof ObjectNode defs)) {
            return;
        }
        substituteRefs(root, defs);
        root.remove("$defs");
    }

    private static void substituteRefs(final @NotNull JsonNode node, final @NotNull ObjectNode defs) {
        if (node instanceof ObjectNode obj) {
            final JsonNode ref = obj.get("$ref");
            if (ref != null && ref.isTextual()) {
                final String pointer = ref.asText();
                final String prefix = "#/$defs/";
                if (pointer.startsWith(prefix)) {
                    final String name = pointer.substring(prefix.length());
                    final JsonNode target = defs.get(name);
                    if (target instanceof ObjectNode targetObj) {
                        obj.remove("$ref");
                        final Iterator<java.util.Map.Entry<String, JsonNode>> it = targetObj.fields();
                        while (it.hasNext()) {
                            final java.util.Map.Entry<String, JsonNode> e = it.next();
                            if (!obj.has(e.getKey())) {
                                obj.set(e.getKey(), e.getValue().deepCopy());
                            }
                        }
                    }
                }
            }
            final List<String> names = new ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (final String name : names) {
                substituteRefs(obj.get(name), defs);
            }
        } else if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                substituteRefs(arr.get(i), defs);
            }
        }
    }

    /**
     * There is no default way to provide "displayNames" for enum types, the actual way to do it is provide a secondary array
     * where the indices match the value indices with the names. This can be provided by using the field configuration object
     * "enumDisplayNames {}"
     */
    /**
     * Injects a transient marker attribute onto every type-level schema whose class carries
     * {@link MutuallyExclusiveFields}. The marker is consumed (and stripped) later by
     * {@link #rewriteMarkedNodes(JsonNode)}, which runs after victools has fully inlined nested sub-schemas.
     * Using a marker rather than rewriting in-place is required because {@code withTypeAttributeOverride}
     * fires before nested {@code $ref}s and inlined properties are finalised.
     */
    private static void withMutuallyExclusiveFieldsMarker(final @NotNull SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder.forTypesInGeneral().withTypeAttributeOverride(
                (final ObjectNode collectedTypeAttributes,
                        final TypeScope scope,
                        final SchemaGenerationContext context) -> {
                    final Class<?> clazz = scope.getType().getErasedType();
                    if (clazz == null) {
                        return;
                    }
                    final MutuallyExclusiveFields mef = clazz.getAnnotation(MutuallyExclusiveFields.class);
                    if (mef == null || mef.value().length == 0) {
                        return;
                    }
                    final ObjectNode marker = collectedTypeAttributes.putObject(MEF_MARKER);
                    final ArrayNode valueArr = marker.putArray("value");
                    for (final String v : mef.value()) {
                        valueArr.add(v);
                    }
                    final ArrayNode titlesArr = marker.putArray("titles");
                    for (final String t : mef.titles()) {
                        titlesArr.add(t);
                    }
                    marker.put("includeDefault", mef.includeDefault());
                    marker.put("defaultTitle", mef.defaultTitle());
                    marker.put("groupTitle", mef.groupTitle());
                });
    }

    /**
     * Walks the generated schema tree and rewrites every object node carrying an {@link #MEF_MARKER}
     * into a {@code oneOf} with one branch per declared member (plus an optional default branch). The
     * rewrite runs after full inlining so each branch inherits the complete nested sub-schema.
     */
    private static void rewriteMarkedNodes(final @NotNull JsonNode root) {
        if (root instanceof ObjectNode obj) {
            final JsonNode marker = obj.get(MEF_MARKER);
            if (marker instanceof ObjectNode mefMarker) {
                applyMutuallyExclusiveMarker(obj, mefMarker);
            }
            final List<String> fieldNames = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (final String name : fieldNames) {
                if (!MEF_MARKER.equals(name)) {
                    rewriteMarkedNodes(obj.get(name));
                }
            }
        } else if (root instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                rewriteMarkedNodes(arr.get(i));
            }
        }
    }

    private static void applyMutuallyExclusiveMarker(
            final @NotNull ObjectNode schema, final @NotNull ObjectNode marker) {
        final List<String> names = new ArrayList<>();
        for (final JsonNode n : marker.withArray("value")) {
            names.add(n.asText());
        }
        final List<String> titles = new ArrayList<>();
        for (final JsonNode n : marker.withArray("titles")) {
            titles.add(n.asText());
        }
        final boolean includeDefault = marker.path("includeDefault").asBoolean(false);
        final String defaultTitle = marker.path("defaultTitle").asText("Default");
        final String groupTitle = marker.path("groupTitle").asText("");

        schema.remove(MEF_MARKER);

        final JsonNode propsNode = schema.get("properties");
        if (!(propsNode instanceof ObjectNode properties)) {
            return;
        }

        final ArrayNode oneOf = schema.withArray("oneOf");
        if (!groupTitle.isBlank()) {
            schema.put("title", groupTitle);
        }

        if (includeDefault) {
            final ObjectNode def = oneOf.addObject();
            def.put("title", defaultTitle);
            def.put("type", "object");
            def.putObject("properties");
            // The default branch has no member properties. additionalProperties: false keeps it
            // mutually exclusive with the member branches so RJSF/AJV can disambiguate.
            def.put("additionalProperties", false);
        }

        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            final JsonNode sub = properties.get(name);
            if (sub == null) {
                continue;
            }
            final ObjectNode branch = oneOf.addObject();
            branch.put("title", (i < titles.size() && !titles.get(i).isBlank()) ? titles.get(i) : name);
            branch.put("type", "object");
            branch.with("properties").set(name, sub.deepCopy());
            branch.withArray("required").add(name);
            branch.put("additionalProperties", false);
        }

        // Set a top-level default so new/empty instances match the default branch deterministically
        // — prevents RJSF from merging per-branch defaults into a hybrid shape that matches no branch.
        if (includeDefault && !schema.has("default")) {
            schema.putObject("default");
        }

        for (final String name : names) {
            properties.remove(name);
        }
        if (!properties.fieldNames().hasNext()) {
            schema.remove("properties");
        }

        final JsonNode requiredNode = schema.get("required");
        if (requiredNode instanceof ArrayNode topRequired) {
            for (int i = topRequired.size() - 1; i >= 0; i--) {
                for (final String name : names) {
                    if (name.equals(topRequired.get(i).asText())) {
                        topRequired.remove(i);
                        break;
                    }
                }
            }
            if (topRequired.isEmpty()) {
                schema.remove("required");
            }
        }
    }

    private static void withEnumDisplayNameProvider(final @NotNull SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder.forFields().withInstanceAttributeOverride((collectedMemberAttributes, member, context) -> {
            ModuleConfigField configField = member.getAnnotation(ModuleConfigField.class);
            if (configField != null) {
                String[] displayValues = configField.enumDisplayValues();
                if (displayValues != null && displayValues.length > 0) {
                    ArrayNode node = (ArrayNode) collectedMemberAttributes.get(ENUM_NAMES_ATTRIBUTE);
                    if (node == null) {
                        node = collectedMemberAttributes.putArray(ENUM_NAMES_ATTRIBUTE);
                    }
                    Arrays.stream(displayValues).forEach(node::add);
                }
            }
        });
    }

    private static class ModuleConfigSchemaGeneratorModule implements Module {

        @Override
        public void applyToConfigBuilder(final @NotNull SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder) {
            applyToConfigPart(schemaGeneratorConfigBuilder.forFields());
            applyToConfigPart(schemaGeneratorConfigBuilder.forMethods());
        }

        private void applyToConfigPart(final @NotNull SchemaGeneratorConfigPart<?> schemaGeneratorConfigPart) {
            schemaGeneratorConfigPart
                    .withTitleResolver(this::title)
                    .withArrayMinItemsResolver(this::arrayMinItems)
                    .withArrayMaxItemsResolver(this::arrayMaxItems)
                    .withArrayUniqueItemsResolver(this::arrayUniqueItems)
                    .withDescriptionResolver(this::description)
                    .withStringFormatResolver(this::stringFormat)
                    .withDefaultResolver(this::defaultValue)
                    .withRequiredCheck(this::required)
                    .withReadOnlyCheck(this::readOnly)
                    .withWriteOnlyCheck(this::writeOnly)
                    .withNumberInclusiveMinimumResolver(this::numberInclusiveMin)
                    .withNumberInclusiveMaximumResolver(this::numberInclusiveMax)
                    .withStringMinLengthResolver(this::stringMinLength)
                    .withStringMaxLengthResolver(this::stringMaxLength)
                    .withStringPatternResolver(this::stringPattern)
                    .withIgnoreCheck(this::ignored)
                    .withInstanceAttributeOverride(this::customAttributes);
        }

        private void customAttributes(
                final @NotNull ObjectNode jsonNodes,
                final @NotNull MemberScope<?, ?> memberScope,
                final @NotNull SchemaGenerationContext schemaGenerationContext) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            if (fieldInfo == null || fieldInfo.customAttributes() == null || fieldInfo.customAttributes().length < 1) {
                return;
            }

            for (ModuleConfigField.CustomAttribute customAttribute : fieldInfo.customAttributes()) {
                jsonNodes.put(customAttribute.name(), customAttribute.value());
            }
        }

        private boolean writeOnly(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.writeOnly();
        }

        private boolean readOnly(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.readOnly();
        }

        private @Nullable Boolean arrayUniqueItems(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return (fieldInfo != null && fieldInfo.arrayUniqueItems()) ? true : null;
        }

        private @Nullable Integer arrayMinItems(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.arrayMinItems() > 0 ? fieldInfo.arrayMinItems() : null;
        }

        private @Nullable Integer arrayMaxItems(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.arrayMaxItems() > 0 && fieldInfo.arrayMaxItems() < Integer.MAX_VALUE
                    ? fieldInfo.arrayMaxItems()
                    : null;
        }

        private @Nullable String title(final @NotNull MemberScope<?, ?> memberScope) {
            // -- We should not allow duplication of titles for wrapped types
            if (memberScope.isFakeContainerItemScope()) return null;
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null
                            && fieldInfo.title() != null
                            && !fieldInfo.title().isBlank()
                    ? fieldInfo.title()
                    : null;
        }

        private @Nullable String description(final @NotNull MemberScope<?, ?> memberScope) {
            // -- We should not allow duplication of descriptions for wrapped types
            if (memberScope.isFakeContainerItemScope()) return null;
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null
                            && fieldInfo.description() != null
                            && !fieldInfo.description().isBlank()
                    ? fieldInfo.description()
                    : null;
        }

        private @Nullable String stringFormat(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null
                            && fieldInfo.format() != null
                            && fieldInfo.format() != ModuleConfigField.FieldType.UNSPECIFIED
                    ? fieldInfo.format().getName()
                    : null;
        }

        private @Nullable Object defaultValue(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            String str = fieldInfo != null
                            && fieldInfo.defaultValue() != null
                            && !fieldInfo.defaultValue().isEmpty()
                    ? fieldInfo.defaultValue()
                    : null;
            return getNativeObject(str);
        }

        private boolean required(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.required();
        }

        private boolean ignored(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.ignore();
        }

        private @Nullable BigDecimal numberInclusiveMin(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.numberMin() != Double.MIN_VALUE
                    ? BigDecimal.valueOf(fieldInfo.numberMin())
                    : null;
        }

        private @Nullable BigDecimal numberInclusiveMax(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.numberMax() < Double.MAX_VALUE
                    ? BigDecimal.valueOf(fieldInfo.numberMax())
                    : null;
        }

        private @Nullable Integer stringMinLength(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null && fieldInfo.stringMinLength() > 0 ? fieldInfo.stringMinLength() : null;
        }

        private @Nullable Integer stringMaxLength(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null
                            && fieldInfo.stringMaxLength() > 0
                            && fieldInfo.stringMaxLength() < Integer.MAX_VALUE
                    ? fieldInfo.stringMaxLength()
                    : null;
        }

        protected @Nullable String stringPattern(final @NotNull MemberScope<?, ?> memberScope) {
            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
            return fieldInfo != null
                            && fieldInfo.stringPattern() != null
                            && !fieldInfo.stringPattern().isBlank()
                    ? fieldInfo.stringPattern()
                    : null;
        }

        // -- Removed support until bug resolved in 3rd party lib
        //        protected List<?> resolveAllowableValues(final @NotNull MemberScope<?, ?> memberScope) {
        //            final ModuleConfigField fieldInfo = getModuleFieldInfo(memberScope);
        //            if (fieldInfo != null &&
        //                    fieldInfo.allowableValues() != null &&
        //                    fieldInfo.allowableValues().length > 0) {
        //                try {
        //                    return Arrays.stream(fieldInfo.allowableValues()).
        //                        map(BigInteger::new).collect(
        //                        Collectors.toList());
        //                } catch(NumberFormatException e){
        //                    return Arrays.asList(fieldInfo.allowableValues());
        //                }
        //            }
        //            return null;
        //        }

        protected @Nullable ModuleConfigField getModuleFieldInfo(MemberScope<?, ?> member) {
            ModuleConfigField annotation = member.getAnnotation(ModuleConfigField.class);
            if (annotation == null) {
                final MemberScope<?, ?> source;
                if (member instanceof FieldScope fieldScope) {
                    source = fieldScope.findGetter();
                } else if (member instanceof MethodScope methodScope) {
                    source = methodScope.findGetterField();
                } else {
                    source = null;
                }
                annotation = source == null ? null : source.getAnnotation(ModuleConfigField.class);
            }
            return annotation;
        }
    }

    @SuppressWarnings("EmptyCatch")
    private static @Nullable Object getNativeObject(final @Nullable String format) {
        if (format != null) {
            if ("true".equalsIgnoreCase(format.trim()) || "false".equalsIgnoreCase(format.trim())) {
                return Boolean.parseBoolean(format.trim());
            }
            try {
                return Long.parseLong(format);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Double.parseDouble(format);
            } catch (NumberFormatException ignored) {
            }
        }
        return format;
    }
}
