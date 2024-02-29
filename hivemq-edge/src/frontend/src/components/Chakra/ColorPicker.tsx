import {
  Button,
  ButtonProps,
  Popover,
  PopoverArrow,
  PopoverContent,
  PopoverTrigger,
  SimpleGrid,
  PlacementWithLogical,
  Box,
  Flex,
  Portal,
  forwardRef,
} from '@chakra-ui/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

// TODO[NVL] Verify that they are valid colorScheme from the theme
const defaultColorSchemes = ['gray', 'red', 'orange', 'yellow', 'green', 'teal', 'blue', 'cyan', 'purple', 'pink']

interface ColorPickerProps extends Omit<ButtonProps, 'onChange'> {
  colorScheme: string | undefined
  onChange: (value: string) => void
  colorSchemes?: string[]
  isDisabled?: boolean
  placement?: PlacementWithLogical
}

export const ColorPicker = forwardRef<ColorPickerProps, 'div'>(
  ({ onChange, isDisabled, colorScheme, colorSchemes, placement, ...props }: ColorPickerProps, ref) => {
    const { t } = useTranslation('components')
    const schemeOptions = colorSchemes || defaultColorSchemes
    const [selectedColorScheme, setSelectedColorScheme] = useState<string>(colorScheme || schemeOptions[0])

    return (
      <Popover placement={placement || 'bottom'} isLazy>
        <PopoverTrigger>
          <Button
            ref={ref}
            isDisabled={isDisabled}
            data-testid={'colorPicker-trigger'}
            data-color-scheme={selectedColorScheme}
            aria-label={t('ColorPicker.trigger', { scheme: selectedColorScheme }) as string}
            bg={`${selectedColorScheme}.500`}
            _hover={{ bg: `${selectedColorScheme}.500` }}
            _active={{ bg: `${selectedColorScheme}.700` }}
            {...props}
          ></Button>
        </PopoverTrigger>

        <Portal>
          <Box
            // Fix for the portal's zIndex
            sx={{
              '& .chakra-popover__popper': {
                zIndex: 'popover',
              },
            }}
          >
            <PopoverContent
              w="auto"
              boxShadow="md"
              data-testid={'colorPicker-popover'}
              aria-label={'ColorPicker.popover'}
            >
              <PopoverArrow backgroundColor={`${selectedColorScheme}.500`} />
              <SimpleGrid columns={1}>
                <Flex
                  data-testid={'colorPicker-sample'}
                  alignItems={'center'}
                  justifyContent={'center'}
                  borderWidth={0}
                  borderTopRadius={'sm'}
                  h={10}
                  w="100%"
                  p={0}
                  fontSize={'lg'}
                  bg={`${selectedColorScheme}.500`}
                  color={'black'}
                >
                  {selectedColorScheme}
                </Flex>
              </SimpleGrid>
              <SimpleGrid columns={5} p={1} spacing={1}>
                {schemeOptions.map((color, index) => {
                  // const [a] = color.split('.')
                  // const g = `${a}.${selectedColorScheme || 50}`
                  return (
                    <Button
                      key={`color-picker-${color}-${index}`}
                      data-testid={`colorPicker-selector-${index}`}
                      data-color-scheme={color}
                      aria-label={t('ColorPicker.option', { scheme: color }) as string}
                      onClick={() => {
                        setSelectedColorScheme(color)
                        onChange(color)
                      }}
                      h={6}
                      w={6}
                      minW={6}
                      bg={`${color}.500`}
                      _hover={{ bg: `${color}.500`, transform: 'scale(1.1)' }}
                      _active={{ bg: `${color}.500` }}
                    />
                  )
                })}
              </SimpleGrid>
            </PopoverContent>
          </Box>
        </Portal>
      </Popover>
    )
  }
)
