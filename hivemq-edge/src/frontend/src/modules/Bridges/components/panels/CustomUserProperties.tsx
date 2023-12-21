import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Controller, useFieldArray } from 'react-hook-form'
import { BridgePanelType, SubscriptionType } from '@/modules/Bridges/types.ts'
import { ButtonGroup, FormControl, FormLabel, Input, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { AddIcon } from '@chakra-ui/icons'
import { MdRemove } from 'react-icons/md'
import IconButton from '@/components/Chakra/IconButton.tsx'

interface CustomUserPropertiesProps extends BridgePanelType {
  type: SubscriptionType
  subscriptionIndex: number
}

const CustomUserProperties: FC<CustomUserPropertiesProps> = ({ form, subscriptionIndex, type }) => {
  const { t } = useTranslation()
  const { fields, append, remove } = useFieldArray({
    control: form.control, // control props comes from useForm (optional: if you are using FormContext)
    name: `${type}.${subscriptionIndex}.customUserProperties`, // unique name for your Field Array
  })

  return (
    <FormControl>
      <FormLabel htmlFor={`${type}.${subscriptionIndex}.customUserProperties`}>
        {t('bridge.subscription.customUserProperties.label')}

        <Table variant="unstyled" size="sm" id={`${type}.${subscriptionIndex}.customUserProperties`}>
          {/*<TableCaption placement={'top'}>{t('bridge.subscription.type', { context: type })}</TableCaption>*/}
          <Thead>
            <Tr>
              <Th w={'50%'}>{t('bridge.subscription.customUserProperties.headers.key')}</Th>
              <Th w={'50%'}>{t('bridge.subscription.customUserProperties.headers.value')}</Th>
              <Th>{t('bridge.subscription.customUserProperties.headers.action')}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {fields.map((_, index) => {
              return (
                <Tr key={`${type}.${subscriptionIndex}.customUserProperties.${index}`}>
                  <Td py={0} p={0}>
                    <Controller
                      name={`${type}.${subscriptionIndex}.customUserProperties.${index}.key`}
                      render={({ field }) => (
                        <Input
                          sx={{
                            fontFamily: 'var(--chakra-fonts-mono);',
                            background: 'var(--badge-bg);',
                            color: 'var(--badge-color);',
                          }}
                          {...field}
                          id={`${type}.${subscriptionIndex}.customUserProperties.${index}.key`}
                          type="text"
                          size={'sm'}
                          placeholder={t('bridge.options.id.placeholder') as string}
                        />
                      )}
                      control={form.control}
                    />
                  </Td>
                  <Td py={0} p={0}>
                    <Controller
                      name={`${type}.${subscriptionIndex}.customUserProperties.${index}.value`}
                      render={({ field }) => (
                        <Input
                          sx={{
                            fontFamily: 'var(--chakra-fonts-mono);',
                            background: 'var(--badge-bg);',
                            color: 'var(--badge-color);',
                          }}
                          {...field}
                          id={`${type}.${subscriptionIndex}.customUserProperties.${index}.value`}
                          type="text"
                          size={'sm'}
                          placeholder={t('bridge.options.id.placeholder') as string}
                        />
                      )}
                      control={form.control}
                    />{' '}
                  </Td>
                  <Td py={0}>
                    <ButtonGroup size="sm" isAttached variant="outline">
                      <IconButton
                        variant="outline"
                        size={'xs'}
                        aria-label={t('bridge.subscription.customUserProperties.actions.delete')}
                        icon={<MdRemove />}
                        onClick={() => remove(index)}
                      />
                    </ButtonGroup>
                  </Td>
                </Tr>
              )
            })}
            <Tr>
              <Td colSpan={3}>
                <ButtonGroup size="sm" isAttached variant="outline">
                  <IconButton
                    variant="outline"
                    size={'xs'}
                    aria-label={t('bridge.subscription.customUserProperties.actions.add')}
                    icon={<AddIcon />}
                    onClick={() => append({ key: '', value: '' })}
                  />
                </ButtonGroup>
              </Td>
            </Tr>
          </Tbody>
        </Table>
      </FormLabel>
    </FormControl>
  )
}

export default CustomUserProperties
