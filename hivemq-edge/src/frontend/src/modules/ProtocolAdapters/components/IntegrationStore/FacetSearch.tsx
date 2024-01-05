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
  Skeleton,
} from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

import { ProtocolAdapter, ProtocolAdapterCategory } from '@/api/__generated__'

import { ProtocolFacetType } from '../../types.ts'

interface SearchFilterAdaptersProps {
  items: ProtocolAdapter[]
  facet: ProtocolFacetType | undefined
  onChange?: (value: ProtocolFacetType) => void
  isLoading?: boolean
}

const FacetSearch: FC<SearchFilterAdaptersProps> = ({ items, isLoading, facet, onChange }) => {
  const { t } = useTranslation()

  const { categories, tags } = useMemo(() => {
    // const categories = Array.from(new Set(data?.items?.map((e) => e.category) || []))
    const categories =
      items?.reduce((acc, cur) => {
        if (!cur?.category) return acc
        const setSoFar = acc.map((e) => e.name)
        if (setSoFar.includes(cur.category.name)) return acc
        return [...acc, cur.category]
      }, [] as ProtocolAdapterCategory[]) || []
    const tags =
      items?.reduce((acc, cur) => {
        return Array.from(new Set([...acc, ...(cur?.tags || [])]))
      }, [] as string[]) || []
    return { categories, tags }
  }, [items])

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
    <Flex
      flexDirection={'column'}
      gap={4}
      maxW={'250px'}
      minW={'fit-content'}
      role="region"
      aria-label={t('protocolAdapter.facet.description') as string}
    >
      <FormControl role={'search'}>
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
            isDisabled={isLoading}
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
          <List spacing={3} aria-labelledby={'facet-filter-clear'}>
            <ListItem>
              <Skeleton isLoaded={!isLoading}>
                <Button
                  id={`facet-filter-clear`}
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
              </Skeleton>
            </ListItem>
          </List>
        </Box>
        <Box aria-hidden={true} id={`facet-filter-category`}>
          {t('protocolAdapter.facet.filter.category')}
        </Box>
        <Box pb={4} pt={4}>
          <List spacing={3} aria-labelledby={'facet-filter-category'}>
            {categories.map((item) => (
              <ListItem key={item.name}>
                <Skeleton isLoaded={!isLoading}>
                  <Button
                    data-testid={`facet-filter-category-${item.name}`}
                    justifyContent={'flex-start'}
                    variant={facet?.filter?.value === item.name ? 'outline' : 'ghost'}
                    aria-pressed={facet?.filter?.value === item.name}
                    size="sm"
                    w={'100%'}
                    onClick={() => handleFilter({ value: item.name, key: 'category' })}
                  >
                    {item.displayName}
                  </Button>
                </Skeleton>
              </ListItem>
            ))}
          </List>
        </Box>
        <Box aria-hidden={true} id={`facet-filter-tags`}>
          {t('protocolAdapter.facet.filter.tags')}
        </Box>
        <Box pb={4} pt={4}>
          <List spacing={3} aria-labelledby={'facet-filter-tags'}>
            {tags.map((item) => (
              <ListItem key={item}>
                <Skeleton isLoaded={!isLoading}>
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
                </Skeleton>
              </ListItem>
            ))}
          </List>
        </Box>
      </Flex>
    </Flex>
  )
}

export default FacetSearch
