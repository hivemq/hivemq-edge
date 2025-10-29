/**
 * Test to verify localStorage persistence works correctly
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import useWorkspaceStore from './useWorkspaceStore'
import type { Node, Edge } from '@xyflow/react'

describe('useWorkspaceStore - localStorage persistence', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear()
  })

  it('should persist nodes and edges to localStorage', () => {
    const { result } = renderHook(() => useWorkspaceStore())

    const testNodes: Node[] = [
      { id: '1', position: { x: 0, y: 0 }, data: { test: 'node1' } },
      { id: '2', position: { x: 100, y: 100 }, data: { test: 'node2' } },
    ]

    const testEdges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

    // Add nodes and edges
    act(() => {
      result.current.onAddNodes(testNodes)
      result.current.onAddEdges(testEdges)
    })

    // Check localStorage
    const stored = localStorage.getItem('edge.workspace')
    expect(stored).toBeTruthy()

    const parsed = JSON.parse(stored!)
    expect(parsed.state.nodes).toHaveLength(2)
    expect(parsed.state.edges).toHaveLength(1)
    expect(parsed.state.nodes[0].id).toBe('1')
    expect(parsed.state.edges[0].id).toBe('e1-2')
  })

  it('should persist nodes and edges even when layout feature is disabled', () => {
    const { result } = renderHook(() => useWorkspaceStore())

    const testNodes: Node[] = [{ id: 'test-node', position: { x: 50, y: 50 }, data: {} }]

    act(() => {
      result.current.onAddNodes(testNodes)
    })

    const stored = localStorage.getItem('edge.workspace')
    const parsed = JSON.parse(stored!)

    // Nodes should always be persisted
    expect(parsed.state.nodes).toBeDefined()
    expect(parsed.state.nodes).toHaveLength(1)
  })

  it('should restore nodes and edges from localStorage on mount', () => {
    // Simulate existing data in localStorage
    const existingData = {
      state: {
        nodes: [{ id: 'restored', position: { x: 200, y: 200 }, data: { restored: true } }],
        edges: [{ id: 'e-restored', source: 'restored', target: 'restored' }],
      },
      version: 0,
    }

    localStorage.setItem('edge.workspace', JSON.stringify(existingData))

    // Create new hook instance (simulates page reload)
    const { result } = renderHook(() => useWorkspaceStore())

    // Should have restored data
    expect(result.current.nodes).toHaveLength(1)
    expect(result.current.nodes[0].id).toBe('restored')
    expect(result.current.edges).toHaveLength(1)
  })
})
