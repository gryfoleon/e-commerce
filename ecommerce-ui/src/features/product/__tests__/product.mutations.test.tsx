import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createProduct, deleteProduct, importProductsCsv, updateProduct } from '../api/product.api'
import type { CsvImportReport, ProductDto, ProductUpdateRequest, UUID } from '../api/product.dto'
import {
  useCreateProductMutation,
  useDeleteProductMutation,
  useImportProductsCsvMutation,
  useUpdateProductMutation
} from '../hooks/product.mutations'
import { productQueryKeys } from '../hooks/product.query'

vi.mock('../api/product.api', () => ({
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
  importProductsCsv: vi.fn()
}))

const createProductMock = vi.mocked(createProduct)
const updateProductMock = vi.mocked(updateProduct)
const deleteProductMock = vi.mocked(deleteProduct)
const importProductsCsvMock = vi.mocked(importProductsCsv)

const ID = '3f2b8c1e-1d2a-4c5b-9e6f-7a8b9c0d1e2f' as UUID
const KEY = '9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d' as UUID

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

const updateRequest: ProductUpdateRequest = {
  name: 'Wireless Mouse v2',
  category: 'Electronics',
  price: 29.99,
  stock: 100
}

const LIST_KEY = productQueryKeys.list({ name: 'mouse' })

const setup = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  queryClient.setQueryData(LIST_KEY, { content: [], pageDetails: {} })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const isListStale = () => queryClient.getQueryState(LIST_KEY)?.isInvalidated
  return { queryClient, wrapper, isListStale }
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('useCreateProductMutation', () => {
  it('creates with idempotency key, seeds the detail cache and invalidates lists', async () => {
    createProductMock.mockResolvedValue({ data: product, etag: '"3"', idempotencyKey: KEY, replayed: false })
    const { queryClient, wrapper, isListStale } = setup()
    const { result } = renderHook(() => useCreateProductMutation(), { wrapper })

    await act(() => result.current.mutateAsync({ product: { ...product }, idempotencyKey: KEY }))

    expect(createProductMock).toHaveBeenCalledWith({ ...product }, { idempotencyKey: KEY, signal: undefined })
    expect(queryClient.getQueryData(productQueryKeys.detail(ID))).toEqual({ data: product, etag: '"3"' })
    expect(isListStale()).toBe(true)
  })

  it('does not invalidate lists when creation fails', async () => {
    createProductMock.mockRejectedValue(new Error('409'))
    const { wrapper, isListStale } = setup()
    const { result } = renderHook(() => useCreateProductMutation(), { wrapper })

    await act(() => expect(result.current.mutateAsync({ product: { ...product } })).rejects.toThrow('409'))

    expect(isListStale()).toBe(false)
  })
})

describe('useUpdateProductMutation', () => {
  it('sends the cached ETag as If-Match (by SKU) and stores the new version', async () => {
    const updated = { ...product, ...updateRequest, version: 4 }
    updateProductMock.mockResolvedValue({ data: updated, etag: '"4"' })
    const { queryClient, wrapper, isListStale } = setup()
    queryClient.setQueryData(productQueryKeys.detail(ID), { data: product, etag: '"3"' })
    const { result } = renderHook(() => useUpdateProductMutation(), { wrapper })

    await act(() => result.current.mutateAsync({ idOrSku: product.sku, product: updateRequest }))

    expect(updateProductMock).toHaveBeenCalledWith(product.sku, updateRequest, { ifMatch: '"3"', signal: undefined })
    expect(queryClient.getQueryData(productQueryKeys.detail(ID))).toEqual({ data: updated, etag: '"4"' })
    expect(isListStale()).toBe(true)
  })

  it('prefers an explicit If-Match and sends none when nothing is cached', async () => {
    updateProductMock.mockResolvedValue({ data: product, etag: '"3"' })
    const { wrapper } = setup()
    const { result } = renderHook(() => useUpdateProductMutation(), { wrapper })

    await act(() => result.current.mutateAsync({ idOrSku: ID, product: updateRequest, ifMatch: '"7"' }))
    await act(() => result.current.mutateAsync({ idOrSku: 'OTHER', product: updateRequest }))

    expect(updateProductMock).toHaveBeenNthCalledWith(1, ID, updateRequest, { ifMatch: '"7"', signal: undefined })
    expect(updateProductMock).toHaveBeenNthCalledWith(2, 'OTHER', updateRequest, {
      ifMatch: undefined,
      signal: undefined
    })
  })

  it('invalidates the cached detail when the update fails (e.g. 412 stale ETag)', async () => {
    updateProductMock.mockRejectedValue(new Error('412'))
    const { queryClient, wrapper, isListStale } = setup()
    queryClient.setQueryData(productQueryKeys.detail(ID), { data: product, etag: '"3"' })
    const { result } = renderHook(() => useUpdateProductMutation(), { wrapper })

    await act(() => expect(result.current.mutateAsync({ idOrSku: ID, product: updateRequest })).rejects.toThrow())

    expect(queryClient.getQueryState(productQueryKeys.detail(ID))?.isInvalidated).toBe(true)
    expect(isListStale()).toBe(false)
  })
})

describe('useDeleteProductMutation', () => {
  it.each([
    ['id', ID],
    ['sku', product.sku]
  ])('removes the cached detail by %s and invalidates lists', async (_label, idOrSku) => {
    deleteProductMock.mockResolvedValue()
    const { queryClient, wrapper, isListStale } = setup()
    queryClient.setQueryData(productQueryKeys.detail(ID), { data: product, etag: '"3"' })
    const { result } = renderHook(() => useDeleteProductMutation(), { wrapper })

    await act(() => result.current.mutateAsync({ idOrSku }))

    expect(deleteProductMock).toHaveBeenCalledWith(idOrSku, undefined)
    expect(queryClient.getQueryData(productQueryKeys.detail(ID))).toBeUndefined()
    expect(isListStale()).toBe(true)
  })
})

describe('useImportProductsCsvMutation', () => {
  it('uploads the file and invalidates lists and details', async () => {
    const report: CsvImportReport = {
      totalRows: 1,
      insertedRows: 0,
      updatedRows: 1,
      rejectedRows: 0,
      errorsTruncated: false,
      aborted: false,
      durationMs: 10,
      errors: []
    }
    importProductsCsvMock.mockResolvedValue({ data: report, idempotencyKey: KEY, replayed: false })
    const { queryClient, wrapper, isListStale } = setup()
    queryClient.setQueryData(productQueryKeys.detail(ID), { data: product, etag: '"3"' })
    const file = new Blob(['sku,name'], { type: 'text/csv' })
    const { result } = renderHook(() => useImportProductsCsvMutation(), { wrapper })

    let returned: Awaited<ReturnType<typeof importProductsCsv>> | undefined
    await act(async () => {
      returned = await result.current.mutateAsync({ file, idempotencyKey: KEY })
    })

    expect(importProductsCsvMock).toHaveBeenCalledWith(file, { idempotencyKey: KEY, signal: undefined })
    expect(returned?.data).toEqual(report)
    expect(isListStale()).toBe(true)
    expect(queryClient.getQueryState(productQueryKeys.detail(ID))?.isInvalidated).toBe(true)
  })

  it('also refreshes the products when the import fails, since some batches may have been saved', async () => {
    importProductsCsvMock.mockRejectedValue(new Error('Network Error'))
    const { wrapper, isListStale } = setup()
    const { result } = renderHook(() => useImportProductsCsvMutation(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ file: new Blob(['x']) }).catch(() => {})
    })

    expect(isListStale()).toBe(true)
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
