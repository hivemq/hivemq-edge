import { FC, useEffect, useState } from 'react'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Flex,
  FormControl,
  FormLabel,
  Switch,
  Text,
} from '@chakra-ui/react'
import Editor from '@monaco-editor/react'
import Form from '@rjsf/chakra-ui'
import validator from '@rjsf/validator-ajv8'
import { IChangeEvent } from '@rjsf/core'
import { RJSFSchema } from '@rjsf/utils'

import AdapterTypeSummary from '@/modules/ProtocolAdapters/components/adapters/AdapterTypeSummary.tsx'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { Adapter, JsonNode, ProtocolAdapter } from '@/api/__generated__'
import { ObjectFieldTemplate } from '@/modules/ProtocolAdapters/components/adapters/ObjectFieldTemplate.tsx'

const MOCK_ADAPTER_TYPE = 'simulation-adapter'
const MONACO_DEFAULTS = {
  height: '28vh',
  options: { minimap: { enabled: false } },
  defaultLanguage: 'json',
}

interface UIGroup {
  title: string
  children: string[]
}

const groups: UIGroup[] = [
  {
    title: 'Important Fields',
    children: ['host', 'id', 'port'],
  },
  { title: 'Less Important Fields', children: ['subscriptions', 'pollingIntervalMillis'] },
]

const UiSchemaTest: FC = () => {
  const { data } = useGetAdapterTypes()
  const [isUiSchema, setIsUiSchema] = useState<boolean>(false)
  const [schema, setSchema] = useState<JsonNode | undefined>(undefined)
  const [uiSchema, setUiSchema] = useState<JsonNode | undefined>({
    'ui:groups': groups,

    'ui:submitButtonOptions': {
      norender: false,
      submitText: 'Validate',
    },
    pollingIntervalMillis: {
      'ui:widget': 'range',
    },
    port: {
      'ui:widget': 'updown',
    },
    subscriptions: {
      items: {
        qos: {
          'ui:widget': 'radio',
          'ui:options': {
            inline: true,
            enum: [0, 1, 2],
          },
        },
      },
    },
  })

  useEffect(() => {
    const adapter: ProtocolAdapter | undefined = data?.items?.find((e) => e.id === MOCK_ADAPTER_TYPE)
    const { configSchema } = adapter || {}
    const { properties, requires, type } = configSchema || {}

    setSchema({ properties, requires, type } as JsonNode)
  }, [data])

  const onValidate = (data: IChangeEvent<Adapter, RJSFSchema>) => {
    console.log('XXXXXX', data)
  }

  return (
    <div>
      <Flex gap={4}>
        <Box w={'50%'} maxW={800}>
          <Accordion defaultIndex={[0]} allowMultiple>
            <AccordionItem>
              <Flex gap={1}>
                <AdapterTypeSummary id={MOCK_ADAPTER_TYPE} />
                <Box m={4}>
                  <FormControl display="flex" alignItems="center">
                    <FormLabel htmlFor="dev-mode" mb="0" ml={1}>
                      <Text fontSize="xs">use UI Schema</Text>
                    </FormLabel>
                    <Switch
                      id="dev-mode"
                      size="sm"
                      onChange={() => setIsUiSchema((old) => !old)}
                      isChecked={isUiSchema}
                    />
                  </FormControl>
                </Box>
              </Flex>
              <h2>
                <AccordionButton>
                  <Box as="span" flex="1" textAlign="left">
                    JSON Schema
                  </Box>
                  <AccordionIcon />
                </AccordionButton>
              </h2>
              <AccordionPanel pb={4}>
                <Editor
                  {...MONACO_DEFAULTS}
                  value={JSON.stringify(schema, null, 2)}
                  onChange={(value: string | undefined) => {
                    if (value) {
                      const dd = JSON.parse(value)
                      setSchema(dd)
                    }
                  }}
                />
              </AccordionPanel>
            </AccordionItem>
            {isUiSchema && (
              <AccordionItem>
                <h2>
                  <AccordionButton>
                    <Box as="span" flex="1" textAlign="left">
                      UI Schema
                    </Box>
                    <AccordionIcon />
                  </AccordionButton>
                </h2>
                <AccordionPanel pb={4}>
                  <Editor
                    {...MONACO_DEFAULTS}
                    value={JSON.stringify(uiSchema, null, 2)}
                    onChange={(value: string | undefined) => {
                      if (value) {
                        const dd = JSON.parse(value)
                        setUiSchema(dd)
                      }
                    }}
                  />
                </AccordionPanel>
              </AccordionItem>
            )}
          </Accordion>
        </Box>
        <Box flexGrow={1} h={'80vh'} overflowY={'scroll'} fontSize={'75%'}>
          {schema && (
            <Form
              id="adapter-instance-form"
              schema={schema}
              uiSchema={isUiSchema ? uiSchema : undefined}
              templates={isUiSchema ? { ObjectFieldTemplate } : undefined}
              liveValidate
              onSubmit={onValidate}
              validator={validator}
              showErrorList={'bottom'}
              onError={(errors) => console.log('XXXXXXX errors', errors)}
              // formData={{ port: 1234, pollingIntervalMillis: 1000 }}
            />
          )}
        </Box>
      </Flex>
    </div>
  )
}

export default UiSchemaTest
