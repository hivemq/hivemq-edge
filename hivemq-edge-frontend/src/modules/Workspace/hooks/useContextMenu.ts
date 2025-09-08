import type { MouseEvent } from 'react'
import { useEffect } from 'react'
import { useKeyPress } from '@xyflow/react'
import { useNavigate } from 'react-router-dom'

export const useContextMenu = (id: string, selected: boolean, route: string) => {
  const navigate = useNavigate()
  // TODO[NVL] keyboard unsupported until accessibility can be fixed (see #18293)
  const spacePressed = useKeyPress(['Meta+Enter', 'Shift+Enter'], {
    target: document.querySelector<HTMLElement>(`[data-id="${id}"]`),
  })

  useEffect(() => {
    if (selected && spacePressed) {
      navigate(route)
    }
  }, [id, navigate, route, selected, spacePressed])

  const onContextMenu = (event: MouseEvent<HTMLElement>) => {
    if (selected) {
      navigate(route)
      event.preventDefault()
    }
  }

  return { onContextMenu }
}
