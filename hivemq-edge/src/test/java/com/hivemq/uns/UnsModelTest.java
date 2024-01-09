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
package com.hivemq.uns;

import com.hivemq.api.utils.ApiValidation;
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
public class UnsModelTest {

    @Test
    void testApiAlphaNumValidation_empty_null() {

        Assert.assertTrue("Empty string should be allowed when specified",
                ApiValidation.validAlphaNumericSpaces("", true));
        Assert.assertFalse("Empty string should NOT be allowed when specified",
                ApiValidation.validAlphaNumericSpaces("", false));

        Assert.assertTrue("<null> string should be allowed when specified",
                ApiValidation.validAlphaNumericSpaces(null, true));
        Assert.assertFalse("<null> string should NOT be allowed when specified",
                ApiValidation.validAlphaNumericSpaces(null, false));
    }

    @Test
    void testApiAlphaNumSpacesValidation() {

        Assert.assertTrue("Valid value (with spaces) should be valid",
                ApiValidation.validAlphaNumericSpaces("valid value", false));

        Assert.assertTrue("Valid value (without spaces) should be valid",
                ApiValidation.validAlphaNumericSpaces("validvalue", false));

        Assert.assertTrue("Multi character should be valid",
                ApiValidation.validAlphaNumericSpaces("vv", false));

        Assert.assertTrue("Multi character with space should be valid",
                ApiValidation.validAlphaNumericSpaces("v ", false));

        Assert.assertTrue("Single character should be valid",
                ApiValidation.validAlphaNumericSpaces("v", false));

        Assert.assertFalse("Special char should not be invalid",
                ApiValidation.validAlphaNumericSpaces("\'", false));

        Assert.assertTrue("Mixed case should be valid",
                ApiValidation.validAlphaNumericSpaces("aAb", false));
    }

    @Test
    void testApiAlphaNumValidation() {

        Assert.assertFalse("Invalid value (with spaces) should be invalid",
                ApiValidation.validAlphaNumeric("invalid value", false));

        Assert.assertTrue("Valid value (without spaces) should be valid",
                ApiValidation.validAlphaNumeric("validvalue", false));

        Assert.assertTrue("Multi character should be valid",
                ApiValidation.validAlphaNumeric("vv", false));

        Assert.assertFalse("Multi character with space should NOT be valid",
                ApiValidation.validAlphaNumeric("v ", false));

        Assert.assertTrue("Single character should be valid",
                ApiValidation.validAlphaNumeric("v", false));

        Assert.assertFalse("Special char should not be invalid",
                ApiValidation.validAlphaNumeric("\'", false));

        Assert.assertTrue("Mixed case should be valid",
                ApiValidation.validAlphaNumericSpaces("aAb", false));
    }

    @Test
    void testApiAlphaNumSpacesAndDashesValidation() {

        Assert.assertTrue("Valid value (with spaces) should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("valid value", false));

        Assert.assertTrue("Valid value (without spaces) should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("validvalue", false));

        Assert.assertTrue("Multi character should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("vv", false));

        Assert.assertTrue("Multi character with space should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("v ", false));

        Assert.assertTrue("Single character should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("v", false));

        Assert.assertFalse("Special char should not be invalid",
                ApiValidation.validAlphaNumericSpacesAndDashes("\'", false));

        Assert.assertTrue("Mixed case should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("aAb", false));

        Assert.assertTrue("Dashes should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("a-b", false));

        Assert.assertTrue("Leading dashes should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("-ab", false));

        Assert.assertTrue("Trailing dashes should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("ab-", false));

        Assert.assertTrue("Underscores should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("a_b", false));

        Assert.assertTrue("Trailing Underscores should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("a_", false));

        Assert.assertTrue("Leading Underscores should be valid",
                ApiValidation.validAlphaNumericSpacesAndDashes("_a", false));
    }
}
