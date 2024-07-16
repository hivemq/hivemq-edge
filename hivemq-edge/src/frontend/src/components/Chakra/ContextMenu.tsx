import { MutableRefObject, useCallback, useEffect, useRef, useState } from 'react'
import { useEventListener, Portal, Menu, MenuButton, PortalProps, MenuButtonProps, MenuProps } from '@chakra-ui/react'

export interface ContextMenuProps<T extends HTMLElement> {
  renderMenu: () => JSX.Element | null
  children: (ref: MutableRefObject<T | null>) => JSX.Element | null
  menuProps?: Omit<MenuProps, 'children'> & { children?: React.ReactNode }
  portalProps?: Omit<PortalProps, 'children'> & { children?: React.ReactNode }
  menuButtonProps?: MenuButtonProps
}

/**
 * From https://github.com/lukasbach/chakra-ui-contextmenu
 * @deprecated Doesn't integrate well with React Flow (keyboard navigation) and ChakraUI (focus on first element)
 */
export function ContextMenu<T extends HTMLElement = HTMLElement>(props: ContextMenuProps<T>) {
  const [isOpen, setIsOpen] = useState(false)
  const [isRendered, setIsRendered] = useState(false)
  const [isDeferredOpen, setIsDeferredOpen] = useState(false)
  const [position, setPosition] = useState<[number, number]>([0, 0])
  const targetRef = useRef<T>(null)

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        setIsRendered(true)
        setTimeout(() => {
          setIsDeferredOpen(true)
        })
      })
    } else {
      setIsDeferredOpen(false)
      const timeout = setTimeout(() => {
        setIsRendered(isOpen)
      }, 1000)
      return () => clearTimeout(timeout)
    }
  }, [isOpen])

  useEventListener('contextmenu', (e) => {
    if (targetRef.current?.contains(e.target as any) || e.target === targetRef.current) {
      e.preventDefault()
      setIsOpen(true)
      setPosition([e.pageX, e.pageY])
    } else {
      setIsOpen(false)
    }
  })

  const onCloseHandler = useCallback(() => {
    props.menuProps?.onClose?.()
    setIsOpen(false)
  }, [props.menuProps])

  return (
    <>
      {props.children(targetRef)}
      {isRendered && (
        <Portal {...props.portalProps}>
          <Menu isOpen={isDeferredOpen} gutter={0} {...props.menuProps} onClose={onCloseHandler}>
            <MenuButton
              aria-hidden={true}
              w={1}
              h={1}
              style={{
                position: 'absolute',
                left: position[0],
                top: position[1],
                cursor: 'default',
              }}
              {...props.menuButtonProps}
            />
            {props.renderMenu()}
          </Menu>
        </Portal>
      )}
    </>
  )
}
