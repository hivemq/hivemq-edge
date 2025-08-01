import { MaxQoS } from '@/api/schemas/definitions/MaxQoS.json-schema.ts'
import type { JSONSchema7 } from 'json-schema'

import { StatusConnection, StatusRuntime } from '@/api/schemas/definitions'
import { CYPHER_SUITES, TLS_PROTOCOLS } from '@/modules/Bridges/utils/tlsConfiguration.ts'

/**
 * TODO[NVL] Required as the schema generated by the codegen is of an older version of JSONSchema (v5) and not well structured ($ref missing)
 *  Conditional properties are also not properly defined in the OpenAPI specs
 */
/* istanbul ignore next -- @preserve */
export const bridgeSchema: JSONSchema7 = {
  definitions: {
    BridgeCustomUserProperty: {
      description: `The customUserProperties for this subscription`,
      properties: {
        key: {
          type: 'string',
          description: `The key from the property`,
        },
        value: {
          type: 'string',
          description: `The value from the property`,
        },
      },
    },
    LocalBridgeSubscription: {
      description: `localSubscriptions associated with the bridge`,
      properties: {
        customUserProperties: {
          type: 'array',
          items: {
            $ref: '#/definitions/BridgeCustomUserProperty',
          },
        },
        destination: {
          type: 'string',
          description: `The destination topic for this filter set.`,
          format: 'mqtt-topic',
        },
        excludes: {
          type: 'array',
          items: {
            type: 'string',
            description: `The exclusion patterns`,
          },
        },
        filters: {
          type: 'array',
          items: {
            type: 'string',
            description: `The filters for this subscription.`,
            format: 'mqtt-topic-filter',
          },
        },
        maxQoS: MaxQoS,
        preserveRetain: {
          type: 'boolean',
          description: `The preserveRetain for this subscription`,
        },
        queueLimit: {
          type: 'number',
          description: `The limit of this bridge for QoS-1 and QoS-2 messages.`,
          format: 'int64',
        },
      },
    },
    BridgeSubscription: {
      description: `remoteSubscriptions associated with the bridge`,
      properties: {
        customUserProperties: {
          type: 'array',
          items: {
            $ref: '#/definitions/BridgeCustomUserProperty',
          },
        },
        destination: {
          type: 'string',
          description: `The destination topic for this filter set.`,
          format: 'mqtt-topic',
        },
        filters: {
          type: 'array',
          items: {
            type: 'string',
            description: `The filters for this subscription.`,
            format: 'mqtt-topic-filter',
          },
        },
        maxQoS: MaxQoS,
        preserveRetain: {
          type: 'boolean',
          description: `The preserveRetain for this subscription`,
        },
      },
    },
    Status: {
      description: `Information associated with the runtime of this adapter`,
      properties: {
        connection: StatusConnection,
        id: {
          type: 'string',
          description: `The identifier of the object`,
        },
        lastActivity: {
          type: 'string',
          description: `The datetime of the last activity through this connection`,
          format: 'date-time',
        },
        message: {
          type: 'string',
          description: `A message associated with the state of a connection`,
        },
        runtime: StatusRuntime,
        startedAt: {
          type: 'string',
          description: `The datetime the object was 'started' in the system.`,
          format: 'date-time',
        },
        type: {
          type: 'string',
          description: `The type of the object`,
        },
      },
    },
    TlsConfiguration: {
      description: `tlsConfiguration associated with the bridge`,
      properties: {
        enabled: {
          type: 'boolean',
          description: `If TLS is used`,
          default: false,
        },
      },
      if: {
        properties: {
          enabled: {
            const: true,
          },
        },
      },
      then: {
        properties: {
          cipherSuites: {
            type: 'array',
            uniqueItems: true,
            items: {
              type: 'string',
              description: `The cipherSuites from the config`,
              enum: CYPHER_SUITES,
            },
          },
          handshakeTimeout: {
            type: 'number',
            description: `The handshakeTimeout from the config`,
            format: 'int32',
          },
          keystorePassword: {
            type: 'string',
            description: `The keystorePassword from the config`,
          },
          keystorePath: {
            type: 'string',
            description: `The keystorePath from the config`,
          },
          keystoreType: {
            type: 'string',
            description: `The keystoreType from the config`,
          },
          privateKeyPassword: {
            type: 'string',
            description: `The privateKeyPassword from the config`,
          },
          protocols: {
            type: 'array',
            uniqueItems: true,
            items: {
              type: 'string',
              description: `The protocols from the config`,
              enum: TLS_PROTOCOLS,
            },
          },
          truststorePassword: {
            type: 'string',
            description: `The truststorePassword from the config`,
          },
          truststorePath: {
            type: 'string',
            description: `The truststorePath from the config`,
          },
          truststoreType: {
            type: 'string',
            description: `The truststoreType from the config`,
          },
          verifyHostname: {
            type: 'boolean',
            description: `The verifyHostname from the config`,
          },
        },
      },
    },
    WebsocketConfiguration: {
      description: `websocketConfiguration associated with the bridge`,
      properties: {
        enabled: {
          type: 'boolean',
          description: `If Websockets are used`,
          default: false,
        },
      },
      if: {
        properties: {
          enabled: {
            const: true,
          },
        },
      },
      then: {
        properties: {
          serverPath: {
            type: 'string',
            description: `The server path used by the bridge client. This must be setup as path at the remote broker`,
            default: '/mqtt',
          },
          subProtocol: {
            type: 'string',
            description: `The sub-protocol used by the bridge client. This must be supported by the remote broker`,
            default: 'mqtt',
          },
        },
      },
    },
    Bridge: {
      required: ['id', 'host', 'port', 'clientId', 'keepAlive', 'sessionExpiry'],
      properties: {
        cleanStart: {
          type: 'boolean',
          description: `The cleanStart value associated the the MQTT connection.`,
          format: 'boolean',
          default: true,
        },
        clientId: {
          type: 'string',
          description: `The client identifier associated the the MQTT connection.`,
          maxLength: 65535,
        },
        host: {
          type: 'string',
          description: `The host the bridge connects to - a well formed hostname, ipv4 or ipv6 value.`,
          maxLength: 255,
        },
        id: {
          type: 'string',
          description: `The bridge id, must be unique and only contain alpha numeric characters with spaces and hyphens.`,
          maxLength: 500,
          minLength: 1,
          pattern: '^([a-zA-Z_0-9-_])*$',
          format: 'identifier',
        },
        keepAlive: {
          type: 'number',
          description: `The keepAlive associated the the MQTT connection.`,
          format: 'int32',
          maximum: 65535,
          default: 60,
        },
        localSubscriptions: {
          type: 'array',
          items: {
            $ref: '#/definitions/LocalBridgeSubscription',
          },
        },
        loopPreventionEnabled: {
          type: 'boolean',
          description: `Is loop prevention enabled on the connection`,
          default: false,
        },
        password: {
          type: 'string',
          description: `The password value associated the the MQTT connection.`,
          maxLength: 65535,
          format: 'password',
        },
        persist: {
          type: 'boolean',
          description: `If this flag is set to true, any outgoing mqtt messages with QoS-1 or QoS-2 will be persisted on disc in case disc persistence is active.If this flag is set to false, the QoS of any outgoing mqtt messages will be set to QoS-0 and no traffic will be persisted on disc.`,
        },
        port: {
          type: 'number',
          description: `The port number to connect to`,
          format: 'int32',
          maximum: 65535,
          minimum: 1,
          default: 1883,
        },
        remoteSubscriptions: {
          type: 'array',
          items: {
            $ref: '#/definitions/BridgeSubscription',
          },
        },
        sessionExpiry: {
          type: 'number',
          description: `The sessionExpiry associated the the MQTT connection.`,
          format: 'int64',
          default: 3600,
        },
        status: {
          $ref: '#/definitions/Status',
        },
        tlsConfiguration: {
          $ref: '#/definitions/TlsConfiguration',
        },
        username: {
          type: 'string',
          description: `The username value associated the the MQTT connection.`,
          maxLength: 65535,
        },
        websocketConfiguration: {
          $ref: '#/definitions/WebsocketConfiguration',
        },
      },
      if: {
        properties: {
          loopPreventionEnabled: {
            const: true,
          },
        },
      },
      then: {
        required: ['loopPreventionHopCount'],
        properties: {
          loopPreventionHopCount: {
            type: 'number',
            description: `Loop prevention hop count`,
            format: 'int32',
            maximum: 100,
          },
        },
      },
    },
  },

  type: 'object',
  $ref: '#/definitions/Bridge',
}
