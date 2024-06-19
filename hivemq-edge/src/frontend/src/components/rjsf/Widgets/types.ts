import type { ObjectNode } from '@/api/__generated__'

export interface FlatObjectNode extends Omit<ObjectNode, 'children'> {
  breadcrumb?: string[]
}

export interface INode<M> {
  /** A non-negative integer that uniquely identifies the node */
  id: string
  /** Used to match on key press */
  name: string
  /** An array with the ids of the children nodes */
  children: string[]
  /** The parent of the node; null for the root node */
  parent: string | null
  /** Used to indicated whether a node is branch to be able load async data onExpand*/
  isBranch?: boolean
  /** User-defined metadata */
  metadata?: M
}
