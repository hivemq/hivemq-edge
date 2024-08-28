import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FieldProps } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { Accordion, AccordionItem, AccordionButton, AccordionPanel, AccordionIcon, Box } from '@chakra-ui/react'

import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import ListSubscriptions from '@/components/rjsf/MqttTransformation/components/ListSubscriptions.tsx'
import MappingContainer from '@/components/rjsf/MqttTransformation/components/MappingContainer.tsx'

export const MqttTransformationField: FC<FieldProps<OutwardSubscription[], RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const [selectedItem, setSelectedItem] = useState<number | undefined>(undefined)
  const [subsData, setSubsData] = useState<OutwardSubscription[] | undefined>(props.formData)

  useEffect(() => {
    return () => console.log('end', subsData)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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
  }

  const handleAdd = () => {
    setSubsData((old) => [
      ...(old || []),
      {
        node: '',
        'mqtt-topic': [],
        mapping: [],
      },
    ])
  }

  if (!subsData) return null

  return (
    <Accordion defaultIndex={0} index={selectedItem === undefined ? 0 : 1}>
      <AccordionItem isDisabled={selectedItem !== undefined}>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            {t('rjsf.MqttTransformationField.tabs.list')}
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel pb={4}>
          <ListSubscriptions
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
            <MappingContainer item={subsData[selectedItem]} onClose={handleClose} onSubmit={handleSubmit} />
          )}
        </AccordionPanel>
      </AccordionItem>
    </Accordion>
  )
}
