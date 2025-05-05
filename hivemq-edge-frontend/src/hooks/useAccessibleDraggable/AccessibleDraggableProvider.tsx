import { type FC, type PropsWithChildren, useCallback, useEffect, useState, useRef, type MutableRefObject } from 'react'
import { useToast } from '@chakra-ui/react'
import { useHotkeys } from 'react-hotkeys-hook'
import { useTranslation } from 'react-i18next'

import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'
import { DATAHUB_HOTKEY } from '@/extensions/datahub/utils/datahub.utils'

import { AccessibleDraggableContext, type AccessibleDraggableProps } from './index'

const TOAST_DRAGGABLE = 'TOAST_DRAGGABLE'

export const AccessibleDraggableProvider: FC<PropsWithChildren> = ({ children }) => {
  const { t } = useTranslation('components')
  const draggableRef = useRef<HTMLDivElement | null>(null)

  const [isDragging, setIsDragging] = useState(false)
  const [source, setSource] = useState<
    { property: FlatJSONSchema7; dataReference?: DataReference | undefined } | undefined
  >(undefined)

  const toast = useToast({
    id: TOAST_DRAGGABLE,
    onCloseComplete: () => {
      setIsDragging(false)
      setSource(undefined)
    },
    variant: 'top-accent',
    duration: null,
    isClosable: true,
    containerStyle: {
      width: 'var(--chakra-sizes-2xl)',
      maxWidth: '100%',
    },
    description: t('AccessibleDraggable.alert.description'),
  })

  const onStartDragging = useCallback(
    (data: {
      property: FlatJSONSchema7
      dataReference?: DataReference | undefined
      ref?: MutableRefObject<HTMLDivElement | null>
    }) => {
      setIsDragging(true)
      setSource(data)
      if (data.ref) draggableRef.current = data.ref.current
      if (!toast.isActive(TOAST_DRAGGABLE)) toast()
    },
    [toast]
  )

  const onEndDragging = useCallback(() => {
    draggableRef.current?.focus()
    setIsDragging(false)
    setSource(undefined)
    toast.close(TOAST_DRAGGABLE)
    draggableRef.current = null
  }, [toast])

  // TODO[NVL] ESCAPE is caught by Chakra (for closing the modals) and cannot be intercepted yet; reverting to BACKSPACE
  useHotkeys([DATAHUB_HOTKEY.ESCAPE, DATAHUB_HOTKEY.BACKSPACE], () => {
    onEndDragging()
  })

  useEffect(() => {
    const handleMouseDown = () => {
      onEndDragging()
    }

    document.addEventListener('mousedown', handleMouseDown)
    return () => {
      document.removeEventListener('mousedown', handleMouseDown)
    }
  }, [onEndDragging])

  const defaultAccessibleDraggableValues: AccessibleDraggableProps = {
    source,
    isDragging: isDragging,
    startDragging: onStartDragging,
    endDragging: onEndDragging,
    isValidDrop: (target: FlatJSONSchema7) => {
      if (!source) return false

      return source.property.type === target.type && source.property.arrayType === target.arrayType
    },
    ref: draggableRef,
  }

  return (
    <AccessibleDraggableContext.Provider value={defaultAccessibleDraggableValues}>
      {children}
    </AccessibleDraggableContext.Provider>
  )
}
