export type BridgeSubscription = {
  topics: string[]
  topicFilters: string[]
  mappings: string[][]
}

export interface ChordMatrixData {
  matrix: number[][]
  keys: string[]
}

export type TreeNode = {
  type: 'node'
  value: number
  name: string
  children: Tree[]
}

export type TreeLeaf = {
  type: 'leaf'
  name: string
  value: number
  links: string[]
}

export type Tree = TreeNode | TreeLeaf
