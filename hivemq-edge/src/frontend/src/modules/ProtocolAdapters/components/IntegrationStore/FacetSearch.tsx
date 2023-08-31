import { ChangeEvent, FC, useMemo } from 'react'
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
import { useTranslation } from 'react-i18next'

import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'

import { ProtocolFacetType } from '../../types.ts'

interface SearchFilterAdaptersProps {
  facet: ProtocolFacetType | undefined
  onChange?: (value: ProtocolFacetType) => void
}

const FacetSearch: FC<SearchFilterAdaptersProps> = ({ facet, onChange }) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()

  const { categories, tags } = useMemo(() => {
    // const categories = Array.from(new Set(data?.items?.map((e) => e.category) || []))
    const categories =
      data?.items?.reduce((acc, cur) => {
        cur?.category
        return cur?.category ? Array.from(new Set([...acc, cur.category])) : acc
      }, [] as string[]) || []
    const tags =
      data?.items?.reduce((acc, cur) => {
        return Array.from(new Set([...acc, ...(cur?.tags || [])]))
      }, [] as string[]) || []
    return { categories, tags }
  }, [data])

  const handleChangeSearch = (event: ChangeEvent<HTMLInputElement>) => {
    onChange?.({ search: event.target.value })
  }

  const handleClearSearch = () => {
    onChange?.({ search: null })
  }

  const handleFilter = (filter: { key: keyof ProtocolAdapter; value: string } | null) => {
    onChange?.({ filter: filter })
  }

  return (
    <Flex flexDirection={'column'} gap={4} maxW={'250px'} minW={'fit-content'}>
      <FormControl>
        <FormLabel>{t('protocolAdapter.facet.search.label')}</FormLabel>
        <InputGroup>
          <InputLeftElement pointerEvents="none">
            <SearchIcon />
          </InputLeftElement>
          <Input
            id={'facet-search-input'}
            placeholder={t('protocolAdapter.facet.search.placeholder') as string}
            onChange={handleChangeSearch}
            value={facet?.search || ''}
          />
          <InputRightElement>
            <CloseButton
              id={'facet-search-clear'}
              onClick={handleClearSearch}
              aria-label={t('protocolAdapter.facet.search.clear') as string}
            />
          </InputRightElement>
        </InputGroup>
      </FormControl>

      <Flex flexDirection={'column'}>
        <Box pb={4} pt={4}>
          <List spacing={3}>
            <ListItem>
              <Button
                data-testid={`facet-filter-clear`}
                justifyContent={'flex-start'}
                variant={!facet?.filter ? 'outline' : 'ghost'}
                aria-pressed={!facet?.filter}
                size="sm"
                w={'100%'}
                onClick={() => handleFilter(null)}
              >
                {t('protocolAdapter.facet.filter.allFilters')}
              </Button>
            </ListItem>
          </List>
        </Box>
        <div>{t('protocolAdapter.facet.filter.category')}</div>
        <Box pb={4} pt={4}>
          <List spacing={3}>
            {categories.map((item) => (
              <ListItem key={item}>
                <Button
                  data-testid={`facet-filter-category-${item}`}
                  justifyContent={'flex-start'}
                  variant={facet?.filter?.value === item ? 'outline' : 'ghost'}
                  aria-pressed={facet?.filter?.value === item}
                  size="sm"
                  w={'100%'}
                  onClick={() => handleFilter({ value: item, key: 'category' })}
                >
                  {item}
                </Button>
              </ListItem>
            ))}
          </List>
        </Box>
        <div>{t('protocolAdapter.facet.filter.tags')}</div>
        <Box pb={4} pt={4}>
          <List spacing={3}>
            {tags.map((item) => (
              <ListItem key={item}>
                <Button
                  data-testid={`facet-filter-tag-${item}`}
                  justifyContent={'flex-start'}
                  variant={facet?.filter?.value === item ? 'outline' : 'ghost'}
                  size="sm"
                  w={'100%'}
                  onClick={() => handleFilter({ value: item, key: 'tags' })}
                  aria-pressed={facet?.filter?.value === item}
                >
                  {item}
                </Button>
              </ListItem>
            ))}
          </List>
        </Box>
      </Flex>
    </Flex>
  )
}

export default FacetSearch
