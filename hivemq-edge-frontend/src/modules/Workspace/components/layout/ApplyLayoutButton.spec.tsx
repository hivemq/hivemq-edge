/**
 * Tests for ApplyLayoutButton Component
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ApplyLayoutButton from './ApplyLayoutButton.tsx'

// Mock config
vi.mock('@/config', () => ({
  default: {
    features: {
      WORKSPACE_AUTO_LAYOUT: true,
    },
  },
}))

// Mock useLayoutEngine
const mockApplyLayout = vi.fn()
const mockCurrentAlgorithmInstance = {
  name: 'Vertical Tree Layout',
  type: 'DAGRE_TB',
}

vi.mock('../../hooks/useLayoutEngine', () => ({
  useLayoutEngine: () => ({
    applyLayout: mockApplyLayout,
    currentAlgorithmInstance: mockCurrentAlgorithmInstance,
  }),
}))

describe('ApplyLayoutButton', () => {
  it('should render when feature flag is enabled', () => {
    render(<ApplyLayoutButton />)

    expect(screen.getByTestId('workspace-apply-layout')).toBeInTheDocument()
  })

  it('should call applyLayout when clicked', async () => {
    const user = userEvent.setup()
    mockApplyLayout.mockResolvedValue({ success: true, duration: 50, nodes: [] })

    render(<ApplyLayoutButton />)

    const button = screen.getByTestId('workspace-apply-layout')
    await user.click(button)

    expect(mockApplyLayout).toHaveBeenCalled()
  })

  it('should show loading state while applying layout', async () => {
    const user = userEvent.setup()
    mockApplyLayout.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)))

    render(<ApplyLayoutButton />)

    const button = screen.getByTestId('workspace-apply-layout')
    await user.click(button)

    // Button should be disabled while loading
    expect(button).toBeDisabled()
  })
})
