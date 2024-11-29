import { describe, expect } from 'vitest'

import { ObjectNode } from '@/api/__generated__'
import { convertEdgeNode, getAdapterTreeView } from '@/components/rjsf/Widgets/utils/treeview.utils.ts'
import { FlatObjectNode, INode } from '@/components/rjsf/Widgets/types.ts'

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
            breadcrumb: ['node1'],
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
            breadcrumb: ['node1'],
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
            breadcrumb: ['node1', 'node2'],
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
            breadcrumb: ['node1', 'node3'],
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
            breadcrumb: ['node1'],
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
            breadcrumb: ['node1', 'node2'],
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
            breadcrumb: ['node1', 'node2', 'node3'],
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
    expect(getAdapterTreeView({ items: [] })).toStrictEqual([])
  })

  it('should convert a tree', () => {
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
        id: expect.stringContaining(''),
        isBranch: false,
        metadata: {
          id: 'node0',
          name: 'node0',
          description: 'the root',
          nodeType: 'OBJECT',
          breadcrumb: ['node0'],
          selectable: false,
        },
        name: 'node0',
        parent: expect.stringContaining(''),
      },
      {
        children: [expect.stringContaining('')],
        id: expect.stringContaining(''),
        isBranch: false,
        metadata: {
          id: 'node1',
          name: 'node1',
          description: 'the first node',
          nodeType: 'OBJECT',
          breadcrumb: ['node1'],
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
          nodeType: 'VALUE',
          breadcrumb: ['node1', 'node2'],
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
          nodeType: 'FOLDER',
          breadcrumb: ['node1', 'node2', 'node3'],
          selectable: false,
        },
        name: 'node3',
        parent: expect.stringContaining(''),
      },
    ])
  })

  it('should add the root when asked to', () => {
    expect(
      getAdapterTreeView(
        {
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
              children: [],
            },
          ],
        },
        true
      )
    ).toStrictEqual([
      {
        children: [],
        id: expect.stringContaining(''),
        name: 'root-node',
        parent: null,
      },
      {
        children: [],
        id: expect.stringContaining(''),
        isBranch: false,
        metadata: {
          id: 'node0',
          name: 'node0',
          description: 'the root',
          breadcrumb: ['node0'],
          nodeType: 'OBJECT',
          selectable: false,
        },
        name: 'node0',
        parent: expect.stringContaining(''),
      },
      {
        children: [],
        id: expect.stringContaining(''),
        isBranch: false,
        metadata: {
          id: 'node1',
          name: 'node1',
          breadcrumb: ['node1'],
          description: 'the first node',
          nodeType: 'OBJECT',
          selectable: false,
        },
        name: 'node1',
        parent: expect.stringContaining(''),
      },
    ])
  })
})
