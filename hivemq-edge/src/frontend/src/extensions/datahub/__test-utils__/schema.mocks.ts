export const MOCK_PROTOBUF_SCHEMA = `syntax = "proto3";

message GpsCoordinates {
  int32 longitude = 1;
  int32 latitude = 2;
}
`

export const MOCK_JSONSCHEMA_SCHEMA = `{
   "$schema":"https://json-schema.org/draft/2020-12/schema",
   "title":"",
   "description":"",
   "required":[],
   "type":"object",
   "properties":{}
}
`

export const MOCK_JAVASCRIPT_SCHEMA = `
/**
 *
 * @param {Object} publish
 * @param {string} publish.topic    The MQTT topic that is currently specified for this PUBLISH packet.
 * @param {Object} publish.payload  A list of the name and value of all user properties of the MQTT 5 PUBLISH packet. This setting has no effect on MQTT 3 clients.
 * @param {Record<string, string>[]} publish.userProperties The JSON object representation of the deserialized MQTT payload.
 * @param {Object} context
 * @param {Record<string, string>[]} context.arguments  The arguments provided to the script. Currently, arguments can only be provided via a data policy.
 * @param {string} context.policyId The policy id of the policy from which the transformation function is called.
 * @param {string} context.clientId The client Id of the client from which the MQTT publish was sent.
 * @returns {Object} The publish-object is passed as a parameter into the transform function. The same object or a new object is returned as the transformed object.
 */
function transform(publish, context) {
  return publish
}

`
