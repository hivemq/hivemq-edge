import type {
  FormContextType,
  ObjectFieldTemplateProps,
  RJSFSchema,
  StrictRJSFSchema,
  UIOptionsType,
} from '@rjsf/utils'
import { descriptionId } from '@rjsf/utils'
import { getTemplate, getUiOptions, titleId } from '@rjsf/utils'
import { Box, Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'
import type { UITab } from '@/components/rjsf/Form/types.ts'
import { useFormControlStore } from '@/components/rjsf/Form/useFormControlStore.ts'

export const ObjectFieldTemplate = <
  T = never,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = never,
>(
  props: ObjectFieldTemplateProps<T, S, F>
) => {
  const { registry, properties, title, description, uiSchema, required, schema, idSchema } = props
  const uiOptions = getUiOptions(uiSchema, {})
  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate', T, S, F>('TitleFieldTemplate', registry, uiOptions)
  const DescriptionFieldTemplate = getTemplate<'DescriptionFieldTemplate', T, S, F>(
    'DescriptionFieldTemplate',
    registry,
    uiOptions
  )
  const { tabIndex, setTabIndex } = useFormControlStore()

  const { tabs } = uiOptions as UIOptionsType & { tabs?: UITab[] }
  if (!tabs) {
    return (
      <>
        <Box mb={4}>
          {title && (
            <TitleFieldTemplate
              id={titleId<T>(idSchema)}
              title={title}
              required={required}
              schema={schema}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
          {description && (
            <DescriptionFieldTemplate
              id={descriptionId<T>(idSchema)}
              description={description}
              schema={schema}
              uiSchema={uiSchema}
              registry={registry}
            />
          )}
        </Box>
        {properties.map((prop) => (
          <Box _notLast={{ marginBottom: '24px' }} className="x0" key={prop.content.key}>
            {prop.content}
          </Box>
        ))}
      </>
    )
  }

  // TODO[NVL] Not efficient. Build a cluster
  const allGrouped = tabs.map((e) => e.properties).flat()

  return (
    <>
      <Tabs index={tabIndex} onChange={setTabIndex}>
        <TabList>
          {tabs.map((e) => {
            const filteredProps = properties.filter((property) => e.properties.includes(property.name))
            if (!filteredProps.length) return null
            return (
              <Tab fontSize="md" key={e.id}>
                {e.title}
              </Tab>
            )
          })}
        </TabList>

        <TabPanels>
          {tabs.map((e) => {
            const filteredProps = properties.filter((property) => e.properties.includes(property.name))
            if (!filteredProps.length) return null
            return (
              <TabPanel key={e.id} p={0} pt="1px" mb={6}>
                <>
                  {filteredProps.map((prop) => (
                    <Box _first={{ marginTop: '24px' }} _notLast={{ marginBottom: '24px' }} key={prop.content.key}>
                      {prop.content}
                    </Box>
                  ))}
                </>
              </TabPanel>
            )
          })}
        </TabPanels>
        {properties
          .filter((property) => !allGrouped.includes(property.name))
          .map((prop) => (
            <Box _first={{ marginTop: '24px' }} _notLast={{ marginBottom: '24px' }} key={prop.content.key}>
              {prop.content}
            </Box>
          ))}
      </Tabs>
    </>
  )
}
