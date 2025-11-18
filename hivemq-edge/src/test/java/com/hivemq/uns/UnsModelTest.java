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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Simon L Johnson
 */
public class UnsModelTest {

    @Test
    void testApiAlphaNumValidation_empty_null() {

        assertTrue(ApiValidation.validAlphaNumericSpaces("", true),
                "Empty string should be allowed when specified");
        assertFalse(ApiValidation.validAlphaNumericSpaces("", false),
                "Empty string should NOT be allowed when specified");

        assertTrue(ApiValidation.validAlphaNumericSpaces(null, true),
                "<null> string should be allowed when specified");
        assertFalse(ApiValidation.validAlphaNumericSpaces(null, false),
                "<null> string should NOT be allowed when specified");
    }

    @Test
    void testApiAlphaNumSpacesValidation() {

        assertTrue(ApiValidation.validAlphaNumericSpaces("valid value", false),
                "Valid value (with spaces) should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("validvalue", false),
                "Valid value (without spaces) should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("vv", false),
                "Multi character should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("v ", false),
                "Multi character with space should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("v", false),
                "Single character should be valid");

        assertFalse(ApiValidation.validAlphaNumericSpaces("\'", false),
                "Special char should not be invalid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("aAb", false),
                "Mixed case should be valid");
    }

    @Test
    void testApiAlphaNumValidation() {

        assertFalse(ApiValidation.validAlphaNumeric("invalid value", false),
                "Invalid value (with spaces) should be invalid");

        assertTrue(ApiValidation.validAlphaNumeric("validvalue", false),
                "Valid value (without spaces) should be valid");

        assertTrue(ApiValidation.validAlphaNumeric("vv", false),
                "Multi character should be valid");

        assertFalse(ApiValidation.validAlphaNumeric("v ", false),
                "Multi character with space should NOT be valid");

        assertTrue(ApiValidation.validAlphaNumeric("v", false),
                "Single character should be valid");

        assertFalse(ApiValidation.validAlphaNumeric("\'", false),
                "Special char should not be invalid");

        assertTrue(ApiValidation.validAlphaNumericSpaces("aAb", false),
                "Mixed case should be valid");
    }

    @Test
    void testApiAlphaNumSpacesAndDashesValidation() {

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("valid value", false),
                "Valid value (with spaces) should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("validvalue", false),
                "Valid value (without spaces) should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("vv", false),
                "Multi character should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("v ", false),
                "Multi character with space should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("v", false),
                "Single character should be valid");

        assertFalse(ApiValidation.validAlphaNumericSpacesAndDashes("\'", false),
                "Special char should not be invalid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("aAb", false),
                "Mixed case should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("a-b", false),
                "Dashes should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("-ab", false),
                "Leading dashes should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("ab-", false),
                "Trailing dashes should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("a_b", false),
                "Underscores should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("a_", false),
                "Trailing Underscores should be valid");

        assertTrue(ApiValidation.validAlphaNumericSpacesAndDashes("_a", false),
                "Leading Underscores should be valid");
    }
}
