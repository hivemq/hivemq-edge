import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { Node } from '@xyflow/react'
import { useStore, useReactFlow } from '@xyflow/react'

import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import type { DeviceMetadata } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { WorkspaceNavigationCommand } from '@/modules/Workspace/types.ts'
import { addSelectedNodesState } from '@/modules/Workspace/utils/react-flow.utils.ts'

const SelectionListener = () => {
  const { state, pathname } = useLocation()
  const navigate = useNavigate()
  const addSelectedNodes = useStore(addSelectedNodesState)
  const { nodes } = useWorkspaceStore()
  const { fitView } = useReactFlow()

  useEffect(() => {
    const { adapterId, type, command } = state?.selectedAdapter || {}
    if (!adapterId || !type) return

    const focusOnNodes = (nodesIds: string[]) => {
      addSelectedNodes(nodesIds)
      fitView({ nodes: nodesIds.map((e) => ({ id: e })), duration: 750 })
      navigate(pathname, { state: null, replace: true })
    }

    switch (command) {
      case WorkspaceNavigationCommand.TOPIC_FILTERS: {
        // Topic filters are on the Edge node
        const edgeFound = nodes.find((e) => e.type === NodeTypes.EDGE_NODE)
        if (edgeFound) focusOnNodes([edgeFound.id])

        return
      }
      case WorkspaceNavigationCommand.TAGS: {
        // Tags are on the Device node connected to the adapter
        const found = nodes.find((e) => adapterId === e.data.id)
        if (found?.type === NodeTypes.ADAPTER_NODE) {
          const device = nodes.find(
            (e) => e.type === NodeTypes.DEVICE_NODE && adapterId === (e as Node<DeviceMetadata>).data.sourceAdapterId
          )
          if (device) focusOnNodes([device.id])
        }
        return
      }
      case WorkspaceNavigationCommand.MAPPINGS: {
        // N and S mappings are on the adapter node
        const found = nodes.find((e) => adapterId === e.data.id)
        if (found) {
          focusOnNodes([found.id])
        }
        return
      }
      case WorkspaceNavigationCommand.ASSET_MAPPER: {
        const found = nodes.filter(
          (e) => e.type === NodeTypes.PULSE_NODE || (e.type === NodeTypes.ASSETS_NODE && e.data.id === adapterId)
        )
        if (found.length > 0) {
          focusOnNodes(found.map((e) => e.id))
        }
        return
      }
      default: {
        // For all other navigation commands, just select adapter + device nodes
        const found = nodes.find((e) => adapterId === e.data.id)
        if (found) {
          const nodesToAdd: string[] = []

          nodesToAdd.push(found.id)

          if (found.type === NodeTypes.ADAPTER_NODE) {
            const device = nodes.find(
              (e) => e.type === NodeTypes.DEVICE_NODE && adapterId === (e as Node<DeviceMetadata>).data.sourceAdapterId
            )
            if (device) nodesToAdd.push(device.id)
          }

          focusOnNodes(nodesToAdd)
        }
        return
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname, state?.selectedAdapter])

  return null
}

export default SelectionListener
