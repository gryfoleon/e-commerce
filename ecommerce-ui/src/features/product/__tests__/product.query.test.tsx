import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getProductById, searchProducts } from '../api/product.api'
import type { PageableResponse, ProductDto, ProductSearchDto, UUID } from '../api/product.dto'
import {
  productQueryKeys,
  useProductByIdQuery,
  useProductsInfiniteQuery,
  useProductsQuery
} from '../hooks/product.query'

vi.mock('../api/product.api', () => ({
  searchProducts: vi.fn(),
  getProductById: vi.fn()
}))

const searchProductsMock = vi.mocked(searchProducts)
const getProductByIdMock = vi.mocked(getProductById)

const ID = '3f2b8c1e-1d2a-4c5b-9e6f-7a8b9c0d1e2f' as UUID

const product: ProductDto = {
  id: ID,
  sku: 'ELECMS001',
  name: 'Wireless Mouse',
  description: null,
  category: 'Electronics',
  price: 24.99,
  stock: 150,
  weightKg: 0.12,
  version: 3,
  createdAt: '2026-11-10T00:00:00Z',
  updatedAt: '2026-11-10T00:00:00Z'
}

const searchItem = (sku: string): ProductSearchDto => ({ ...product, sku })

const page = (page: number, totalPages: number, content: ProductSearchDto[]): PageableResponse<ProductSearchDto> => ({
  content,
  pageDetails: {
    page,
    size: content.length,
    totalPages,
    totalElements: totalPages * content.length,
    isLast: page + 1 >= totalPages
  }
})

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

describe('productQueryKeys', () => {
  it('nests every list key under lists() so mutations can invalidate them together', () => {
    expect(productQueryKeys.list({ name: 'a' }).slice(0, 2)).toEqual(productQueryKeys.lists())
    expect(productQueryKeys.infinite({ name: 'a' }).slice(0, 2)).toEqual(productQueryKeys.lists())
    expect(productQueryKeys.detail(ID)).toEqual(['products', 'detail', ID])
  })
})

describe('useProductsQuery', () => {
  it('fetches with filters and abort signal, exposes content', async () => {
    searchProductsMock.mockResolvedValue(page(0, 2, [searchItem('A')]))
    const { wrapper } = createWrapper()
    const filters = { name: 'mouse', page: 0, size: 1 }

    const { result } = renderHook(() => useProductsQuery(filters), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(searchProductsMock).toHaveBeenCalledWith(filters, expect.any(AbortSignal))
    expect(result.current.products).toEqual([searchItem('A')])
    expect(result.current.pageDetails?.totalPages).toBe(2)
  })

  it('keeps the previous page visible while a new filter set loads', async () => {
    searchProductsMock.mockResolvedValueOnce(page(0, 2, [searchItem('A')]))
    let resolveNext: (value: PageableResponse<ProductSearchDto>) => void = () => {}
    searchProductsMock.mockReturnValueOnce(new Promise((resolve) => (resolveNext = resolve)))
    const { wrapper } = createWrapper()

    const { result, rerender } = renderHook(({ pageIndex }) => useProductsQuery({ page: pageIndex }), {
      wrapper,
      initialProps: { pageIndex: 0 }
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    rerender({ pageIndex: 1 })
    await waitFor(() => expect(result.current.isPlaceholderData).toBe(true))
    expect(result.current.products).toEqual([searchItem('A')])

    act(() => resolveNext(page(1, 2, [searchItem('B')])))
    await waitFor(() => expect(result.current.products).toEqual([searchItem('B')]))
    expect(searchProductsMock).toHaveBeenCalledTimes(2)
  })
})

describe('useProductsInfiniteQuery', () => {
  it('flattens pages and stops after the last page', async () => {
    searchProductsMock
      .mockResolvedValueOnce(page(0, 2, [searchItem('A')]))
      .mockResolvedValueOnce(page(1, 2, [searchItem('B')]))
    const { wrapper } = createWrapper()
    const filters = { category: 'Electronics', size: 1 }

    const { result } = renderHook(() => useProductsInfiniteQuery(filters), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(searchProductsMock).toHaveBeenLastCalledWith({ ...filters, page: 0 }, expect.any(AbortSignal))
    expect(result.current.hasNextPage).toBe(true)

    await act(() => result.current.fetchNextPage())

    await waitFor(() => expect(result.current.products).toEqual([searchItem('A'), searchItem('B')]))
    expect(searchProductsMock).toHaveBeenLastCalledWith({ ...filters, page: 1 }, expect.any(AbortSignal))
    expect(result.current.hasNextPage).toBe(false)
    expect(result.current.totalElements).toBe(2)
  })

  it('returns an empty list before data arrives', () => {
    searchProductsMock.mockReturnValue(new Promise(() => {}))
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useProductsInfiniteQuery(), { wrapper })

    expect(result.current.products).toEqual([])
    expect(result.current.totalElements).toBe(0)
  })
})

describe('useProductByIdQuery', () => {
  it('does not fetch without an id', () => {
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useProductByIdQuery(undefined), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(getProductByIdMock).not.toHaveBeenCalled()
  })

  it('returns the product and revalidates with If-None-Match, keeping cached data on 304', async () => {
    getProductByIdMock
      .mockResolvedValueOnce({ notModified: false, data: product, etag: '"3"' })
      .mockResolvedValueOnce({ notModified: true })
    const { queryClient, wrapper } = createWrapper()

    const { result } = renderHook(() => useProductByIdQuery(ID), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(product))
    expect(getProductByIdMock).toHaveBeenLastCalledWith(ID, { ifNoneMatch: undefined, signal: expect.any(AbortSignal) })

    await act(() => queryClient.refetchQueries({ queryKey: productQueryKeys.detail(ID) }))

    expect(getProductByIdMock).toHaveBeenLastCalledWith(ID, { ifNoneMatch: '"3"', signal: expect.any(AbortSignal) })
    expect(result.current.data).toEqual(product)
    expect(queryClient.getQueryData(productQueryKeys.detail(ID))).toEqual({ data: product, etag: '"3"' })
  })

  it('fails when the server answers 304 without a cached version', async () => {
    getProductByIdMock.mockResolvedValueOnce({ notModified: true })
    const { wrapper } = createWrapper()

    const { result } = renderHook(() => useProductByIdQuery(ID), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
