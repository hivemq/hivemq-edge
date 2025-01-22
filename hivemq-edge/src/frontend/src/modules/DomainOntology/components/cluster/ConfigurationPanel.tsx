import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  HStack,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverHeader,
  PopoverTrigger,
} from '@chakra-ui/react'
import Form from '@rjsf/chakra-ui'
import validator from '@rjsf/validator-ajv8'

import { schema, uiSchema } from '@/modules/DomainOntology/components/cluster/form-cluster.json-schema.ts'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'

interface ConfigurationPanelProps {
  groupKeys: string[]
  onSubmit: (data: string[]) => void
}

const ConfigurationPanel: FC<ConfigurationPanelProps> = ({ groupKeys, onSubmit }) => {
  const { t } = useTranslation()
  const data = useMemo(() => {
    const data = { groups: [...groupKeys] }
    return data
  }, [groupKeys])

  return (
    <Popover placement="bottom-start" closeOnBlur={false} isLazy>
      {({ onClose }) => {
        return (
          <>
            <PopoverTrigger>
              <Button>Configuration</Button>
            </PopoverTrigger>
            <PopoverContent minWidth="sm">
              <PopoverArrow />
              <PopoverCloseButton />
              <PopoverHeader>Configuration</PopoverHeader>
              <PopoverBody>
                <Form
                  id="cluster-form"
                  schema={schema}
                  uiSchema={uiSchema}
                  formData={data}
                  validator={validator}
                  onChange={(e) => console.log('onChange', e)}
                  onSubmit={(e) => {
                    console.log('onSubmit', e.formData.groups)
                    onSubmit(e.formData.groups)
                    onClose()
                  }}
                  templates={{
                    ArrayFieldTemplate,
                  }}
                />
                <HStack justifyContent="flex-end">
                  <Button type="submit" form="cluster-form" variant="primary">
                    {t('ontology.charts.cluster.configuration.cta.submit')}
                  </Button>
                </HStack>
              </PopoverBody>
            </PopoverContent>
          </>
        )
      }}
    </Popover>
  )
}

export default ConfigurationPanel
