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


/**
 * WARNING - this mapper will attempt to create assume wrapper elements where there are lists of Maps and the attribute name
 * ends with an 's'. NB: This only happens at level > 1 (the first level objects will not be considered).
 *
 * For example; An entry like:
 *
 * object -> subscriptions[] -> Map{attributes}
 *
 * will yield
 * <object>
 *     <subscriptions>
 *         <subscription>
 *             <!-- attributes -->
 *         </subscription>
 *         <subscription>
 *  *             <!-- attributes -->
 *  *         </subscription>
 *     </subscriptions>
 * </object>
 *
 */
public class ArbitraryValuesMapAdapter extends XmlAdapter<ArbitraryValuesMapAdapter.ElementMap, Map<String, Object>> {

    public static class ElementMap {
        @XmlAnyElement
        public @NotNull List<Element> elements = new ArrayList<>();

    }

    @Override
    public ElementMap marshal(final @NotNull Map<String, Object> v) {
       return marshalInternal(v, null);
    }

    public ElementMap marshalInternal(final @NotNull Map<String, Object> v, final String parentName) {
        List elements = new ArrayList();
        for (Map.Entry<String, Object> property : v.entrySet()) {
            String key = property.getKey();
            readChildren(key, property.getValue(), elements, parentName);
        }
        ElementMap el = new ElementMap();
        el.elements = elements;
        return el;
    }

    protected void readChildren(final @NotNull String currentKeyName, final @NotNull Object value, final @NotNull List<JAXBElement> currentEl, final String parentName){

//        System.err.println("---> currentKeyName="+ currentKeyName + ", object=" + value + ", parentName=" + parentName);

        if (value instanceof Map) {
            currentEl.add(new JAXBElement<>(new QName(currentKeyName), ElementMap.class, marshalInternal((Map) value, currentKeyName)));
        }
        else if (value instanceof List) {
            List list = (List) value;
            if(parentName != null && currentKeyName.endsWith("s")){
                List children = new ArrayList();
                readChildren(shortName(currentKeyName), list, children, currentKeyName);
                ElementMap elementMap = new ElementMap();
                elementMap.elements = children;
                if(!children.isEmpty()){
                    // add the plural
                    currentEl.add(new JAXBElement<>(new QName(currentKeyName), ElementMap.class, elementMap));
                }
            }
            else {
                for(Object listElement : list) {
                    //-- Recurse point
                    readChildren(currentKeyName, listElement, currentEl, currentKeyName);
                }
            }
        }
        else {
            currentEl.add(new JAXBElement<>(new QName(currentKeyName), String.class, value.toString()));
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
