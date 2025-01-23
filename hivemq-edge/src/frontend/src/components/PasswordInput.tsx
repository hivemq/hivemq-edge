import type { FC } from 'react'
import { useState } from 'react'
import type { InputProps as CUIInputProps } from '@chakra-ui/react'
import { Button, Input, InputGroup, InputRightElement } from '@chakra-ui/react'

import type {
  FieldValues,
  UseFormRegister,
  RegisterOptions,
  // useForm, // don't need this import
} from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { BiHide, BiShow } from 'react-icons/bi'

interface InputProps extends CUIInputProps {
  register: UseFormRegister<FieldValues> // declare register props
  options: RegisterOptions
}

const PasswordInput: FC<InputProps> = ({ register, options, ...rest }) => {
  const { t } = useTranslation()
  const [show, setShow] = useState(false)
  const handleClick = () => setShow(!show)

  return (
    <InputGroup size="md">
      <Input {...rest} {...register(rest.name as string, options)} pr="4.5rem" type={show ? 'text' : 'password'} />
      <InputRightElement width="3.2rem">
        <Button
          h="1.75rem"
          size="sm"
          onClick={handleClick}
          aria-label={show ? t('login.password.hide') : t('login.password.show')}
        >
          {show ? <BiHide /> : <BiShow />}
        </Button>
      </InputRightElement>
    </InputGroup>
  )
}

export default PasswordInput
