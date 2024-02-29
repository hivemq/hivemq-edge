import { FC, useEffect } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Alert,
  AlertDescription,
  AlertIcon,
  AlertTitle,
  Box,
  Button,
  Flex,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Heading,
  Input,
  Text,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { usePostAuthentication } from '@/api/hooks/usePostAuthentication'
import { ApiBearerToken, ApiError, FirstUseInformation, UsernamePasswordCredentials } from '@/api/__generated__'
import { parseJWT } from '@/api/utils.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import PasswordInput from '@/components/PasswordInput.tsx'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'

const Login: FC<{ first?: FirstUseInformation; preLoadError?: ApiError | null }> = ({ first, preLoadError }) => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useTranslation()
  const { isLoading, isError, error, mutateAsync: submitCredentials } = usePostAuthentication()
  const {
    handleSubmit,
    register,
    setError,
    formState: { errors },
  } = useForm<UsernamePasswordCredentials>({
    defaultValues:
      first?.prefillUsername && first?.prefillPassword
        ? { userName: first.prefillUsername, password: first.prefillPassword }
        : undefined,
  })
  const showFirstUseMessage = (!!first?.firstUseDescription || !!first?.firstUseTitle) && !errors.root

  const verifyCredential = (e: ApiBearerToken) => {
    if (!e.token)
      setError('root.ApiError', {
        type: t('login.error.tokenType') as string,
        message: t('login.error.tokenInvalid') as string,
      })
    else if (!parseJWT(e.token))
      setError('root.ApiError', {
        type: t('login.error.tokenType') as string,
        message: t('login.error.tokenExpired') as string,
      })
    else
      auth.login({ token: e.token }, () => {
        navigate(location.state?.from?.pathname || '/', { replace: true })
      })
  }

  const onSubmit: SubmitHandler<UsernamePasswordCredentials> = (data) => {
    submitCredentials({ password: data.password, userName: data.userName })
      .then(verifyCredential)
      .catch((e: ApiError) => {
        setError('root.ApiError', { type: e.message, message: e.body.title })
      })
  }

  useEffect(() => {
    if (!isError) return

    setError('root.ApiError', { type: error.message, message: error.body?.title || t('login.error.verifyCredentials') })
  }, [isError, error, setError, t])

  useEffect(() => {
    if (!preLoadError) return

    setError('root.ApiError', {
      type: preLoadError.message,
      message: preLoadError.body?.title || t('login.error.defaultCredentials'),
    })
  }, [preLoadError, setError, t])

  return (
    <Flex align="center" flexDirection="column">
      <Box p={4} maxWidth={600}>
        {showFirstUseMessage && (
          <Alert status="info">
            <AlertIcon />
            <div>
              <AlertTitle>{first?.firstUseTitle}</AlertTitle>
              <AlertDescription>{first?.firstUseDescription}</AlertDescription>
            </div>
          </Alert>
        )}
        {errors.root?.ApiError && (
          <ErrorMessage type={errors.root?.ApiError.type} message={errors.root?.ApiError.message} />
        )}
      </Box>

      <Box width={'100%'} maxWidth={'450px'} p={3}>
        <Heading as={'h1'} mb={6}>
          {t('translation:login.title')}
        </Heading>
      </Box>

      <Box p={4} width={'100%'} maxWidth={'450px'}>
        <form onSubmit={handleSubmit(onSubmit)}>
          <FormControl isInvalid={!!errors.userName} isRequired>
            <FormLabel htmlFor="username">{t('translation:login.username.label')}</FormLabel>
            <Input
              autoFocus
              id="username"
              placeholder={t('translation:login.username.placeholder') as string}
              autoComplete="username"
              {...register('userName', {
                required: t('translation:login.username.error.required') as string,
              })}
            />
            <FormErrorMessage>{errors.userName && errors.userName.message}</FormErrorMessage>
          </FormControl>
          <FormControl isInvalid={!!errors.password} mt={'2em'} isRequired>
            <FormLabel htmlFor="password">{t('translation:login.password.label')}</FormLabel>
            <PasswordInput
              id="password"
              name={'password'}
              placeholder={t('translation:login.password.placeholder') as string}
              autoComplete={'current-password'}
              register={register}
              options={{
                required: t('translation:login.password.error.required') as string,
              }}
            />
            <FormErrorMessage>{errors.password && errors.password.message}</FormErrorMessage>
          </FormControl>
          <Button
            data-testid="loginPage-submit"
            width={'100%'}
            mt={'7em'}
            type="submit"
            isLoading={isLoading}
            variant={'primary'}
          >
            {t('translation:login.submit.label')}
          </Button>
        </form>
      </Box>

      {(!first?.firstUseDescription || !first?.firstUseTitle) && (
        <Text fontFamily={'heading'} textAlign={'center'}>
          {t('login.password.support')}
        </Text>
      )}
    </Flex>
  )
}

export default Login
