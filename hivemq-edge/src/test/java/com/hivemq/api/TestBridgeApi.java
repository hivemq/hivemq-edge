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
package com.hivemq.api;


import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestBridgeApi {

    private static final String BASE_URL = "http://localhost:8080";

    @Disabled
    @Test
    public void testAddBridgeConnectionString() {
        String connectionString = "eyJjbGVhblN0YXJ0IjogdHJ1ZSwgImNsaWVudElkIjogIm15LWV4YW1wbGUtY2xpZW50LWlkIiwgImhvc3QiOiAic3RyaW5nIiwgImlkIjogInh5M1pZT0s5diIsICJrZWVwQWxpdmUiOiAyNDAsICJsb2NhbFN1YnNjcmlwdGlvbnMiOiBbIHsKICAgICJjdXN0b21Vc2VyUHJvcGF0aWVzIjogWwogICAgICAgIHsKICAgICAgICAgICJrZXkiOiAiImlkIiwgInZhbHVlIjogInN0cmluZyIsCiAgICAgICAgICJ2YWx1ZSI6ICJzdHJpbmciCiwgICAgICAgfQogICAgICB9XSwKICAgICAgImRlc3Rpb25hdGlvbiI6ICJzb21lL3RvcGljL3ZhbHVlIiwgICAgICAgCiAgICAgImV4Y2x1c3MiOiBbCiAgICAgICAgInN0cmluZyIsICJzdHJpbmciLAogICAgICAgfSwKICAgICAgImZpbHRlcnMiOiAic29tZS90b3BpYy92YWx1ZSIsCiAg\n";
        // TODO gen oauthToken (automated)
        String oauthToken = "eyJraWQiOiIwMDAwMSIsImFsZyI6IlJTMjU2In0.eyJqdGkiOiJFRExXRlJKWWRVcGRtSE1TTlhTdzhnIiwiaWF0IjoxNjk3NjQxMTIzLCJhdWQiOiJIaXZlTVEtRWRnZS1BcGkiLCJpc3MiOiJIaXZlTVEtRWRnZSIsImV4cCI6MTY5NzY0MjkyMywibmJmIjoxNjk3NjQxMDAzLCJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIkFETUlOIl19.j_mbmW0zkakibyNLKx-MJdXBxampm0UqYpiK_R4LZFErysvSiEctlajoIpXYJgzlVB-Ho4OJfsm9oGydsvroMdBTacZZ7IynjbhE8xx369P365aJkTJOKdoEHmLHAHFLP3gU6vVg6j0aWnl-O8szcIG84lXZoxM_WEPMugwZxfQu-8G2KUYT0oiZqv4IEqm91E7SwEzPcgPUTEzw-dJX2TyeNuYtXNHoqooPN9_ITB8mhI4v68RfGq9L4PnfG_Qw82T0xvHm8bvoD3PZB1jlZ1KicawPmm1-LDjBmYzyLyAqmroWYB4Ul208EVH8RRUibd48rvA-55RFnx2As1b9GQ";
        // Send a POST request to the API endpoint
        RestAssured.given()
                .auth().oauth2(oauthToken)
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"connectionString\": \"" + connectionString + "\"}")
                .when()
                .post("/api/v1/management/bridges")
                .then()
                .statusCode(201);
    }
}
