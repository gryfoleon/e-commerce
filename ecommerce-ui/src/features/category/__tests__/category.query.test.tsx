import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getCategories, getCategoryById, getCategoryByName } from '../api/category.api'
import type { CategoryDto, CategoryPage } from '../api/category.dto'
import {
  CATEGORY_OPTIONS_PARAMS,
  categoryQueryKeys,
  useCategoriesQuery,
  useCategoryByIdQuery,
  useCategoryByNameQuery,
  useCategoryOptionsQuery
} from '../hooks/category.query'

vi.mock('../api/category.api', () => ({
  getCategories: vi.fn(),
  getCategoryById: vi.fn(),
  getCategoryByName: vi.fn()
}))

const getCategoriesMock = vi.mocked(getCategories)
const getCategoryByIdMock = vi.mocked(getCategoryById)
const getCategoryByNameMock = vi.mocked(getCategoryByName)

const category = (id: number, name: string): CategoryDto => ({
  id,
  name,
  createdAt: '2026-11-11T20:00:00Z'
})

const page: CategoryPage = {
  content: [category(1, 'Accessories'), category(2, 'Books')],
  pageDetails: { page: 0, size: 100, totalPages: 1, totalElements: 2, isLast: true }
}

const createWrapper = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('categoryQueryKeys', () => {
  it('nests list and detail keys under the feature root', () => {
    expect(categoryQueryKeys.list({ size: 10 }).slice(0, 2)).toEqual(categoryQueryKeys.lists())
    expect(categoryQueryKeys.detail(5)).toEqual(['categories', 'detail', 5])
  })

  it('normalizes names so case variants share a cache entry', () => {
    expect(categoryQueryKeys.byName('  Books ')).toEqual(categoryQueryKeys.byName('books'))
  })
})

describe('useCategoriesQuery', () => {
  it('fetches the page and exposes categories and pageDetails', async () => {
    getCategoriesMock.mockResolvedValue(page)
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoriesQuery({ size: 20 }), { wrapper })

    expect(result.current.categories).toEqual([])
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.categories).toEqual(page.content)
    expect(result.current.pageDetails).toEqual(page.pageDetails)
    expect(getCategoriesMock).toHaveBeenCalledWith({ size: 20 }, expect.any(AbortSignal))
  })

  it('surfaces errors', async () => {
    getCategoriesMock.mockRejectedValue(new Error('boom'))
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoriesQuery(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.message).toBe('boom')
  })
})

describe('useCategoryOptionsQuery', () => {
  it('requests the selector page and maps categories to options', async () => {
    getCategoriesMock.mockResolvedValue(page)
    const { wrapper, queryClient } = createWrapper()

    const { result } = renderHook(() => useCategoryOptionsQuery(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.options).toEqual([
      { value: 'Accessories', label: 'Accessories' },
      { value: 'Books', label: 'Books' }
    ])
    expect(getCategoriesMock).toHaveBeenCalledWith(CATEGORY_OPTIONS_PARAMS, expect.any(AbortSignal))
    expect(queryClient.getQueryData(categoryQueryKeys.list(CATEGORY_OPTIONS_PARAMS))).toEqual(page)
  })
})

describe('useCategoryByIdQuery', () => {
  it('fetches the category by id', async () => {
    getCategoryByIdMock.mockResolvedValue(category(5, 'Electronics'))
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoryByIdQuery(5), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(category(5, 'Electronics'))
    expect(getCategoryByIdMock).toHaveBeenCalledWith(5, expect.any(AbortSignal))
  })

  it.each([null, undefined])('stays idle when id is %s', (id) => {
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoryByIdQuery(id), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(getCategoryByIdMock).not.toHaveBeenCalled()
  })
})

describe('useCategoryByNameQuery', () => {
  it('fetches by the trimmed name', async () => {
    getCategoryByNameMock.mockResolvedValue(category(3, 'Food & Beverage'))
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoryByNameQuery(' food & beverage '), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.name).toBe('Food & Beverage')
    expect(getCategoryByNameMock).toHaveBeenCalledWith('food & beverage', expect.any(AbortSignal))
  })

  it.each(['', '   ', null, undefined])('stays idle when name is %j', (name) => {
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useCategoryByNameQuery(name), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(getCategoryByNameMock).not.toHaveBeenCalled()
  })
})
