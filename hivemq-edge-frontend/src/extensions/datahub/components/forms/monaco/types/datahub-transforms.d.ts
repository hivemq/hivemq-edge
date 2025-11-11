/**
 * DataHub JavaScript Transform API Type Definitions
 *
 * These type definitions provide IntelliSense support for writing DataHub transform functions.
 * They define the structure and behavior of the init() and transform() functions used in
 * DataHub data policies.
 *
 * @module datahub-transforms
 */

/**
 * User property entry for MQTT 5 messages.
 * Each user property consists of a name-value pair, both of type string.
 *
 * @example
 * const userProps: UserProperty[] = [
 *   { name: 'correlationId', value: '12345' },
 *   { name: 'source', value: 'sensor-01' }
 * ];
 */
interface UserProperty {
  /** The name/key of the user property */
  name: string
  /** The value of the user property */
  value: string
}

/**
 * Client connection state object for maintaining state across message transformations.
 * Use this to store and retrieve stateful information per client connection.
 *
 * @example
 * // In init function:
 * const messageCount = initContext.addClientConnectionState('count', 0);
 *
 * // In transform function:
 * messageCount.value++; // Increment counter
 * console.log('Message count:', messageCount.value);
 */
interface ClientConnectionState {
  /**
   * The current value of the state.
   * Can be read and modified during transform execution.
   */
  value: unknown
}

/**
 * Branch object for routing messages to different processing paths.
 * Branches allow you to create multiple output messages from a single input message.
 *
 * @example
 * // In init function:
 * const errorBranch = initContext.addBranch('error-handling');
 *
 * // In transform function:
 * if (publish.payload.error) {
 *   errorBranch.addPublish({
 *     topic: 'errors/' + publish.topic,
 *     payload: { error: publish.payload.error }
 *   });
 * }
 */
interface Branch {
  /**
   * Adds a new MQTT PUBLISH message to this branch.
   *
   * @param publish - The publish object to add to this branch
   * @returns The branch instance for chaining
   *
   * @example
   * errorBranch.addPublish({
   *   topic: 'alerts/temperature',
   *   qos: 1,
   *   retain: true,
   *   payload: { alert: 'High temperature detected' }
   * });
   */
  addPublish(publish: Partial<Publish>): Branch
}

/**
 * MQTT PUBLISH packet object.
 * Represents an MQTT message that can be transformed and routed.
 *
 * All properties can be read and modified during transformation.
 */
interface Publish {
  /**
   * The MQTT topic for this PUBLISH packet.
   * Can be modified to route the message to a different topic.
   *
   * @example
   * publish.topic = 'sensors/' + context.clientId + '/temperature';
   */
  topic: string

  /**
   * Quality of Service level for the PUBLISH packet.
   * Valid values: 0 (at most once), 1 (at least once), 2 (exactly once)
   *
   * Defaults to the same QoS as the original incoming message.
   *
   * @example
   * publish.qos = 1; // Ensure at-least-once delivery
   */
  qos: 0 | 1 | 2

  /**
   * Whether the PUBLISH packet is a retained message.
   * Retained messages are stored by the broker and delivered to new subscribers.
   *
   * Defaults to the same retain flag as the original message, or false if used with addPublish.
   *
   * @example
   * publish.retain = true; // Make this a retained message
   */
  retain: boolean

  /**
   * The payload of the MQTT message.
   * Must be a JSON-serializable value (object, array, string, number, boolean, null).
   *
   * @example
   * // Reading payload
   * const temperature = publish.payload.temperature;
   *
   * // Modifying payload
   * publish.payload = {
   *   ...publish.payload,
   *   timestamp: Date.now(),
   *   processed: true
   * };
   */
  payload: Record<string, unknown> | unknown

  /**
   * User properties for MQTT 5 PUBLISH packets.
   * A list of name-value pairs for application-specific metadata.
   *
   * Note: Modifying this property has no effect on MQTT 3 clients.
   * Defaults to an empty array.
   *
   * @example
   * // Add a user property
   * publish.userProperties.push({
   *   name: 'processedBy',
   *   value: 'datahub-transform'
   * });
   *
   * // Read user properties
   * const correlationId = publish.userProperties.find(
   *   prop => prop.name === 'correlationId'
   * )?.value;
   */
  userProperties: UserProperty[]
}

/**
 * Context object passed to the init function.
 * Provides methods for setting up branches and client connection states.
 *
 * The init function is called once when the script is loaded.
 * Use it to set up any branches or persistent state needed for transformations.
 */
interface InitContext {
  /**
   * Creates a new branch for routing messages.
   * Branches allow you to send messages to different topics or processing paths.
   *
   * @param branchId - Unique identifier for the branch
   * @returns Branch object that can be used in the transform function
   *
   * @example
   * function init(initContext) {
   *   // Create branches for different message types
   *   const highPriorityBranch = initContext.addBranch('high-priority');
   *   const errorBranch = initContext.addBranch('error-handling');
   *   const archiveBranch = initContext.addBranch('archive');
   *
   *   return { highPriorityBranch, errorBranch, archiveBranch };
   * }
   */
  addBranch(branchId: string): Branch

  /**
   * Defines a client connection state variable.
   * State is maintained per client connection and persists across message transformations.
   *
   * @param stateId - Unique identifier for the state variable
   * @param defaultValue - Initial value for the state
   * @returns ClientConnectionState object that can be read/written in transform function
   *
   * @example
   * function init(initContext) {
   *   // Track message count per client
   *   const messageCount = initContext.addClientConnectionState('messageCount', 0);
   *
   *   // Track last message timestamp
   *   const lastMessageTime = initContext.addClientConnectionState('lastMessageTime', null);
   *
   *   // Track connection state
   *   const connectionInfo = initContext.addClientConnectionState('connectionInfo', {
   *     firstMessage: null,
   *     totalMessages: 0
   *   });
   *
   *   return { messageCount, lastMessageTime, connectionInfo };
   * }
   */
  addClientConnectionState(stateId: string, defaultValue: unknown): ClientConnectionState
}

/**
 * Context object passed to the transform function.
 * Provides information about the current transformation context and access to branches.
 */
interface TransformContext {
  /**
   * Arguments provided to the script.
   * Currently, arguments can only be provided via a data policy configuration.
   *
   * @example
   * // Access arguments
   * const config = context.arguments[0];
   * const maxValue = config?.maxValue || 100;
   */
  arguments: Record<string, string>[]

  /**
   * The policy ID of the policy from which the transformation function is called.
   * Useful for logging and debugging.
   *
   * @example
   * console.log('Executing policy:', context.policyId);
   */
  policyId: string

  /**
   * The client ID of the client from which the MQTT PUBLISH was sent.
   * Useful for client-specific processing or routing.
   *
   * @example
   * // Route based on client ID
   * if (context.clientId.startsWith('sensor-')) {
   *   publish.topic = 'sensors/' + context.clientId + '/' + publish.topic;
   * }
   */
  clientId: string

  /**
   * Access to branches created in the init function.
   * Use branches to route messages to different processing paths.
   *
   * @example
   * // Assuming branches were created in init and stored in a variable
   * if (publish.payload.priority === 'high') {
   *   context.branches['high-priority'].addPublish(publish);
   * }
   */
  branches: Record<string, Branch>
}

/**
 * Initialization function for the DataHub transform script.
 * Called once when the script is loaded, before any messages are transformed.
 *
 * Use this function to:
 * - Create branches for message routing
 * - Define client connection state variables
 * - Set up any initialization logic
 *
 * @param initContext - Context providing setup methods
 * @returns Optional object containing branches and state variables for use in transform()
 *
 * @example
 * function init(initContext) {
 *   // Create branches
 *   const errorBranch = initContext.addBranch('error-handling');
 *   const validBranch = initContext.addBranch('valid-messages');
 *
 *   // Create state
 *   const messageCount = initContext.addClientConnectionState('count', 0);
 *
 *   // Return for use in transform function
 *   return { errorBranch, validBranch, messageCount };
 * }
 */
declare function init(initContext: InitContext): Record<string, unknown> | void

/**
 * Transform function for processing MQTT PUBLISH packets.
 * Called for each incoming MQTT message.
 *
 * The function can:
 * - Modify the publish object (topic, payload, qos, retain, userProperties)
 * - Return the modified publish object to continue processing
 * - Return a new publish object to replace the original
 * - Return null/undefined to drop the message
 * - Use branches to create additional messages
 *
 * @param publish - The MQTT PUBLISH packet to transform
 * @param context - Context information about the transformation
 * @returns The transformed publish object, or null to drop the message
 *
 * @example
 * function transform(publish, context) {
 *   // Add timestamp to payload
 *   publish.payload.timestamp = Date.now();
 *   publish.payload.processedBy = context.policyId;
 *
 *   // Modify topic based on client ID
 *   publish.topic = 'processed/' + context.clientId + '/' + publish.topic;
 *
 *   // Add user property (MQTT 5)
 *   publish.userProperties.push({
 *     name: 'transformedAt',
 *     value: new Date().toISOString()
 *   });
 *
 *   return publish;
 * }
 *
 * @example
 * // Example with state and branches (from init return value)
 * const { messageCount, errorBranch } = init(initContext);
 *
 * function transform(publish, context) {
 *   // Update state
 *   messageCount.value++;
 *
 *   // Validate payload
 *   if (!publish.payload.temperature) {
 *     errorBranch.addPublish({
 *       topic: 'errors/missing-temperature',
 *       payload: { error: 'Missing temperature', original: publish.payload }
 *     });
 *     return null; // Drop original message
 *   }
 *
 *   // Enrich with state
 *   publish.payload.messageNumber = messageCount.value;
 *
 *   return publish;
 * }
 */
declare function transform(publish: Publish, context: TransformContext): Publish | null | undefined
