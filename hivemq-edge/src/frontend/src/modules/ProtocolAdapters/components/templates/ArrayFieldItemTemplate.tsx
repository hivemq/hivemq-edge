import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ArrayFieldTemplateItemType, getTemplate, getUiOptions } from '@rjsf/utils'
import { Box, ButtonGroup, FormControl, HStack, useDisclosure, VStack } from '@chakra-ui/react'
import { LuPanelTopClose, LuPanelTopOpen } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'

// TODO[NVL] This is driven by subscription handling; use uiSchema to allow configuration for individual array property
export const ArrayFieldItemTemplate: FC<ArrayFieldTemplateItemType> = (props) => {
  const {
    children,
    disabled,
    hasToolbar,
    hasCopy,
    hasMoveDown,
    hasMoveUp,
    hasRemove,
    index,
    onCopyIndexClick,
    onDropIndexClick,
    onReorderClick,
    readonly,
    uiSchema,
    registry,
  } = props
  const { t } = useTranslation('components')
  const { isOpen, onToggle, getButtonProps, getDisclosureProps } = useDisclosure({
    // This is a real hack but didn't find a better way of detecting a new item
    defaultIsOpen: index === props.totalItems - 1 && children.props.formData.destination === undefined,
  })
  const name = useMemo<string>(() => {
    const childrenFormData = children.props.formData.destination
    if (childrenFormData) return `${children.props.name} - ${childrenFormData}`
    return children.props.name
  }, [children.props.formData.destination, children.props.name])

  const { CopyButton, MoveDownButton, MoveUpButton, RemoveButton } = registry.templates.ButtonTemplates
  const onCopyClick = useMemo(() => onCopyIndexClick(index), [index, onCopyIndexClick])
  const onRemoveClick = useMemo(() => onDropIndexClick(index), [index, onDropIndexClick])
  const onArrowUpClick = useMemo(() => onReorderClick(index, index - 1), [index, onReorderClick])
  const onArrowDownClick = useMemo(() => onReorderClick(index, index + 1), [index, onReorderClick])

  const renderCollapsed = () => {
    const uiOptions = getUiOptions(uiSchema)
    const TitleFieldTemplate = getTemplate<'TitleFieldTemplate'>('TitleFieldTemplate', registry, uiOptions)
    return (
      <FormControl variant="hivemq">
        <TitleFieldTemplate
          title={name}
          id={children.props.name}
          registry={registry}
          uiSchema={uiSchema}
          schema={props.schema}
        />
      </FormControl>
    )
  }
  const { hidden, ...rest } = getDisclosureProps()

  return (
    <HStack flexDirection="row-reverse" alignItems="flex-start" py={1} role="listitem">
      {hasToolbar && (
        <VStack gap={6} role="toolbar">
          <ButtonGroup>
            <IconButton
              icon={isOpen ? <LuPanelTopClose /> : <LuPanelTopOpen />}
              onClick={onToggle}
              {...getButtonProps()}
              aria-label={t('rjsf.ArrayFieldItem.Buttons.expanded', { context: isOpen ? 'true' : 'false' })}
            />
          </ButtonGroup>
          {isOpen && (
            <ButtonGroup isAttached mb={1} orientation="vertical">
              {(hasMoveUp || hasMoveDown) && (
                <MoveUpButton
                  disabled={disabled || readonly || !hasMoveUp}
                  onClick={onArrowUpClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {(hasMoveUp || hasMoveDown) && (
                <MoveDownButton
                  disabled={disabled || readonly || !hasMoveDown}
                  onClick={onArrowDownClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {hasCopy && (
                <CopyButton
                  disabled={disabled || readonly}
                  onClick={onCopyClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
              {hasRemove && (
                <RemoveButton
                  disabled={disabled || readonly}
                  onClick={onRemoveClick}
                  uiSchema={uiSchema}
                  registry={registry}
                />
              )}
            </ButtonGroup>
          )}
        </VStack>
      )}

      <Box w="100%" {...rest}>
        {isOpen ? children : renderCollapsed()}
      </Box>
    </HStack>
  )
}
