/**
 * DataHub Transform API Type Definitions
 * Exported as string for Monaco addExtraLib
 */

export const DATAHUB_TYPES = `
/**
 * User property entry for MQTT 5 messages.
 */
interface UserProperty {
  name: string;
  value: string;
}

/**
 * Client connection state object for maintaining state across message transformations.
 */
interface ClientConnectionState {
  value: any;
}

/**
 * Branch object for routing messages to different processing paths.
 */
interface Branch {
  /**
   * Adds a new MQTT PUBLISH message to this branch.
   * @param publish - Partial publish object with properties to set
   */
  addPublish(publish: Partial<Publish>): Branch;
}

/**
 * MQTT PUBLISH packet object.
 * Represents an MQTT message that can be transformed and routed.
 */
interface Publish {
  /** The MQTT topic for this PUBLISH packet */
  topic: string;

  /** Quality of Service level (0, 1, or 2) */
  qos: 0 | 1 | 2;

  /** Whether this is a retained message */
  retain: boolean;

  /** The payload of the MQTT message */
  payload: any;

  /** User properties for MQTT 5 (array of name-value pairs) */
  userProperties: UserProperty[];
}

/**
 * Context object passed to the init function.
 */
interface InitContext {
  /**
   * Creates a new branch for routing messages.
   * @param branchId - Unique identifier for the branch
   */
  addBranch(branchId: string): Branch;

  /**
   * Defines a client connection state variable.
   * @param stateId - Unique identifier for the state
   * @param defaultValue - Initial value for the state
   */
  addClientConnectionState(stateId: string, defaultValue: any): ClientConnectionState;
}

/**
 * Context object passed to the transform function.
 */
interface TransformContext {
  /** Arguments provided to the script via data policy */
  arguments: Record<string, string>[];

  /** The policy ID from which the transformation is called */
  policyId: string;

  /** The client ID of the MQTT client */
  clientId: string;

  /** Access to branches created in init function */
  branches: Record<string, Branch>;
}

/**
 * Initialization function for DataHub transform script.
 * Called once when the script is loaded.
 * @param initContext - Context providing setup methods
 */
declare function init(initContext: InitContext): any;

/**
 * Transform function for processing MQTT PUBLISH packets.
 * Called for each incoming MQTT message.
 * @param publish - The MQTT PUBLISH packet to transform
 * @param context - Context information about the transformation
 * @returns The transformed publish object, or null to drop the message
 */
declare function transform(publish: Publish, context: TransformContext): Publish | null | undefined;

// Make these available globally for user code
declare const publish: Publish;
declare const context: TransformContext;
declare const initContext: InitContext;
`
