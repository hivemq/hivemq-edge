import type { FC } from 'react'
import { useMemo } from 'react'
import type { ArrayFieldTemplateItemType } from '@rjsf/utils'
import { ButtonGroup, Td, Tr } from '@chakra-ui/react'
import { CopyButton, MoveDownButton, MoveUpButton, RemoveButton } from '@/components/rjsf/__internals/IconButton.tsx'

export const CompactArrayFieldItemTemplate: FC<ArrayFieldTemplateItemType> = (props) => {
  const {
    children,
    index,
    hasMoveUp,
    hasRemove,
    hasMoveDown,
    hasCopy,
    disabled,
    readonly,
    onCopyIndexClick,
    onDropIndexClick,
    onReorderClick,
    uiSchema,
    registry,
  } = props

  const onCopyClick = useMemo(() => onCopyIndexClick(index), [index, onCopyIndexClick])
  const onRemoveClick = useMemo(() => onDropIndexClick(index), [index, onDropIndexClick])
  const onArrowUpClick = useMemo(() => onReorderClick(index, index - 1), [index, onReorderClick])
  const onArrowDownClick = useMemo(() => onReorderClick(index, index + 1), [index, onReorderClick])

  return (
    <Tr>
      {children}
      <Td>
        <ButtonGroup isAttached size="sm" orientation="horizontal" ml={2}>
          {(hasMoveUp || hasMoveDown) && (
            <MoveUpButton
              data-testid="compact-up-item"
              disabled={disabled || readonly || !hasMoveUp}
              onClick={onArrowUpClick}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
          {(hasMoveUp || hasMoveDown) && (
            <MoveDownButton
              data-testid="compact-down-item"
              disabled={disabled || readonly || !hasMoveDown}
              onClick={onArrowDownClick}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
          {hasCopy && (
            <CopyButton
              data-testid="compact-copy-item"
              disabled={disabled || readonly}
              onClick={onCopyClick}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
          {hasRemove && (
            <RemoveButton
              data-testid="compact-delete-item"
              disabled={disabled || readonly}
              onClick={onRemoveClick}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
        </ButtonGroup>
      </Td>
    </Tr>
  )
}
