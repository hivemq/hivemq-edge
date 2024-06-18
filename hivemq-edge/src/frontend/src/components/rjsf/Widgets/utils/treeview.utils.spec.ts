import { describe, expect } from 'vitest'
import { INode } from 'react-accessible-treeview'

import { ObjectNode } from '@/api/__generated__'
import { convertEdgeNode, getAdapterTreeView } from '@/components/rjsf/Widgets/utils/treeview.utils.ts'
import { FlatObjectNode } from '@/components/rjsf/Widgets/types.ts'

interface TestEachSuite {
  parent: INode<FlatObjectNode>
  node: ObjectNode
  expected: INode<FlatObjectNode>[]
}

describe('convertEdgeNode', () => {
  const root: INode<FlatObjectNode> = {
    id: 'root',
    name: 'root node',
    parent: null,
    children: [],
  }

  test.each<TestEachSuite>([
    {
      node: {
        id: 'node1',
        name: 'node1',
        description: 'the first node',
        nodeType: ObjectNode.nodeType.OBJECT,
        selectable: false,
      },
      parent: root,
      expected: [
        {
          children: [],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node1',
            name: 'node1',
            description: 'the first node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          name: 'node1',
          parent: 'root',
        },
      ],
    },
    {
      node: {
        id: 'node1',
        name: 'node1',
        description: 'the first node',
        nodeType: ObjectNode.nodeType.OBJECT,
        selectable: false,
        children: [
          {
            id: 'node2',
            name: 'node2',
            description: 'the second node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
            children: [],
          },
          {
            id: 'node3',
            name: 'node3',
            description: 'the third node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
            children: [],
          },
        ],
      },
      parent: root,
      expected: [
        {
          children: [expect.stringContaining(''), expect.stringContaining('')],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node1',
            name: 'node1',
            description: 'the first node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          name: 'node1',
          parent: 'root',
        },
        {
          children: [],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node2',
            name: 'node2',
            description: 'the second node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          name: 'node2',
          parent: expect.stringContaining(''),
        },
        {
          children: [],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node3',
            name: 'node3',
            description: 'the third node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          name: 'node3',
          parent: expect.stringContaining(''),
        },
      ],
    },
    {
      node: {
        id: 'node1',
        name: 'node1',
        description: 'the first node',
        nodeType: ObjectNode.nodeType.OBJECT,
        selectable: false,
        children: [
          {
            id: 'node2',
            name: 'node2',
            description: 'the second node',
            nodeType: ObjectNode.nodeType.VALUE,
            selectable: false,
            children: [
              {
                id: 'node3',
                name: 'node3',
                description: 'the third node',
                nodeType: ObjectNode.nodeType.FOLDER,
                selectable: false,
              },
            ],
          },
        ],
      },
      parent: root,
      expected: [
        {
          children: [expect.stringContaining('')],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node1',
            name: 'node1',
            description: 'the first node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          name: 'node1',
          parent: expect.stringContaining(''),
        },
        {
          children: [expect.stringContaining('')],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node2',
            name: 'node2',
            description: 'the second node',
            nodeType: ObjectNode.nodeType.VALUE,
            selectable: false,
          },
          name: 'node2',
          parent: expect.stringContaining(''),
        },
        {
          children: [],
          id: expect.stringContaining(''),
          isBranch: false,
          metadata: {
            id: 'node3',
            name: 'node3',
            description: 'the third node',
            nodeType: ObjectNode.nodeType.FOLDER,
            selectable: false,
          },
          name: 'node3',
          parent: expect.stringContaining(''),
        },
      ],
    },
  ])('should convert the tree', ({ parent, node, expected }) => {
    expect(convertEdgeNode(parent, node)).toStrictEqual(expected)
  })
})

describe('getAdapterTreeView', () => {
  it('should convert an empty tree', () => {
    expect(getAdapterTreeView({})).toStrictEqual([])
    expect(getAdapterTreeView({ items: undefined })).toStrictEqual([])
    expect(getAdapterTreeView({ items: [] })).toStrictEqual([])
  })

  it('should convert a  tree', () => {
    expect(
      getAdapterTreeView({
        items: [
          {
            id: 'node0',
            name: 'node0',
            description: 'the root',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
          },
          {
            id: 'node1',
            name: 'node1',
            description: 'the first node',
            nodeType: ObjectNode.nodeType.OBJECT,
            selectable: false,
            children: [
              {
                id: 'node2',
                name: 'node2',
                description: 'the second node',
                nodeType: ObjectNode.nodeType.VALUE,
                selectable: false,
                children: [
                  {
                    id: 'node3',
                    name: 'node3',
                    description: 'the third node',
                    nodeType: ObjectNode.nodeType.FOLDER,
                    selectable: false,
                  },
                ],
              },
            ],
          },
        ],
      })
    ).toStrictEqual([
      {
        children: [],
        id: 'node0',
        isBranch: false,
        metadata: {
          description: 'the root',
          nodeType: 'OBJECT',
          selectable: false,
        },
        name: 'node0',
        parent: null,
      },
      {
        children: ['node2'],
        id: 'node1',
        isBranch: false,
        metadata: {
          description: 'the first node',
          nodeType: 'OBJECT',
          selectable: false,
        },
        name: 'node1',
        parent: null,
      },
      {
        children: ['node3'],
        id: 'node2',
        isBranch: false,
        metadata: {
          description: 'the second node',
          nodeType: 'VALUE',
          selectable: false,
        },
        name: 'node2',
        parent: 'node1',
      },
      {
        children: [],
        id: 'node3',
        isBranch: false,
        metadata: {
          description: 'the third node',
          nodeType: 'FOLDER',
          selectable: false,
        },
        name: 'node3',
        parent: 'node2',
      },
    ])
  })
})
