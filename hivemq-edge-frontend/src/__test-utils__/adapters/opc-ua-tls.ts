import type { RJSFSchema, UiSchema } from '@rjsf/utils'

import artifact from './generated/opc-ua-tls.schema.json'

/**
 * The OPC UA adapter's certificate-validation surface, as Edge serves it.
 *
 * Not a transcription. `generated/opc-ua-tls.schema.json` is written by
 * `TlsSchemaArtifactTest` in `hivemq-edge-module-opcua` from the two things the product actually uses:
 * the data schema `CustomConfigSchemaGenerator` produces for the adapter config, and the `tls` block
 * of `opcua-adapter-ui-schema.json`. Change either on the backend and that test rewrites this artifact
 * and fails until the new version is committed — the same staleness gate the committed OpenAPI spec
 * has.
 *
 * That gate is the point. A hand-maintained copy let both backend guards stay green while this file
 * drifted: `UiSchemaSurfaceTest` checks the real UI schema against the real data schema and
 * `ConfigSchemaIT` pins the served data schema, but neither had any relationship to what the frontend
 * test rendered. The RJSF spec could keep passing against a self-consistent copy of a form the product
 * no longer had.
 *
 * Deliberately separate from `opc-ua.ts`: that fixture is a whole-adapter snapshot taken from Edge
 * 2025.5 and marked "do not edit", and it predates the presets/axes model entirely — no `tlsChecks`,
 * no `tlsChecksFull`, no `allowList`, and no `tls` block in its uiSchema. Regenerating it is its own
 * job; this file covers the one surface under test.
 *
 * Do not edit the generated JSON by hand. Regenerate it:
 *   ./gradlew :hivemq-edge-module-opcua:test --tests '*TlsSchemaArtifactTest'
 */
export const MOCK_OPC_UA_TLS_SCHEMA: RJSFSchema = artifact.schema as RJSFSchema

export const MOCK_OPC_UA_TLS_UI_SCHEMA: UiSchema = artifact.uiSchema as UiSchema
