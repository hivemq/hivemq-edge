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
package com.hivemq.adapter;

import com.hivemq.api.model.components.Link;
import com.hivemq.api.model.components.Module;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Simon L Johnson
 */
public class ModuleModelTests {

    static String MODULE_ID = "moduleId";
    static String MODULE_VERSION = "moduleVersion";
    static String MODULE_NAME = "moduleName";
    static String MODULE_DESCRIPTION = "moduleDescription";
    static String MODULE_AUTHOR = "moduleAuthor";
    static Integer MODULE_PRIORITY = 3;
    static String MODULE_TYPE = "moduleType";

    static String LINK_DISPLAY = "testDisplay";
    static String LINK_DESC = "description";
    static String LINK_TARGET = "_blank";
    static String LINK_IMAGE_URL = "imageUrl";
    static String LINK_URL = "url";


    @Test
    void testModuleModel() {
        Module module = createTestModule();
        assertEquals(MODULE_ID, module.getId(), "Module Id should match");
        assertEquals(MODULE_VERSION, module.getVersion(), "Module version should match");
        assertEquals(MODULE_NAME, module.getName(), "Module name should match");
        assertEquals(MODULE_DESCRIPTION, module.getDescription(), "Module desc should match");
        assertEquals(MODULE_AUTHOR, module.getAuthor(), "Module author should match");
        assertEquals(MODULE_PRIORITY, module.getPriority(), "Module priority should match");
        assertEquals(MODULE_TYPE, module.getModuleType(), "Module type should match");

        assertEquals(createTestLink("logoUrl"), module.getLogoUrl(), "Logo url should match");
        assertEquals(createTestLink("documentationLink"), module.getDocumentationLink(), "documentation url should match");
        assertEquals(createTestLink("provisioningLink"), module.getProvisioningLink(), "Provisioning url should match");

    }

    @Test
    void testLinkModel() {
        Link link = createTestLink();
        assertEquals(LINK_DISPLAY, link.getDisplayText(), "Link display should match");
        assertEquals(LINK_DESC, link.getDescription(), "Link desc should match");
        assertEquals(LINK_URL, link.getUrl(), "Link url should match");
        assertEquals(LINK_TARGET, link.getTarget(), "Link target should match");
        assertEquals(LINK_IMAGE_URL, link.getImageUrl(), "Link image url should match");
    }

    protected static @NotNull Link createTestLink(){
        return createTestLink(LINK_DISPLAY);
    }

    protected static @NotNull Link createTestLink(String name){
        return createLink(name, LINK_URL, LINK_DESC, LINK_TARGET, LINK_IMAGE_URL, false);
    }

    protected static @NotNull Module createTestModule(){
        return createModule(MODULE_ID, MODULE_VERSION, MODULE_NAME, createTestLink("logoUrl"),
                MODULE_DESCRIPTION,
                MODULE_AUTHOR,
                MODULE_PRIORITY, true,
                MODULE_TYPE,
                createTestLink("documentationLink"),
                createTestLink("provisioningLink"));
    }

    protected static Link createLink(final @NotNull String displayText, final @NotNull String url,
                                     final @NotNull String description, final @NotNull String target,
                                     final @NotNull String imageUrl, final boolean external){
        Link link = new Link(displayText, url, description, target, imageUrl, external);
        return link;
    }

    protected static @NotNull Module createModule(@NotNull final String id,
                                          @NotNull final String version,
                                          @NotNull final String name,
                                          @Nullable final Link logoUrl,
                                          @Nullable final String description,
                                          @NotNull final String author,
                                          @NotNull final Integer priority,
                                          @NotNull final Boolean installed,
                                          @Nullable final String moduleType,
                                          @Nullable final Link documentationLink,
                                          @Nullable final Link provisioningLink){
        Module module = new Module(id, version, name, logoUrl, description, author, priority, installed, moduleType, documentationLink, provisioningLink);
        return module;
    }
}
