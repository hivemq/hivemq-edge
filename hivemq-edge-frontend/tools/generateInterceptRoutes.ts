/**
 * Generates cypress/support/__generated__/apiRoutes.ts and apiRoutes.meta.json
 * from src/api/__generated__/services/*.ts
 *
 * Run directly: node --experimental-strip-types tools/generateInterceptRoutes.ts
 * Run via npm:  pnpm dev:openAPI  (integrated into the OpenAPI codegen pipeline)
 *
 * @see {@link https://linear.app/hivemq/issue/EDG-73}
 */

import { readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const SERVICES_DIR = join(ROOT, 'src/api/__generated__/services')
const OUTPUT_FILE = join(ROOT, 'cypress/support/__generated__/apiRoutes.ts')
const META_FILE = join(ROOT, 'cypress/support/__generated__/apiRoutes.meta.json')

// ─── Types ────────────────────────────────────────────────────────────────────

interface RouteEntry {
  methodName: string
  httpMethod: string
  url: string
  returnType: string
  hasPathParams: boolean
}

interface ServiceEntry {
  className: string
  namespace: string
  routes: RouteEntry[]
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Derives the registry namespace from a service class name.
 * Strips the trailing 'Service' suffix, then lowercases the first character.
 * Examples:
 *   AuthenticationService      → authentication
 *   BridgesService             → bridges
 *   DataHubSchemasService      → dataHubSchemas
 *   GatewayEndpointService     → gatewayEndpoint
 *   AuthenticationEndpointService → authenticationEndpoint
 */
function deriveNamespace(className: string): string {
  const base = className.replace(/Service$/, '')
  return base.charAt(0).toLowerCase() + base.slice(1)
}

/** Normalizes return types: 'any' becomes 'void' (no useful shape to enforce). */
function normalizeReturnType(raw: string): string {
  return raw.trim() === 'any' ? 'void' : raw.trim()
}

/**
 * Extracts all PascalCase type names from a return type string.
 * Used to build the import list.
 * E.g. "Array<Instruction>" → ["Instruction"]
 *      "BridgeList"         → ["BridgeList"]
 *      "void"               → []
 */
function extractTypeNames(returnType: string): string[] {
  const GENERIC_WRAPPERS = new Set(['Array', 'Promise', 'Record', 'Partial', 'Required', 'Readonly'])
  return [...returnType.matchAll(/\b([A-Z][A-Za-z0-9]*)\b/g)].map((m) => m[1]).filter((n) => !GENERIC_WRAPPERS.has(n))
}

/**
 * Extracts the type argument of CancelablePromise<T> from a method block,
 * correctly handling one level of nested generics (e.g. Array<Instruction>).
 *
 * Uses angle-bracket depth counting rather than a regex so that
 * CancelablePromise<Array<Instruction>> is handled correctly.
 */
function extractReturnType(block: string): string | null {
  const MARKER = '): CancelablePromise<'
  const markerPos = block.indexOf(MARKER)
  if (markerPos === -1) return null

  let i = markerPos + MARKER.length
  let depth = 1
  while (i < block.length && depth > 0) {
    if (block[i] === '<') depth++
    else if (block[i] === '>') depth--
    i++
  }
  return block.slice(markerPos + MARKER.length, i - 1).trim()
}

/**
 * Extracts the HTTP method and URL from `this.httpRequest.request({...})`.
 * Uses brace counting to find the options object boundary.
 */
function extractRequestCall(block: string): { method: string; url: string } | null {
  const MARKER = 'this.httpRequest.request({'
  const markerPos = block.indexOf(MARKER)
  if (markerPos === -1) return null

  let i = markerPos + MARKER.length
  let depth = 1
  while (i < block.length && depth > 0) {
    if (block[i] === '{') depth++
    else if (block[i] === '}') depth--
    i++
  }
  const optionsBlock = block.slice(markerPos + MARKER.length, i - 1)

  const methodMatch = optionsBlock.match(/\bmethod:\s*'([A-Z]+)'/)
  const urlMatch = optionsBlock.match(/\burl:\s*'([^']+)'/)
  if (!methodMatch || !urlMatch) return null
  return { method: methodMatch[1], url: urlMatch[1] }
}

// ─── Parser ───────────────────────────────────────────────────────────────────

function parseServiceFile(filePath: string): ServiceEntry | null {
  const source = readFileSync(filePath, 'utf-8')

  const classMatch = source.match(/export class (\w+)/)
  if (!classMatch) return null
  const className = classMatch[1]

  const routes: RouteEntry[] = []

  // Match each public method declaration at class-member indentation (4 spaces)
  const memberRe = /^    public (\w+)\(/gm
  let methodMatch: RegExpExecArray | null

  while ((methodMatch = memberRe.exec(source)) !== null) {
    const methodName = methodMatch[1]
    if (methodName === 'constructor') continue

    // Find the opening { of this method's body (first { after the method start)
    const bodyBracePos = source.indexOf('{', methodMatch.index)
    if (bodyBracePos === -1) continue

    // Extract the full method block by counting braces
    let depth = 1
    let i = bodyBracePos + 1
    while (i < source.length && depth > 0) {
      if (source[i] === '{') depth++
      else if (source[i] === '}') depth--
      i++
    }
    const block = source.slice(methodMatch.index, i)

    const returnType = extractReturnType(block)
    if (!returnType) continue

    const requestCall = extractRequestCall(block)
    if (!requestCall) continue
    if (!requestCall.url.startsWith('/api/')) continue

    routes.push({
      methodName,
      httpMethod: requestCall.method,
      url: requestCall.url,
      returnType: normalizeReturnType(returnType),
      hasPathParams: requestCall.url.includes('{'),
    })
  }

  if (routes.length === 0) return null
  return { className, namespace: deriveNamespace(className), routes }
}

// ─── Generator ────────────────────────────────────────────────────────────────

function routeCall(r: RouteEntry): string {
  return r.hasPathParams
    ? `routeWithParams<${r.returnType}>()('${r.httpMethod}', '${r.url}')`
    : `route<${r.returnType}>('${r.httpMethod}', '${r.url}')`
}

function generateOutput(services: ServiceEntry[]): string {
  // Collect unique model type names for the import block
  const typeNames = new Set<string>()
  for (const svc of services) {
    for (const r of svc.routes) {
      for (const name of extractTypeNames(r.returnType)) {
        typeNames.add(name)
      }
    }
  }
  const importList = [...typeNames].sort().join(',\n  ')

  const namespaceBlocks = services
    .map((svc) => {
      const routeLines = svc.routes
        .map((r) =>
          [
            `    /**`,
            `     * @see {@link ${svc.className}.${r.methodName}}`,
            `     */`,
            `    ${r.methodName}: ${routeCall(r)},`,
          ].join('\n')
        )
        .join('\n\n')

      return [`  /**`, `   * @see {@link ${svc.className}}`, `   */`, `  ${svc.namespace}: {`, routeLines, `  },`].join(
        '\n'
      )
    })
    .join('\n\n')

  // The regex literals in the factory bodies need escaped backslashes
  return `/**
 * Type-safe Cypress intercept route registry.
 *
 * GENERATED FILE — do not edit manually.
 * Regenerate with: pnpm dev:openAPI
 *
 * @see {@link https://linear.app/hivemq/issue/EDG-73} for implementation task
 *
 * ## Usage
 *
 * \`\`\`typescript
 * // ✅ TypeScript validates the response shape against the OpenAPI model
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake_token' })
 *
 * // ✅ Status-only shorthand works for any route
 * cy.interceptApi(API_ROUTES.bridges.getBridges, { statusCode: 404 })
 *
 * // ✅ Parametric route with ** wildcard (matches any ID)
 * cy.interceptApi(API_ROUTES.bridges.getBridgeByName, { ...bridgeMock })
 *
 * // ✅ Parametric route with specific ID
 * cy.interceptApi(API_ROUTES.bridges.getBridgeByName.withParams({ bridgeId: 'my-bridge' }), { ...bridgeMock })
 *
 * // ❌ TypeScript error — wrong shape for the route
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { items: [] })
 * \`\`\`
 */

import type {
  ${importList},
} from '@/api/__generated__'

// ─── Core types ───────────────────────────────────────────────────────────────

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

/**
 * A route descriptor carrying the response type as a phantom type parameter.
 * \`T\` is never stored at runtime — it only exists for TypeScript inference.
 */
export type Route<T> = {
  readonly method: HttpMethod
  readonly url: string
  /** Phantom type — undefined at runtime, inferred by TypeScript. Do not access. */
  readonly _responseType: T
}

type ExtractUrlParams<Template extends string> = Template extends \`\${string}{\${infer Param}}\${infer Rest}\`
  ? Param | ExtractUrlParams<Rest>
  : never

type UrlParams<Template extends string> = { [K in ExtractUrlParams<Template>]: string }

/**
 * A ParametricRoute extends Route<T> (usable as-is with ** glob wildcard) and
 * adds withParams() to resolve specific path parameter values into an exact URL.
 *
 * TypeScript infers the URL template as a literal type and enforces the correct
 * parameter names in withParams().
 */
export type ParametricRoute<T, Template extends string> = Route<T> & {
  readonly urlTemplate: Template
  /**
   * Resolve the URL template with specific path parameter values.
   * Returns a plain Route<T> with an exact URL (no wildcards).
   *
   * @example
   * API_ROUTES.bridges.getBridgeByName.withParams({ bridgeId: 'my-bridge' })
   * // → Route<Bridge> with url '/api/v1/management/bridges/my-bridge'
   */
  withParams(params: UrlParams<Template>): Route<T>
}

// ─── Factories ────────────────────────────────────────────────────────────────

/** Factory for routes with no path parameters. */
export const route = <T>(method: HttpMethod, url: string): Route<T> => ({
  method,
  url,
  _responseType: undefined as unknown as T,
})

/**
 * Curried factory for routes with path parameters.
 *
 * TypeScript cannot partially infer type arguments in a single generic call, so
 * this factory uses currying: specify T in the outer call, let TypeScript infer
 * Template from the URL string literal in the inner call.
 *
 * The default url replaces each {param} placeholder with ** (matches any value).
 *
 * @example
 * routeWithParams<Bridge>()('GET', '/api/v1/management/bridges/{bridgeId}')
 * // T = Bridge, Template = '/api/v1/management/bridges/{bridgeId}'
 * // .withParams({ bridgeId }) is enforced; .withParams({ id }) is a TS error
 */
export const routeWithParams =
  <T>() =>
  <Template extends string>(method: HttpMethod, urlTemplate: Template): ParametricRoute<T, Template> => {
    const defaultUrl = (urlTemplate as string).replace(/\\{[^}]+}/g, '**')
    return {
      method,
      url: defaultUrl,
      urlTemplate,
      _responseType: undefined as unknown as T,
      withParams(params: UrlParams<Template>): Route<T> {
        const resolvedUrl = (urlTemplate as string).replace(
          /\\{([^}]+)}/g,
          (_, name) => (params as Record<string, string>)[name] ?? '**'
        )
        return { method, url: resolvedUrl, _responseType: undefined as unknown as T }
      },
    }
  }

// ─── Registry ─────────────────────────────────────────────────────────────────

/**
 * Registry of all typed API routes, namespaced to mirror the HiveMqClient service structure.
 *
 * The namespace matches how the app accesses services in production:
 * - \`appClient.authentication.authenticate()\` → \`API_ROUTES.authenticationEndpoint.authenticate\`
 * - \`appClient.bridges.getBridges()\`           → \`API_ROUTES.bridges.getBridges\`
 *
 * Go-to-definition on any route opens its registry entry with a @see JSDoc link to the
 * service class and method that owns it.
 */
export const API_ROUTES = {
${namespaceBlocks}
} as const
`
}

/**
 * Generates the reverse-lookup map consumed by the no-bare-cy-intercept ESLint rule.
 *
 * Maps "METHOD /url" → "API_ROUTES.namespace.method" so the rule can suggest the
 * exact replacement when it detects a bare cy.intercept() call.
 *
 * Parametric URLs use ** wildcards (matching the runtime url stored in Route.url):
 *   "GET /api/v1/management/bridges/{bridgeId}" → "GET /api/v1/management/bridges/**"
 */
function generateMeta(services: ServiceEntry[]): string {
  const map: Record<string, string> = {}
  for (const svc of services) {
    for (const r of svc.routes) {
      const wildcardUrl = r.url.replace(/\{[^}]+}/g, '**')
      map[`${r.httpMethod} ${wildcardUrl}`] = `API_ROUTES.${svc.namespace}.${r.methodName}`
    }
  }
  return JSON.stringify(map, null, 2) + '\n'
}

// ─── Main ──────────────────────────────────────────────────────────────────────

const serviceFiles = readdirSync(SERVICES_DIR)
  .filter((f) => f.endsWith('.ts'))
  .map((f) => join(SERVICES_DIR, f))

const services = serviceFiles
  .map(parseServiceFile)
  .filter((s): s is ServiceEntry => s !== null)
  .sort((a, b) => a.namespace.localeCompare(b.namespace))

// Check for namespace collisions
const seen = new Map<string, string>()
for (const svc of services) {
  if (seen.has(svc.namespace)) {
    console.error(
      `ERROR: Namespace collision — "${svc.namespace}" produced by both "${seen.get(svc.namespace)}" and "${svc.className}"`
    )
    process.exit(1)
  }
  seen.set(svc.namespace, svc.className)
}

writeFileSync(OUTPUT_FILE, generateOutput(services), 'utf-8')
writeFileSync(META_FILE, generateMeta(services), 'utf-8')

const totalRoutes = services.reduce((sum, s) => sum + s.routes.length, 0)
console.log(`Generated apiRoutes.ts + apiRoutes.meta.json — ${services.length} services, ${totalRoutes} routes`)
