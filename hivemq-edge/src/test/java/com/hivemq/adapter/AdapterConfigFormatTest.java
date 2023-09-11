package com.hivemq.adapter;

import com.hivemq.edge.HiveMQEdgeConstants;
import org.junit.Assert;
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
        Assert.assertFalse("Leading whitespace is invalid for identifiers", pattern.matcher(" invalid-leading-ws").find());
        Assert.assertFalse("Trailing whitespace is invalid for identifiers", pattern.matcher("invalid-trailing-ws ").find());

        //-- Test the allowed cases
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this-is-valid").find());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this_is_valid").find());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this_is-valid").find());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("this-is_valid").find());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("-thisisvalid-").find());
        Assert.assertTrue("Should be valid identifier", pattern.matcher("_thisisvalid_").find());

        //-- Test the not allowed cases
        testPatternDoesNotAllowSpecialCharsOtherThanSpecified("test_-", pattern, '_','-');
    }

    @Test
    void testNameRegex() {
        Pattern pattern = Pattern.compile(HiveMQEdgeConstants.NAME_REGEX);

        //-- Test for leading and trailing whitespace
        Assert.assertFalse("Leading whitespace is invalid for name", pattern.matcher(" invalid-leading-ws").find());
        Assert.assertFalse("Trailing whitespace is invalid for name", pattern.matcher("invalid-trailing-ws ").find());

        //-- Test the allowed cases
        Assert.assertTrue("Should be valid name", pattern.matcher("A Valid Name").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("this-is-valid").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("this_is_valid").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("this_is-valid").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("this-is_valid").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("-thisisvalid-").find());
        Assert.assertTrue("Should be valid name", pattern.matcher("_thisisvalid_").find());

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
