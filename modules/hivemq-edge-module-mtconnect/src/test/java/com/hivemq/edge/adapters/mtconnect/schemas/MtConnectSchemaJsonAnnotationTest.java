/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MtConnectSchemaJsonAnnotationTest {
    private static final @NotNull File DEVICES_FILE =
            new File("src/main/java/com/hivemq/edge/adapters/mtconnect/schemas/devices/");

    protected @NotNull List<File> getFiles(final @NotNull File rootFile) {
        return Arrays.stream(Objects.requireNonNull(rootFile.listFiles()))
                .filter(File::isDirectory)
                .flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles(((dir, name) -> name.endsWith(".java"))))))
                .collect(Collectors.toList());
    }

    protected void patchFile(AnnotationContext context) throws IOException {
        if (context.getAnnotationTreeMap().isEmpty()) {
            return;
        }
        final Path filePath = Path.of(context.getCompilationUnitTree().getSourceFile().getName());
        final String toBePatchedContent = Files.readString(filePath, StandardCharsets.UTF_8);
        assertThat(toBePatchedContent).isNotEmpty();
        final StringBuilder sb = new StringBuilder(toBePatchedContent.length() << 1);
        int position = 0;
        for (AnnotationTree annotationTree : context.getAnnotationTreeMap().values()) {
            assertThat(annotationTree.name).doesNotContain("\"");
            final int lineStartPosition = toBePatchedContent.lastIndexOf("\n", (int) annotationTree.startPosition) + 1;
            final int lineEndPosition = toBePatchedContent.indexOf("\n", (int) annotationTree.endPosition) + 1;
            assertThat(lineStartPosition).isGreaterThanOrEqualTo(position);
            assertThat(lineEndPosition).isGreaterThan(lineStartPosition);
            if (lineStartPosition > position) {
                sb.append(toBePatchedContent, position, lineStartPosition);
            }
            @Nullable String annotationName = null;
            switch (annotationTree.type) {
                case XmlProperty -> {
                    annotationName = JsonProperty.class.getName();
                }
                case XmlType -> {
                    annotationName = JsonTypeName.class.getName();
                }
                default -> {
                    // Skip
                }
            }
            if (annotationName != null) {
                sb.append(" ".repeat((int) annotationTree.startPosition - lineStartPosition));
                sb.append("@").append(annotationName).append("(value = \"").append(annotationTree.name).append("\")\n");
                sb.append(toBePatchedContent, lineStartPosition, lineEndPosition);
            }
            position = lineEndPosition;
        }
        if (position < toBePatchedContent.length()) {
            sb.append(toBePatchedContent, position, toBePatchedContent.length());
        }
        final String patchedContent = sb.toString();
        assertThat(patchedContent).isNotEmpty();
        if (!toBePatchedContent.equals(patchedContent)) {
            Files.writeString(filePath, patchedContent, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void scanAndPatchFiles() throws IOException {
        final List<File> files = getFiles(DEVICES_FILE);
        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final StandardJavaFileManager javaFileManager =
                javaCompiler.getStandardFileManager(diagnosticCollector, null, null);
        final Iterable<? extends JavaFileObject> javaFileObjects = javaFileManager.getJavaFileObjectsFromFiles(files);
        final JavacTask task = (JavacTask) javaCompiler.getTask(null,
                javaFileManager,
                diagnosticCollector,
                null,
                null,
                javaFileObjects);
        final Trees trees = Trees.instance(task);
        final AnnotationScanner annotationScanner = new AnnotationScanner();
        int processedFileCount = 0;
        for (final CompilationUnitTree compilationUnitTree : task.parse()) {
            processedFileCount++;
            final AnnotationContext context = new AnnotationContext(compilationUnitTree, trees.getSourcePositions());
            annotationScanner.scan(compilationUnitTree, context);
            patchFile(context);
        }
        assertThat(processedFileCount).isEqualTo(files.size());
    }

    protected enum TreeType {
        JsonType,
        JsonProperty,
        XmlType,
        XmlProperty,
    }

    protected static class AnnotationContext {
        private final @NotNull CompilationUnitTree compilationUnitTree;
        private final @NotNull SourcePositions sourcePositions;
        private final @NotNull Map<Long, AnnotationTree> annotationTreeMap;

        public AnnotationContext(
                @NotNull final CompilationUnitTree compilationUnitTree,
                @NotNull final SourcePositions sourcePositions) {
            this.compilationUnitTree = compilationUnitTree;
            this.sourcePositions = sourcePositions;
            annotationTreeMap = new TreeMap<>();
        }

        public @NotNull CompilationUnitTree getCompilationUnitTree() {
            return compilationUnitTree;
        }

        public long getEndPosition(final @NotNull Tree tree) {
            return sourcePositions.getEndPosition(compilationUnitTree, tree);
        }

        public long getStartPosition(final @NotNull Tree tree) {
            return sourcePositions.getStartPosition(compilationUnitTree, tree);
        }

        public @NotNull Map<Long, AnnotationTree> getAnnotationTreeMap() {
            return annotationTreeMap;
        }
    }

    protected static class AnnotationScanner extends TreePathScanner<AnnotationScanner, AnnotationContext> {
        private static final @NotNull String NAME = "name";
        private static final @NotNull String VALUE = "value";
        private static final @NotNull Map<String, TreeType> XML_TAG_NAME_MAP = Map.of(XmlType.class.getName(),
                TreeType.XmlType,
                XmlType.class.getSimpleName(),
                TreeType.XmlType,
                XmlElement.class.getName(),
                TreeType.XmlProperty,
                XmlElement.class.getSimpleName(),
                TreeType.XmlProperty,
                XmlAttribute.class.getName(),
                TreeType.XmlProperty,
                XmlAttribute.class.getSimpleName(),
                TreeType.XmlProperty);
        private static final @NotNull Map<String, TreeType> JSON_TAG_NAME_MAP = Map.of(JsonTypeName.class.getName(),
                TreeType.JsonType,
                JsonTypeName.class.getSimpleName(),
                TreeType.JsonType,
                JsonProperty.class.getName(),
                TreeType.JsonProperty,
                JsonProperty.class.getSimpleName(),
                TreeType.JsonProperty);

        @Override
        public @NotNull MtConnectSchemaJsonAnnotationTest.AnnotationScanner visitAnnotation(
                final @NotNull com.sun.source.tree.AnnotationTree tree,
                final @NotNull MtConnectSchemaJsonAnnotationTest.AnnotationContext context) {
            final @Nullable String annotationName = tree.getAnnotationType().toString();
            if (annotationName != null) {
                AnnotationTree annotationTree = null;
                TreeType xmlTreeType = XML_TAG_NAME_MAP.get(annotationName);
                if (xmlTreeType != null) {
                    assertThat(tree.getArguments()).as(tree + " should not be empty").isNotEmpty();
                    final @Nullable String name = tree.getArguments()
                            .stream()
                            .filter(arg -> arg instanceof AssignmentTree)
                            .map(arg -> (AssignmentTree) arg)
                            .filter(assignmentTree -> {
                                if (assignmentTree.getExpression() instanceof LiteralTree) {
                                    if (assignmentTree.getVariable() instanceof final IdentifierTree innerIdentifierTree) {
                                        return NAME.equals(innerIdentifierTree.getName().toString());
                                    }
                                }
                                return false;
                            })
                            .map(assignmentTree -> (LiteralTree) assignmentTree.getExpression())
                            .map(literalTree -> literalTree.getValue().toString())
                            .findFirst()
                            .orElse(null);
                    assertThat(name).isNotNull();
                    annotationTree = new AnnotationTree(context.getStartPosition(tree),
                            context.getEndPosition(tree),
                            name,
                            xmlTreeType,
                            tree);
                }
                TreeType jsonTreeType = JSON_TAG_NAME_MAP.get(annotationName);
                if (jsonTreeType != null) {
                    final @Nullable String name = tree.getArguments()
                            .stream()
                            .filter(arg -> arg instanceof AssignmentTree)
                            .map(arg -> (AssignmentTree) arg)
                            .filter(assignmentTree -> {
                                if (assignmentTree.getExpression() instanceof LiteralTree) {
                                    if (assignmentTree.getVariable() instanceof final IdentifierTree innerIdentifierTree) {
                                        return VALUE.equals(innerIdentifierTree.getName().toString());
                                    }
                                }
                                return false;
                            })
                            .map(assignmentTree -> (LiteralTree) assignmentTree.getExpression())
                            .map(literalTree -> literalTree.getValue().toString())
                            .findFirst()
                            .orElse(null);
                    annotationTree = new AnnotationTree(context.getStartPosition(tree),
                            context.getEndPosition(tree),
                            name,
                            jsonTreeType,
                            tree);
                }
                if (annotationTree != null) {
                    context.getAnnotationTreeMap().put(annotationTree.startPosition, annotationTree);
                }
            }
            return super.visitAnnotation(tree, context);
        }
    }

    protected record AnnotationTree(long startPosition, long endPosition, String name, TreeType type, Tree tree) {
        @Override
        public boolean equals(final @Nullable Object obj) {
            if (obj instanceof final AnnotationTree that) {
                return startPosition == that.startPosition;
            }
            return false;
        }

        @Override
        public String toString() {
            return "AnnotationTree{" +
                    "startPosition=" +
                    startPosition +
                    ", endPosition=" +
                    endPosition +
                    ", name='" +
                    name +
                    '\'' +
                    ", type=" +
                    type +
                    ", tree=" +
                    tree +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(startPosition);
        }
    }
}
