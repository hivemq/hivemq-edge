import { FC } from 'react'
import { SubmitHandler, useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router-dom'
import { Box, Button, Flex, FormControl, FormErrorMessage, FormLabel, Heading, Input, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { usePostAuthentication } from '@/api/hooks/usePostAuthentication'
import { ApiBearerToken, ApiError, UsernamePasswordCredentials } from '@/api/__generated__'
import { parseJWT } from '@/api/utils.ts'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import PasswordInput from '@/components/PasswordInput.tsx'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'

const Login: FC = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useTranslation()
  const { isLoading, mutateAsync: submitCredentials } = usePostAuthentication()
  const {
    handleSubmit,
    register,
    setError,
    formState: { errors },
  } = useForm<UsernamePasswordCredentials>()

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

  return (
    <main>
      <Flex align="center" flexDirection="column">
        <Heading as={'h1'} mb={6}>
          {t('translation:login.title')}
        </Heading>

        <Box p={4} maxWidth={450}>
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
              mt={'2em'}
              type="submit"
              isLoading={isLoading}
              colorScheme={'yellow'}
              variant={'solid'}
            >
              {t('translation:login.submit.label')}
            </Button>
          </form>
        </Box>
        <Box p={4} maxWidth={600}>
          {errors.root?.ApiError && (
            <ErrorMessage type={errors.root?.ApiError.type} message={errors.root?.ApiError.message} />
          )}
        </Box>
        <Text fontFamily={'heading'} textAlign={'center'}>
          {t('login.password.support')}
        </Text>
      </Flex>
    </main>
  )
}

export default Login
