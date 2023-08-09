package com.hivemq.tools;

import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

public class PropertyReplacer {

    private static void parsePropertyString(String value, Vector fragments, Vector propertyRefs) throws IllegalStateException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }
            if (pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                if (value.charAt(pos + 1) == '$') {
                    fragments.addElement("$");
                    prev = pos + 2;
                } else {
                    fragments.addElement(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new IllegalStateException("Syntax error in property: "
                            + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }
        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

    public static String replaceProperties(String value, Map keys) {

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();

        while (i.hasMoreElements()) {
            String fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();
                Object replacement = null;
                if (keys != null) {
                    replacement = keys.get(propertyName);
                }
                fragment = (replacement != null)
                        ? replacement.toString()
                        : "${" + propertyName + "}";
            }
            sb.append(fragment);
        }
        return sb.toString();
    }
}
