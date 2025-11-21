/**
 * DataHub Transform Script Template
 * This is the boilerplate code inserted when users choose "Insert DataHub Transform Template"
 */

/**
 * @param {InitContext} initContext - Context providing setup methods
 */
function init(initContext) {
  // Create branches for message routing (optional)
  // const errorBranch = initContext.addBranch('error-handling');
  // Create client connection state (optional)
  // const messageCount = initContext.addClientConnectionState('count', 0);
}

/**
 * @param {Publish} publish - The MQTT PUBLISH packet to transform
 * @param {TransformContext} context - Transformation context
 * @returns {Publish|null} The transformed message, or null to drop it
 */
function transform(publish, context) {
  // Example: Add timestamp to payload
  // publish.payload.timestamp = Date.now();

  // Example: Modify topic
  // publish.topic = 'processed/' + publish.topic;

  // Example: Add user property (MQTT 5)
  // publish.userProperties.push({
  //   name: 'processedBy',
  //   value: 'datahub-transform'
  // });

  return publish
}
