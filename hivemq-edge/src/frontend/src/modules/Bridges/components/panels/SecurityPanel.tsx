import { FC } from 'react'
import { FormControl, FormHelperText, FormLabel, Input, HStack, Switch } from '@chakra-ui/react'
import { Select } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'
import { Controller, useWatch } from 'react-hook-form'

import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'
import { $TlsConfiguration } from '@/api/__generated__'

import { CYPHER_SUITES, TLS_PROTOCOLS } from '../../utils/tlsConfiguration.ts'
import { BridgePanelType } from '../../types.ts'

const SecurityPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors },
  } = form
  const getRulesForProperty = useValidationRules()

  const isTlsEnabled = useWatch({ name: 'tlsConfiguration.enabled', control: form.control })

  return (
    <FormControl variant={'hivemq'} flexGrow={1} display={'flex'} flexDirection={'column'} gap={4} as={'fieldset'}>
      <FormControl>
        <FormLabel htmlFor={'tlsConfiguration.enabled'}>{t('bridge.security.enabled.label')}</FormLabel>
        <Switch
          id={'tlsConfiguration.enabled'}
          {...register('tlsConfiguration.enabled', {
            ...getRulesForProperty($TlsConfiguration.properties.enabled),
          })}
        />
        <FormHelperText>{t('bridge.security.enabled.helper')}</FormHelperText>
      </FormControl>

      {isTlsEnabled && (
        <>
          <FormControl isInvalid={!!errors.tlsConfiguration?.cipherSuites}>
            <FormLabel htmlFor={'tlsConfiguration.cipherSuites'}>{t('bridge.security.cipherSuites.label')}</FormLabel>
            <Controller
              name={'tlsConfiguration.cipherSuites'}
              control={form.control}
              render={({ field }) => (
                <Select
                  size={'sm'}
                  {...field}
                  inputId={'tlsConfiguration.cipherSuites'}
                  // @ts-ignore TODO[NVL] Need to fix the Typescript definition
                  // defaultValue={}
                  // @ts-ignore
                  options={CYPHER_SUITES.map((e) => ({ label: e, value: e }))}
                  // menuIsOpen={false}
                  isClearable={true}
                  placeholder={'add topic'}
                  isMulti={true}
                  components={{
                    DropdownIndicator: null,
                  }}
                />
              )}
            />
            <FormHelperText>{t('bridge.security.cipherSuites.helper')}</FormHelperText>
          </FormControl>

          <FormControl isInvalid={!!errors.tlsConfiguration?.protocols}>
            <FormLabel htmlFor={'tlsConfiguration.protocols'}>{t('bridge.security.protocols.label')}</FormLabel>
            <Controller
              name={'tlsConfiguration.protocols'}
              control={form.control}
              render={({ field }) => (
                <Select
                  size={'sm'}
                  {...field}
                  inputId={'tlsConfiguration.protocols'}
                  // @ts-ignore TODO[NVL] Need to fix the Typescript definition
                  // @ts-ignore
                  options={TLS_PROTOCOLS.map((e) => ({ label: e, value: e }))}
                  // menuIsOpen={false}
                  isClearable={true}
                  isMulti={true}
                  isSearchable={true}
                  components={{
                    DropdownIndicator: null,
                  }}
                />
              )}
            />
            <FormHelperText>{t('bridge.security.protocols.helper')}</FormHelperText>
          </FormControl>

          <FormControl as="fieldset">
            <FormLabel as="legend">{t('bridge.security.keyStore.legend')}</FormLabel>

            <HStack>
              <FormControl isInvalid={!!errors.tlsConfiguration?.keystorePath}>
                <FormLabel htmlFor={'tlsConfiguration.keystorePath'}>
                  {t('bridge.security.keystorePath.label')}
                </FormLabel>
                <Controller
                  name={'tlsConfiguration.keystorePath'}
                  control={form.control}
                  render={({ field }) => {
                    const { value, ...rest } = field
                    return <Input id={'tlsConfiguration.keystorePath'} {...rest} value={value as string} />
                  }}
                />
                <FormHelperText>{t('bridge.security.keystorePath.helper')}</FormHelperText>
              </FormControl>

              <FormControl isInvalid={!!errors.tlsConfiguration?.keystorePassword}>
                <FormLabel htmlFor={'tlsConfiguration.keystorePassword'}>
                  {t('bridge.security.keystorePassword.label')}
                </FormLabel>
                <Controller
                  name={'tlsConfiguration.keystorePassword'}
                  control={form.control}
                  render={({ field }) => (
                    <Input
                      autoComplete={'current-password'}
                      type={'text'}
                      id={'tlsConfiguration.keystorePassword'}
                      {...field}
                    />
                  )}
                />
                <FormHelperText>{t('bridge.security.keystorePassword.helper')}</FormHelperText>
              </FormControl>
            </HStack>
          </FormControl>

          <FormControl as="fieldset">
            <FormLabel as="legend">{t('bridge.security.trustStore.legend')}</FormLabel>

            <HStack>
              <FormControl isInvalid={!!errors.tlsConfiguration?.truststorePath}>
                <FormLabel htmlFor={'tlsConfiguration.truststorePath'}>
                  {t('bridge.security.truststorePath.label')}
                </FormLabel>
                <Controller
                  name={'tlsConfiguration.truststorePath'}
                  control={form.control}
                  render={({ field }) => {
                    const { value, ...rest } = field
                    return <Input id={'tlsConfiguration.truststorePath'} {...rest} value={value as string} />
                  }}
                />
                <FormHelperText>{t('bridge.security.truststorePath.helper')}</FormHelperText>
              </FormControl>

              <FormControl isInvalid={!!errors.tlsConfiguration?.truststorePassword}>
                <FormLabel htmlFor={'tlsConfiguration.truststorePassword'}>
                  {t('bridge.security.truststorePassword.label')}
                </FormLabel>
                <Controller
                  name={'tlsConfiguration.truststorePassword'}
                  control={form.control}
                  render={({ field }) => (
                    <Input
                      autoComplete={'current-password'}
                      type={'text'}
                      id={'tlsConfiguration.truststorePassword'}
                      {...field}
                    />
                  )}
                />
                <FormHelperText>{t('bridge.security.truststorePassword.helper')}</FormHelperText>
              </FormControl>
            </HStack>
          </FormControl>

          <FormControl isInvalid={!!errors.tlsConfiguration?.privateKeyPassword}>
            <FormLabel htmlFor={'tlsConfiguration.privateKeyPassword'}>
              {t('bridge.security.privateKeyPassword.label')}
            </FormLabel>
            <Controller
              name={'tlsConfiguration.privateKeyPassword'}
              control={form.control}
              render={({ field }) => (
                <Input
                  type={'text'}
                  autoComplete={'current-password'}
                  id={'tlsConfiguration.privateKeyPassword'}
                  {...field}
                />
              )}
            />
            <FormHelperText>{t('bridge.security.privateKeyPassword.helper')}</FormHelperText>
          </FormControl>
        </>
      )}
    </FormControl>
  )
}

export default SecurityPanel
