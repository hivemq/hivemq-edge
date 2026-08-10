import type { RJSFSchema, UiSchema } from '@rjsf/utils'

/**
 * The OPC UA adapter's certificate-validation surface, copied verbatim from the backend module.
 *
 * Deliberately separate from `opc-ua.ts`: that fixture is a whole-adapter snapshot taken from Edge
 * 2025.5 and marked "do not edit", and it predates the presets/axes model entirely — no `tlsChecks`,
 * no `tlsChecksFull`, no `allowList`, and no `tls` block in its uiSchema. Regenerating it is its own
 * job; this file covers the one surface under test.
 *
 * Sources, both of which have backend guards of their own:
 *  - the data schema is what `CustomConfigSchemaGenerator` produces for `Tls`, pinned in
 *    `ConfigSchemaIT.test_opcua_configSchemaStaysTheSame`;
 *  - the uiSchema is the `tls` block of `opcua-adapter-ui-schema.json`, checked against that data
 *    schema by `UiSchemaSurfaceTest`.
 *
 * Keep it verbatim. The point of the spec that uses it is that RJSF is handed exactly what Edge
 * serves, so an approximation here would certify a form the product does not have.
 */
export const MOCK_OPC_UA_TLS_SCHEMA: RJSFSchema = {
  type: 'object',
  properties: {
    tls: {
      type: 'object',
      title: 'TLS Configuration',
      description: 'Configure TLS for use with X509 or connecting to a TLS enabled OPC UA server.',
      properties: {
        enabled: {
          type: 'boolean',
          title: 'Enable TLS',
          description: 'Enables TLS encrypted connection',
          default: false,
        },
        tlsChecks: {
          type: 'string',
          title: 'Certificate validation preset',
          description: 'Named certificate-validation profile. Leaving this unset means STANDARD.',
          enum: ['NONE', 'APPLICATION_URI', 'STANDARD', 'ALL', 'SELF_SIGNED', 'NO_VERIFICATION'],
        },
        tlsChecksFull: {
          type: 'object',
          title: 'Certificate validation (full control)',
          description: 'The six independent validation axes. Mutually exclusive with tlsChecks.',
          properties: {
            trustMode: {
              type: 'string',
              title: 'Trust mode',
              enum: ['CHAIN', 'ALLOW_LIST', 'ANY_CERT'],
            },
            sanUri: {
              type: 'string',
              title: 'SubjectAltName URI check',
              enum: ['NONE', 'APPLICATION_URI'],
            },
            hostname: {
              type: 'string',
              title: 'Hostname check',
              enum: ['NONE', 'HOSTNAME'],
            },
            validity: {
              type: 'string',
              title: 'Validity period check',
              enum: ['NONE', 'NOT_BEFORE_OR_AFTER'],
            },
            revocation: {
              type: 'string',
              title: 'Revocation check',
              enum: ['NONE', 'CHECK', 'REQUIRE_CRLS'],
            },
            keyUsage: {
              type: 'string',
              title: 'Key usage check',
              enum: ['NONE', 'KEY_USAGE', 'SERVER_AUTH'],
            },
          },
        },
        allowList: {
          type: 'object',
          title: 'Certificate allow-list',
          description: 'Allow-list of permitted server-certificate SHA-256 fingerprints.',
          properties: {
            path: {
              type: 'string',
              title: 'Allow-list path',
            },
          },
          required: ['path'],
        },
        keystore: {
          type: 'object',
          title: 'Keystore',
          properties: {
            path: { type: 'string', title: 'Keystore path' },
            password: { type: 'string', title: 'Keystore password' },
            privateKeyPassword: { type: 'string', title: 'Private key password' },
          },
        },
        truststore: {
          type: 'object',
          title: 'Truststore',
          properties: {
            path: { type: 'string', title: 'Truststore path' },
            password: { type: 'string', title: 'Truststore password' },
          },
        },
      },
    },
  },
}

/** The `tls` block of `opcua-adapter-ui-schema.json`, verbatim. */
export const MOCK_OPC_UA_TLS_UI_SCHEMA: UiSchema = {
  tls: {
    'ui:order': ['enabled', 'tlsChecks', 'tlsChecksFull', 'allowList', 'keystore', 'truststore', '*'],
    tlsChecks: {
      'ui:enumNames': [
        'NONE: Chain validation only - the trust chain is still built.',
        'APPLICATION_URI: Chain validation plus the Application URI check.',
        'STANDARD: Default - chain, Application URI, validity period and revocation.',
        'ALL: STANDARD plus hostname verification and key-usage enforcement.',
        'SELF_SIGNED: Fingerprint allow-list plus Application URI, hostname and validity - for servers without a CA.',
        'NO_VERIFICATION: Accept any certificate - vulnerable to MITM, prefer SELF_SIGNED.',
      ],
    },
    tlsChecksFull: {
      'ui:order': ['trustMode', 'sanUri', 'hostname', 'validity', 'revocation', 'keyUsage', '*'],
    },
    allowList: {
      'ui:order': ['path', '*'],
    },
    keystore: {
      'ui:order': ['path', 'password', 'privateKeyPassword'],
    },
    truststore: {
      'ui:order': ['path', 'password'],
    },
  },
}
