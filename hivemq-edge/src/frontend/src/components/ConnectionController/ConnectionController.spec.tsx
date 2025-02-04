import { describe, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'

import '@/config/i18n.config.ts'

import { StatusTransitionResult } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import ConnectionController from './ConnectionController.tsx'

const { mutateAdapterAsync, mutateBridgeAsync, successToast, errorToast } = vi.hoisted(() => {
  return {
    mutateAdapterAsync: vi.fn().mockResolvedValue({}),
    mutateBridgeAsync: vi.fn().mockResolvedValue({}),
    successToast: vi.fn(),
    errorToast: vi.fn(),
  }
})

vi.mock('@/api/hooks/useProtocolAdapters/useSetConnectionStatus.ts', () => {
  return { useSetConnectionStatus: () => ({ mutateAsync: mutateAdapterAsync }) }
})

vi.mock('@/api/hooks/useGetBridges/useSetConnectionStatus.ts', () => {
  return { useSetConnectionStatus: () => ({ mutateAsync: mutateBridgeAsync }) }
})

vi.mock('@/hooks/useEdgeToast/useEdgeToast.tsx', () => {
  return { useEdgeToast: () => ({ successToast: successToast, errorToast: errorToast }) }
})

const mockID = 'my-id'
const mockStatusTransitionResult: StatusTransitionResult = {
  type: 'adapter',
  identifier: mockID,
  status: StatusTransitionResult.status.PENDING,
  callbackTimeoutMillis: 2000,
}

describe('ConnectionController', () => {
  it('should trigger a correct mutation on a state change', async () => {
    mutateAdapterAsync.mockResolvedValue(mockStatusTransitionResult)
    render(<ConnectionController type={DeviceTypes.ADAPTER} id={mockID} />, { wrapper })

    expect(screen.getByTestId('device-action-start')).toBeDefined()

    await waitFor(() => {
      screen.getByTestId('device-action-start').click()

      expect(mutateAdapterAsync).toHaveBeenCalledWith({
        adapterId: mockID,
        requestBody: {
          command: 'START',
        },
      })

      expect(successToast).toHaveBeenCalledWith({
        description: "We've successfully started the adapter. It might take a moment to update the status.",
        title: 'Connection updating',
      })
    })
  })

  it('should trigger an error when mutation not resolved', async () => {
    const err = [
      {
        fieldName: 'command',
        title: 'Invalid user supplied data',
      },
    ]

    mutateBridgeAsync.mockRejectedValue({
      errors: err,
    })
    render(<ConnectionController type={DeviceTypes.BRIDGE} id={mockID} />, { wrapper })

    screen.getByTestId('device-action-start').click()
    await waitFor(() => {
      expect(mutateBridgeAsync).toHaveBeenCalledWith({
        name: mockID,
        requestBody: {
          command: 'START',
        },
      })

      expect(errorToast).toHaveBeenCalledWith(
        {
          title: 'Connection updating',
          description: 'There was a problem trying to reconnect the bridge',
        },
        {
          errors: err,
        }
      )
    })
  })
})
