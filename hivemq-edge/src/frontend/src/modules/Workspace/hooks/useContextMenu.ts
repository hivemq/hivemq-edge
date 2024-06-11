import { MouseEvent, useEffect } from 'react'
import { useKeyPress } from 'reactflow'
import { useNavigate } from 'react-router-dom'

export const useContextMenu = (id: string, selected: boolean, route: string) => {
  const navigate = useNavigate()
  // TODO[NVL] keyboard unsupported until accessibility can be fixed (see #18293)
  const spacePressed = useKeyPress(['Meta+Enter', 'Shift+Enter'], {
    target: document.querySelector<HTMLElement>(`[data-id="${id}"]`),
  })

  useEffect(() => {
    if (selected && spacePressed) {
      navigate(`${route}/${id}`)
    }
  }, [id, navigate, route, selected, spacePressed])

  const onContextMenu = (event: MouseEvent<HTMLElement>) => {
    if (selected) {
      navigate(`${route}/${id}`)
      event.preventDefault()
    }
  }

  return { onContextMenu }
}
