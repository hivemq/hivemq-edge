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
package com.hivemq.configuration.reader;

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArbitraryValuesMapAdapter extends XmlAdapter<ArbitraryValuesMapAdapter.ElementMap, Map<String, Object>> {

    public static class ElementMap {
        @XmlAnyElement
        public @NotNull List<Element> elements = new ArrayList<Element>();

    }

    @Override
    public ElementMap marshal(final @NotNull Map<String, Object> v) {
        List elements = new ArrayList();
        for (Map.Entry<String, Object> property : v.entrySet()) {
            String key = property.getKey();
            readChildren(key, property.getValue(), elements);
        }
        ElementMap el = new ElementMap();
        el.elements = elements;
        return el;
    }


    protected void readChildren(final @NotNull String key, final @NotNull Object value, final @NotNull List elements){
        if (value instanceof Map) {
            elements.add(new JAXBElement<>(new QName(key), ElementMap.class, marshal((Map) value)));
        }
        else if (value instanceof List) {
            List list = (List) value;
            if(key.endsWith("s")){
                //-- create the children onto a shortened key
                List children = new ArrayList();
                readChildren(shortName(key), list, children);
                ElementMap elementMap = new ElementMap();
                elementMap.elements = children;
                // add the plural
                elements.add(new JAXBElement<>(new QName(key), ElementMap.class, elementMap));
            }
            else {
                for(Object listElement : list) {
                    //-- Recurse point
                    readChildren(key, listElement, elements);
                }
            }
        }
        else {
            elements.add(new JAXBElement<>(new QName(key), String.class, value.toString()));
        }
    }

    public static final String shortName(final @NotNull String name){
        return name.substring(0, name.length() - 1);
    }

    @Override
    public @NotNull Map<String, Object> unmarshal(final @NotNull ElementMap elementMap) throws Exception {
        HashMap<String, Object> map = new HashMap<>();
        for (Element element : elementMap.elements) {
            convertElement(map, element, null);
        }
        return map;
    }

    private static void convertElement(
            final @NotNull HashMap<String, Object> map, Node node, final @Nullable String parentName) {
        if (node.hasChildNodes() && node.getChildNodes().item(0).hasChildNodes()) {
            final NodeList childNodes = node.getChildNodes();
            final HashMap<String, Object> childMap = new HashMap<>();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node item = childNodes.item(i);
                convertElement(childMap, item, node.getLocalName());
            }
            if (childMap.values().size() == 1 &&
                    childMap.keySet().stream().findFirst().get().equals(node.getLocalName())) {
                //map only has 1 entry and the same name as the parent => collapse
                createValueOrAddtoList(map, node, childMap.values().stream().findFirst().get(), parentName);
            } else {
                createValueOrAddtoList(map, node, childMap, parentName);
            }
        } else if (node.getLocalName() != null) {
            createValueOrAddtoList(map, node, node.getTextContent(), parentName);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void createValueOrAddtoList(
            final @NotNull HashMap<String, Object> map,
            final @NotNull Node node,
            final @NotNull Object value,
            final @Nullable String parentName) {

        //if child name is plural of parent name, we expect the value to be a list
        if (parentName != null && parentName.equals(node.getLocalName() + "s")) {
            //check for single key maps
            if ((value instanceof Map) && ((Map) value).keySet().size() == 1) {
                replaceWithList(map, ((Map<?, ?>) value).values().stream().findFirst().get(), parentName);
                return;
            }
            replaceWithList(map, value, parentName);
            return;
        }

        if (!map.containsKey(node.getLocalName())) {
            map.put(node.getLocalName(), value);
            return;
        }

        replaceWithList(map, value, node.getLocalName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void replaceWithList(
            final @NotNull HashMap<String, Object> map, final @NotNull Object value, final @NotNull String key) {
        //key already present => create list of values instead
        final Object prevValue = map.get(key);
        if (prevValue == null) {
            map.put(key, Lists.newArrayList(value));
        } else if (prevValue instanceof List) {
            ((List) prevValue).add(value);
        } else {
            map.replace(key, Lists.newArrayList(prevValue, value));
        }
    }

}
