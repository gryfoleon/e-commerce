import { type DefaultError, keepPreviousData, skipToken, useQuery, type UseQueryOptions } from '@tanstack/react-query'
import { getCategories, getCategoryById, getCategoryByName } from '../api/category.api'
import type { CategoryDto, CategoryId, CategoryListParams, CategoryPage } from '../api/category.dto'
import { CATEGORY_PAGE_SIZE_MAX } from '../model/category.schema'
import type { CategoryOption } from '../types/category.types'

export const CATEGORY_STALE_TIME_MS = 5 * 60 * 1000

export const CATEGORY_OPTIONS_PARAMS = {
  page: 0,
  size: CATEGORY_PAGE_SIZE_MAX,
  sortBy: 'name',
  sortDirection: 'ASC'
} as const satisfies CategoryListParams

const normalizeName = (name: string): string => name.trim().toLowerCase()

export const categoryQueryKeys = {
  all: ['categories'] as const,
  lists: () => [...categoryQueryKeys.all, 'list'] as const,
  list: (params: CategoryListParams) => [...categoryQueryKeys.lists(), params] as const,
  details: () => [...categoryQueryKeys.all, 'detail'] as const,
  detail: (id: CategoryId) => [...categoryQueryKeys.details(), id] as const,
  byName: (name: string) => [...categoryQueryKeys.details(), 'name', normalizeName(name)] as const
}

export type CategoriesQueryOptions<TData = CategoryPage> = Omit<
  UseQueryOptions<CategoryPage, DefaultError, TData, ReturnType<typeof categoryQueryKeys.list>>,
  'queryKey' | 'queryFn'
>

export const useCategoriesQuery = (params: CategoryListParams = {}, options: CategoriesQueryOptions = {}) => {
  const query = useQuery({
    queryKey: categoryQueryKeys.list(params),
    queryFn: ({ signal }) => getCategories(params, signal),
    placeholderData: keepPreviousData,
    staleTime: CATEGORY_STALE_TIME_MS,
    ...options
  })

  return {
    ...query,
    categories: query.data?.content ?? [],
    pageDetails: query.data?.pageDetails,
    params
  }
}

const toOptions = (page: CategoryPage): CategoryOption[] =>
  page.content.map(({ name }) => ({ value: name, label: name }))

export const useCategoryOptionsQuery = (
  options: Omit<CategoriesQueryOptions<CategoryOption[]>, 'select' | 'placeholderData'> = {}
) => {
  const query = useQuery({
    queryKey: categoryQueryKeys.list(CATEGORY_OPTIONS_PARAMS),
    queryFn: ({ signal }) => getCategories(CATEGORY_OPTIONS_PARAMS, signal),
    select: toOptions,
    staleTime: CATEGORY_STALE_TIME_MS,
    ...options
  })

  return { ...query, options: query.data ?? [] }
}

export type CategoryDetailQueryOptions<TKey extends readonly unknown[]> = Omit<
  UseQueryOptions<CategoryDto, DefaultError, CategoryDto, TKey>,
  'queryKey' | 'queryFn'
>

export const useCategoryByIdQuery = (
  id: CategoryId | null | undefined,
  options: CategoryDetailQueryOptions<ReturnType<typeof categoryQueryKeys.detail>> = {}
) =>
  useQuery({
    queryKey: categoryQueryKeys.detail(id as CategoryId),
    queryFn: id != null ? ({ signal }) => getCategoryById(id, signal) : skipToken,
    staleTime: CATEGORY_STALE_TIME_MS,
    ...options
  })

export const useCategoryByNameQuery = (
  name: string | null | undefined,
  options: CategoryDetailQueryOptions<ReturnType<typeof categoryQueryKeys.byName>> = {}
) => {
  const trimmed = name?.trim() ?? ''
  return useQuery({
    queryKey: categoryQueryKeys.byName(trimmed),
    queryFn: trimmed ? ({ signal }) => getCategoryByName(trimmed, signal) : skipToken,
    staleTime: CATEGORY_STALE_TIME_MS,
    ...options
  })
}
