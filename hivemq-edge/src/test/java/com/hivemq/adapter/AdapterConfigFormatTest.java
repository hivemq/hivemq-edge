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

import com.hivemq.edge.HiveMQEdgeConstants;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Simon L Johnson
 */
public class AdapterConfigFormatTest {

    @Test
    void testIdentifierRegex() {
        Pattern pattern = Pattern.compile(HiveMQEdgeConstants.ID_REGEX);

        //-- Test for leading and trailing whitespace
        Assert.assertFalse("Leading whitespace is invalid for identifiers", pattern.matcher(" invalid-leading-ws").matches());
        Assert.assertFalse("Trailing whitespace is invalid for identifiers", pattern.matcher("invalid-trailing-ws ").matches());

        //-- Test the allowed cases
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this-is-valid").matches());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this_is_valid").matches());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this_is-valid").matches());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this-is_valid").matches());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("-thisisvalid-").matches());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("_thisisvalid_").matches());

        //-- Test the not allowed cases
        testPatternDoesNotAllowSpecialCharsOtherThanSpecified("test_-", pattern, '_','-');
    }

    @Test
    @Disabled
    void testUrlRegex() {

        //No longer used
        Pattern pattern = Pattern.compile("https?:\\/\\/(?:w{1,3}\\.)?[^\\s.]+(?:\\.[a-z]+)*(?::\\d+)?((?:\\/\\w+)|(?:-\\w+))*\\/?(?![^<]*(?:<\\/\\w+>|\\/?>))");

        //-- Test for valid oracle webpage
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("https://docs.oracle.com/javase/tutorial/essential/io/notification").matches());

        //-- Test for valid oracle webpage
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("https://docs.oracle.com/javase/tutorial/essential/io/notification.html").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("http://localhost:8080").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("https://localhost:8080").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("http://127.0.0.1:8080").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("http://127.0.0.1:8080/").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("http://127.0.0.1:8080/foo").matches());

        //-- Test for valid localhost
        Assert.assertTrue("Url should be allowed",
                pattern.matcher("http://127.0.0.1:8080/foo.html").matches());
    }

    @Test
    void testNameRegex() {
        Pattern pattern = Pattern.compile(HiveMQEdgeConstants.NAME_REGEX);

        //-- Test for leading and trailing whitespace
        Assert.assertFalse("Leading whitespace is invalid for name", pattern.matcher(" invalid-leading-ws").matches());
        Assert.assertFalse("Trailing whitespace is invalid for name", pattern.matcher("invalid-trailing-ws ").matches());

        //-- Test the allowed cases
        Assert.assertTrue("Should be valid name", pattern.matcher("A Valid Name").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("this-is-valid").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("this_is_valid").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("this_is-valid").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("this-is_valid").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("-thisisvalid-").matches());
        Assert.assertTrue("Should be valid name", pattern.matcher("_thisisvalid_").matches());

        //-- Test the not allowed cases
        testPatternDoesNotAllowSpecialCharsOtherThanSpecified("test_-", pattern, '_','-');
    }

    void testPatternDoesNotAllowSpecialCharsOtherThanSpecified(final String validPrefix, final Pattern pattern, final char... exceptions) {
        char[] cs = generateSomeNotAllowedChars(exceptions);
        for (int i = 0; i < cs.length; i++){
            String value = validPrefix + cs[i];
            Assert.assertFalse(String.format("pattern should not be allow special char [%s]",
                            String.format("\\u%04x", (int) cs[i])),
                    pattern.matcher(value).find());
        }
    }

    static char[] generateSomeNotAllowedChars(char... allowedChars){
        List<Character> asciiSpecial = new ArrayList<>();
        asciiSpecial.addAll(List.of('+','-','_','@','#','^','*','{','}','[',']','|','\\','/','<','>','?','~','`'));
        for (int i = 0; i < 128; i++){
            char c = (char) i;
            if((i >= 0 && i <= 47) ||
                (i >= 58 && i <= 64) ||
                (i >= 91 && i <= 96) ||
                (i >= 123 && i <= 127)){

                //CR & LF
                if(i == 10 || i == 13) continue;
                if(!asciiSpecial.contains(c)){
                    asciiSpecial.add(c);
                }
            }
        }
        if(allowedChars != null && allowedChars.length > 0){
            //b-search the allow list to remove those the pattern will allow
            Arrays.sort(allowedChars);
            asciiSpecial.removeIf(character -> Arrays.binarySearch(allowedChars, character) > -1);
        }
        Iterator<Character> itr = asciiSpecial.iterator();
        char[] chars = new char[asciiSpecial.size()];
        int i = 0;
        while(itr.hasNext())    chars[i++] = itr.next();
        return chars;
    }
}
