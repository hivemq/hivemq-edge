import { INode } from 'react-accessible-treeview'
import { v4 as uuidv4 } from 'uuid'

import { type ObjectNode, ValuesTree } from '@/api/__generated__'
import { FlatObjectNode } from '@/components/rjsf/Widgets/types.ts'

export const convertEdgeNode = (parent: INode<FlatObjectNode>, node: ObjectNode): INode<FlatObjectNode>[] => {
  const { children, ...flatNode } = node
  const nodeId = uuidv4()

  const newNode: INode<FlatObjectNode> = {
    id: nodeId,
    name: flatNode.name as string,
    parent: parent.id,
    isBranch: false,
    metadata: flatNode,
    children: [],
  }

  const allChildren: INode<FlatObjectNode>[] = []
  for (const child of children || []) {
    const edgeNodes = convertEdgeNode(newNode, child)
    allChildren.push(...edgeNodes)
    newNode.children.push(edgeNodes[0].id)
  }

  return [newNode, ...allChildren]
}

export const getAdapterTreeView = (tree: ValuesTree, withRoot = false): INode<FlatObjectNode>[] => {
  if (!tree.items) return []

  const rootId = uuidv4()
  const root: INode<FlatObjectNode> = {
    id: rootId,
    name: 'root-node',
    children: [],
    parent: null,
  }

  const allNodes: INode<FlatObjectNode>[] = withRoot ? [root] : []
  for (const child of tree.items) {
    const edgeNodes = convertEdgeNode(root, child)
    allNodes.push(...edgeNodes)
  }

  return allNodes
}
