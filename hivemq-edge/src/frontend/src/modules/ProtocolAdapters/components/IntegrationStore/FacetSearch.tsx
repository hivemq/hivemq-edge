import { ChangeEvent, Dispatch, FC, SetStateAction, useMemo } from 'react'
import {
  Flex,
  FormControl,
  InputGroup,
  FormLabel,
  Input,
  InputRightElement,
  Box,
  List,
  ListItem,
  Button,
  InputLeftElement,
  CloseButton,
} from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { ProtocolFacetType } from '@/modules/ProtocolAdapters/types.ts'
import { useTranslation } from 'react-i18next'

interface SearchFilterAdaptersProps {
  facet: ProtocolFacetType | undefined
  setFacet: Dispatch<SetStateAction<ProtocolFacetType | undefined>>
}

const FacetSearch: FC<SearchFilterAdaptersProps> = ({ facet, setFacet }) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()
  const categories = useMemo(() => {
    return Array.from(new Set(data?.items?.map((e) => e.protocol)))
  }, [data])

  const handleChangeSearch = (event: ChangeEvent<HTMLInputElement>) => {
    setFacet((old) => {
      const { filter } = old || {}
      return { search: event.target.value, filter }
    })
  }

  const handleClearSearch = () => {
    setFacet((old) => {
      const { filter } = old || {}
      return { search: undefined, filter }
    })
  }

  const handleFilter = (filter: { key: string; value: string } | undefined) => {
    setFacet((old) => {
      const { search } = old || {}
      return { search, filter: filter }
    })
  }

  return (
    <Flex flexDirection={'column'} gap={4}>
      <div>
        <FormControl>
          <FormLabel>{t('protocolAdapter.facet.search.label')}</FormLabel>
          <InputGroup>
            <InputLeftElement pointerEvents="none">
              <SearchIcon color="green.500" />
            </InputLeftElement>
            <Input
              placeholder={t('protocolAdapter.facet.search.placeholder') as string}
              onChange={handleChangeSearch}
              value={facet?.search || ''}
            />
            <InputRightElement>
              <CloseButton onClick={handleClearSearch} aria-label={t('protocolAdapter.facet.search.clear') as string} />
            </InputRightElement>
          </InputGroup>
        </FormControl>
      </div>
      <div>
        <Flex flexDirection={'column'}>
          <div>{t('protocolAdapter.facet.categories.label')}</div>
          <Box pb={4} pt={4}>
            <List spacing={3}>
              <ListItem>
                <Button
                  justifyContent={'flex-start'}
                  variant={facet?.filter === undefined ? 'outline' : 'ghost'}
                  size="sm"
                  w={'100%'}
                  onClick={() => handleFilter(undefined)}
                >
                  {t('protocolAdapter.facet.categories.allFilters')}
                </Button>
              </ListItem>
              {categories.map((item) => (
                <ListItem key={item}>
                  <Button
                    justifyContent={'flex-start'}
                    variant={facet?.filter?.value === item ? 'outline' : 'ghost'}
                    size="sm"
                    w={'100%'}
                    onClick={() => handleFilter({ value: item || '', key: 'protocol' })}
                  >
                    {item}
                  </Button>
                </ListItem>
              ))}
            </List>
          </Box>
        </Flex>
      </div>
    </Flex>
  )
}

export default FacetSearch
