import type { FC } from 'react'
import { useCallback, useLayoutEffect, useMemo } from 'react'
import type { Connection, Edge, Node } from 'reactflow'
import ReactFlow, { addEdge, MarkerType, useEdgesState, useNodesState, useReactFlow } from 'reactflow'
import ELK from 'elkjs/lib/elk.bundled'
import type { ElkNode, LayoutOptions } from 'elkjs/lib/elk-api'

import { StateNode } from '@datahub/components/fsm/StateNode.tsx'
import { TransitionNode } from '@datahub/components/fsm/TransitionNode.tsx'
import type { FiniteStateMachine, FsmState } from '@datahub/types.ts'

import 'reactflow/dist/style.css'

const elk = new ELK()

const position = { x: 0, y: 0 }

// Elk has a *huge* amount of options to configure. To see everything you can tweak check out:
// - https://www.eclipse.org/elk/reference/algorithms.html
// - https://www.eclipse.org/elk/reference/options.html
const elkOptions: LayoutOptions = {
  'elk.algorithm': 'mrtree',
  'elk.spacing.nodeNode': '50',
  'elk.mrtree.edgeRoutingMode': 'MIDDLE_TO_MIDDLE',
  'elk.mrtree.searchOrder': 'BFS',
  // 'elk.layered.spacing.edgeEdgeBetweenLayers': '20',
  // 'elk.layered.spacing.nodeNodeBetweenLayers': '40',
  // 'elk.edgeRouting': 'ORTHOGONAL',
}

interface LayoutedElementsType {
  nodes: Node[]
  edges: Edge[]
}

const getLayoutedElements = async (
  nodes: Node[],
  edges: Edge[],
  options: LayoutOptions = {}
): Promise<LayoutedElementsType | undefined> => {
  const isHorizontal = options?.['elk.direction'] === 'RIGHT'
  const graph: ElkNode = {
    id: 'root',
    layoutOptions: options,
    children: nodes.map((node) => ({
      ...node,
      // Adjust the target and source handle positions based on the layout direction.
      targetPosition: isHorizontal ? 'left' : 'top',
      sourcePosition: isHorizontal ? 'right' : 'bottom',

      // Hardcode a width and height for elk to use when layouting.
      width: 150,
      height: 50,
    })),
    // @ts-ignore
    edges: edges,
  }

  try {
    const layoutedGraph = await elk.layout(graph)
    return {
      // @ts-ignore Not a great idea but ignoring casting for now
      nodes: layoutedGraph.children?.map((node) => ({
        ...node,
        // React Flow expects a position property on the node instead of `x` and `y` fields.
        position: { x: node.x, y: node.y },
      })),
      // @ts-ignore Not a great idea but ignoring casting for now
      edges: layoutedGraph.edges,
    }
  } catch (message) {
    console.error(message)
    return undefined
  }
}

export const ReactFlowRenderer: FC<FiniteStateMachine> = (props) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const { fitView } = useReactFlow()

  const { initialNodes, initialEdges } = useMemo(() => {
    const statedNodes: Node<FsmState>[] = props.states.map((state) => ({
      id: state.name,
      type: 'state',
      data: state,
      position,
    }))

    // Create transitions as node instead of edge to improve layout and interactivity
    const transitionNodes: Node[] = props.transitions.map((transition) => ({
      id: `${transition.event}-${transition.fromState}-${transition.toState}`,
      data: { label: transition.event },
      type: 'transition',
      position,
    }))

    const initialToTransitionEdges: Edge[] = props.transitions.map((transition) => ({
      id: `e1-${transition.event}-${transition.fromState}-${transition.toState}`,
      source: transition.fromState,
      target: `${transition.event}-${transition.fromState}-${transition.toState}`,
    }))

    const initialFromTransitionEdges: Edge[] = props.transitions.map((transition) => ({
      id: `e2-${transition.event}-${transition.toState}-${transition.fromState}`,
      source: `${transition.event}-${transition.fromState}-${transition.toState}`,
      target: transition.toState,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
      },
    }))

    const initialNodes: Node[] = [...statedNodes, ...transitionNodes]
    const initialEdges: Edge[] = [...initialToTransitionEdges, ...initialFromTransitionEdges]

    return { initialNodes, initialEdges }
  }, [props.states, props.transitions])

  const onConnect = useCallback((connection: Connection) => setEdges((eds) => addEdge(connection, eds)), [setEdges])
  const onLayout = useCallback(
    ({ useInitialNodes = false }) => {
      const opts = { ...elkOptions }
      const ns = useInitialNodes ? initialNodes : nodes
      const es = useInitialNodes ? initialEdges : edges

      getLayoutedElements(ns, es, opts).then((props) => {
        if (props) {
          const { nodes: layoutedNodes, edges: layoutedEdges } = props
          setNodes(layoutedNodes)
          setEdges(layoutedEdges)
          window.requestAnimationFrame(() => fitView())
        }
      })
    },
    [initialNodes, nodes, initialEdges, edges, setNodes, setEdges, fitView]
  )

  useLayoutEffect(() => {
    onLayout({ useInitialNodes: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const nodeTypes = useMemo(
    () => ({
      state: StateNode,
      transition: TransitionNode,
    }),
    []
  )

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      // edgeTypes={edgeTypes}
      onConnect={onConnect}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      fitView
    ></ReactFlow>
  )
}
