import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { Accordion, AccordionItem, AccordionButton, AccordionPanel, AccordionIcon, Box } from '@chakra-ui/react'

import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardMapping } from '@/modules/Mappings/types.ts'
import ListMappings from '@/components/rjsf/MqttTransformation/components/ListMappings.tsx'
import MappingContainer from '@/components/rjsf/MqttTransformation/components/MappingContainer.tsx'

export const MqttTransformationField: FC<FieldProps<OutwardMapping[], RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const [subsData, setSubsData] = useState<OutwardMapping[] | undefined>(props.formData)

  const { adapterId, adapterType } = props.formContext || {}

  useEffect(() => {
    props.onChange(subsData)
  }, [props, subsData])

  const handleEdit = (index: number) => {
    setSelectedItem(index)
  }

  const handleDelete = (index: number) => {
    setSubsData((old) => {
      const gg = [...(old || [])]
      gg.splice(index, 1)
      return [...gg]
    })
  }

  const handleClose = () => {
    setSelectedItem(undefined)
  }

  const handleSubmit = () => {
    setSelectedItem(undefined)
    props.onChange(subsData)
  }

  const handleAdd = () => {
    setSubsData((old) => [
      ...(old || []),
      {
        mqttTopicFilter: undefined,
        tag: undefined,
        fieldMapping: [],
      },
    ])
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleChange = (id: keyof OutwardMapping, v: any) => {
    if (selectedItem === undefined) return
    setSubsData((old) => {
      const currentItem = old?.[selectedItem]
      if (currentItem) currentItem[id] = v
      return [...(old || [])]
    })
  }

  if (!subsData) return null

  return (
    <Accordion defaultIndex={0} index={selectedItem === undefined ? 0 : 1} data-testid="mapping-editor-switch">
      <AccordionItem isDisabled={selectedItem !== undefined}>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            {t('rjsf.MqttTransformationField.tabs.list')}
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel pb={4}>
          <ListMappings
            items={subsData}
            onEdit={handleEdit}
            onAdd={handleAdd}
            onDelete={handleDelete}
            isDisabled={selectedItem !== undefined}
          />
        </AccordionPanel>
      </AccordionItem>

      <AccordionItem isDisabled={selectedItem === undefined}>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            {t('rjsf.MqttTransformationField.tabs.editor')}
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel pb={4}>
          {selectedItem !== undefined && (
            <MappingContainer
              adapterId={adapterId}
              adapterType={adapterType}
              item={subsData[selectedItem]}
              onClose={handleClose}
              onSubmit={handleSubmit}
              onChange={handleChange}
            />
          )}
        </AccordionPanel>
      </AccordionItem>
    </Accordion>
  )
}
