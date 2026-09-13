import type { Edge, Node } from '@xyflow/react'

import { IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'

/**
 * Reconciling the workspace against the server.
 *
 * The workspace graph is a persisted, append-only copy of server entities: `onAddNodes` skips ids it
 * already holds, and nothing removes nodes for entities the server no longer returns. So the canvas
 * drifts away from the server and stays drifted, across reloads, because it is written to localStorage.
 *
 * Reconciling repairs that drift in place. Server truth (`node.data`) is replaced from a freshly built
 * node; user truth (`position`, `parentId`, grouping, selection) is kept from the node already on the
 * canvas. Both live on the same node object in separate fields, so the split is a field-level copy.
 *
 * This module is deliberately pure — old graph plus freshly built graph in, new graph out — so the
 * decisive logic is unit-testable without React, a browser, or a mocked API.
 */

/**
 * Node types that stand for an entity the server owns, and may therefore be removed by a reconcile
 * when that entity is gone.
 *
 * CLUSTER_NODE (a user-created group) is deliberately absent: a group has no server counterpart, so
 * "the server did not return it" says nothing about whether it should exist. Removing groups here
 * would delete the user's own work. Groups are out of scope for this pass — see the caveat below.
 *
 * EDGE_NODE and PULSE_NODE are absent for a different reason: they are singletons that the graph
 * builder always emits, so they are never stale.
 */
const SERVER_OWNED_NODE_TYPES: ReadonlySet<string> = new Set<string>([
  NodeTypes.ADAPTER_NODE,
  NodeTypes.BRIDGE_NODE,
  NodeTypes.LISTENER_NODE,
  NodeTypes.HOST_NODE,
  NodeTypes.DEVICE_NODE,
  NodeTypes.COMBINER_NODE,
  NodeTypes.ASSETS_NODE,
])

/**
 * User-owned fields, carried across from the node already on the canvas onto its freshly built
 * replacement. Everything else on the node — `data` above all — comes from the server.
 *
 * `position` is the one users notice, but `parentId` and `expandParent` matter just as much: a node
 * inside a group stores a group-*relative* position, so carrying the position without the parentage
 * would fling grouped nodes across the canvas.
 */
const carryUserState = (incoming: Node, existing: Node): Node => ({
  ...incoming,
  position: existing.position,
  ...(existing.parentId !== undefined ? { parentId: existing.parentId } : {}),
  ...(existing.expandParent !== undefined ? { expandParent: existing.expandParent } : {}),
  ...(existing.selected !== undefined ? { selected: existing.selected } : {}),
  ...(existing.hidden !== undefined ? { hidden: existing.hidden } : {}),
  ...(existing.width !== undefined ? { width: existing.width } : {}),
  ...(existing.height !== undefined ? { height: existing.height } : {}),
})

export const isServerOwnedNode = (node: Node) => SERVER_OWNED_NODE_TYPES.has(node.type as string)

/**
 * An edge is kept only while both of its endpoints still exist.
 *
 * This is what stops a reconcile from trading ghost nodes for dangling connectors. One server entity
 * is several things on the canvas — an adapter is an adapter node, a device node and a connector edge;
 * a bridge is a bridge node, a host node and a connector edge — so removing an entity means removing
 * its whole cluster. Rather than teach the reconcile every cluster shape, the nodes go first and the
 * edges follow their endpoints, which is the same answer for every shape including ones added later.
 */
export const pruneDanglingEdges = (edges: Edge[], nodes: Node[]): Edge[] => {
  const nodeIds = new Set(nodes.map((node) => node.id))
  return edges.filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target))
}

export interface ReconcileResult {
  nodes: Node[]
  edges: Edge[]
  /** What changed, for the toast that reports the outcome to the user. */
  summary: {
    updated: number
    added: number
    removed: number
  }
}

/**
 * Merge a freshly built graph into the one currently on the canvas.
 *
 * - a node in both  -> updated: server `data` from `incoming`, user state from `existing`
 * - a node only in `incoming` -> added (it appeared server-side since the canvas was built)
 * - a server-owned node only in `existing` -> removed (the ghost this whole exercise is about)
 * - a user-owned node only in `existing` -> kept (groups; the server has no opinion on them)
 *
 * Edges are taken from the incoming graph, plus any existing edge the builder does not re-emit whose
 * endpoints both survive (group connectors), and then pruned of anything dangling.
 */
export const reconcileWorkspace = (
  existingNodes: Node[],
  existingEdges: Edge[],
  incomingNodes: Node[],
  incomingEdges: Edge[]
): ReconcileResult => {
  const incomingById = new Map(incomingNodes.map((node) => [node.id, node]))
  const existingById = new Map(existingNodes.map((node) => [node.id, node]))

  let updated = 0
  let removed = 0

  // Existing nodes, in their current order — the canvas should not reshuffle itself under the user.
  const keptNodes = existingNodes.reduce<Node[]>((accumulator, existing) => {
    const incoming = incomingById.get(existing.id)

    if (incoming) {
      updated += 1
      accumulator.push(carryUserState(incoming, existing))
      return accumulator
    }

    // Not returned by the server. Only server-owned nodes are ghosts; a group is the user's own.
    if (isServerOwnedNode(existing)) {
      removed += 1
      return accumulator
    }

    accumulator.push(existing)
    return accumulator
  }, [])

  // Anything the server has that the canvas does not — appended, so new entities land predictably.
  const addedNodes = incomingNodes.filter((node) => !existingById.has(node.id))

  const nodes = [...keptNodes, ...addedNodes]

  // Edges the builder re-emits are authoritative (they carry status colouring and animation). Existing
  // edges it does not emit are the user's own — group connectors — and are kept if they still connect.
  const incomingEdgeIds = new Set(incomingEdges.map((edge) => edge.id))
  const survivingExistingEdges = existingEdges.filter((edge) => !incomingEdgeIds.has(edge.id))

  const edges = pruneDanglingEdges([...incomingEdges, ...survivingExistingEdges], nodes)

  return {
    nodes,
    edges,
    summary: { updated, added: addedNodes.length, removed },
  }
}

/**
 * The id shapes the graph builder uses, kept here so the reconcile and its tests describe entity
 * identity in one place rather than re-deriving string formats.
 *
 * They are not uniform, which is the trap: adapters, bridges, listeners and devices carry a type
 * prefix, while a combiner node uses the bare entity id. Matching an entity to the wrong shape makes
 * the reconcile treat it as new rather than as an update, and the canvas grows a duplicate.
 */
export const nodeIdFor = {
  adapter: (adapterId: string) => `${IdStubs.ADAPTER_NODE}@${adapterId}`,
  adapterDevice: (adapterId: string) => `${IdStubs.DEVICE_NODE}@${IdStubs.ADAPTER_NODE}@${adapterId}`,
  bridge: (bridgeId: string) => `${IdStubs.BRIDGE_NODE}@${bridgeId}`,
  bridgeHost: (bridgeId: string) => `${IdStubs.HOST_NODE}@${bridgeId}`,
  listener: (listenerName: string) => `${IdStubs.LISTENER_NODE}@${listenerName}`,
  combiner: (combinerId: string) => combinerId,
} as const
