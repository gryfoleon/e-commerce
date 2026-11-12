import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getApiError } from '../../../core/http/api-client'
import {
  createProduct,
  deleteProduct,
  formatETag,
  getProductById,
  IMPORT_CSV_TIMEOUT_MS,
  importProductsCsv,
  PRODUCTS_ENDPOINT,
  productApiClient,
  searchProducts,
  updateProduct
} from '../api/product.api'
import type { ProductDto, UUID } from '../api/product.dto'

const ID = '3f2b8c1e-1d2a-4c5b-9e6f-7a8b9c0d1e2f' as UUID

const product: ProductDto = {
  id: ID,
  sku: 'ELECMS001',
  name: 'Wireless Mouse',
  description: 'Ergonomic 2.4GHz wireless mouse',
  category: 'Electronics',
  price: 24.99,
  stock: 150,
  weightKg: 0.12,
  version: 3,
  createdAt: '2026-11-10T00:00:00Z',
  updatedAt: '2026-11-10T00:00:00Z'
}

const response = <T>(data: T, status = 200, headers: Record<string, string> = {}): AxiosResponse<T> =>
  ({ data, status, statusText: '', headers, config: { headers: new AxiosHeaders() } }) as AxiosResponse<T>

type RequestConfig = {
  params?: Record<string, unknown>
  headers?: Record<string, string>
  validateStatus: (status: number) => boolean
  signal?: AbortSignal
}

const configOf = (mock: { mock: { calls: unknown[][] } }, argIndex: number): RequestConfig =>
  mock.mock.calls[0][argIndex] as RequestConfig

afterEach(() => {
  vi.restoreAllMocks()
})

describe('product.api', () => {
  describe('searchProducts', () => {
    it('calls GET /filter dropping empty params and returns the page', async () => {
      const page = { content: [], pageDetails: { page: 0, size: 20, totalPages: 0, totalElements: 0, isLast: true } }
      const get = vi.spyOn(productApiClient, 'get').mockResolvedValue(response(page))
      const controller = new AbortController()

      const result = await searchProducts(
        { name: 'camp tent', sku: '', category: undefined, minPrice: 0, sortBy: 'price', sortDirection: 'ASC' },
        controller.signal
      )

      expect(result).toEqual(page)
      expect(get).toHaveBeenCalledWith(`${PRODUCTS_ENDPOINT}/filter`, expect.anything())
      const config = configOf(get, 1)
      expect(config.params).toEqual({ name: 'camp tent', minPrice: 0, sortBy: 'price', sortDirection: 'ASC' })
      expect(config.signal).toBe(controller.signal)
      expect(config.validateStatus(200)).toBe(true)
      expect(config.validateStatus(304)).toBe(false)
    })

    it('skips the browser HTTP cache without adding headers that would trigger a CORS preflight', async () => {
      const page = { content: [], pageDetails: { page: 0, size: 20, totalPages: 0, totalElements: 0, isLast: true } }
      const fetchMock = vi.fn(async (..._args: Parameters<typeof fetch>) => Response.json(page))
      vi.stubGlobal('fetch', fetchMock)

      await expect(searchProducts({ size: 20 })).resolves.toEqual(page)

      expect(fetchMock).toHaveBeenCalledTimes(1)
      const [input, init] = fetchMock.mock.calls[0]
      const request = input instanceof Request ? input : new Request(input, init)
      expect(request.url).toMatch(/\/v1\/products\/filter\?size=20$/)
      expect(init?.cache ?? request.cache).toBe('no-store')
      expect([...request.headers.keys()]).toEqual(['accept'])
      vi.unstubAllGlobals()
    })
  })

  describe('getProductById', () => {
    it('returns the product with its ETag', async () => {
      const get = vi.spyOn(productApiClient, 'get').mockResolvedValue(response(product, 200, { etag: '"3"' }))

      const result = await getProductById(ID)

      expect(result).toEqual({ notModified: false, data: product, etag: '"3"' })
      expect(get).toHaveBeenCalledWith(`${PRODUCTS_ENDPOINT}/${ID}`, expect.anything())
      expect(configOf(get, 1).headers).toBeUndefined()
    })

    it('sends If-None-Match and handles 304 Not Modified', async () => {
      const get = vi.spyOn(productApiClient, 'get').mockResolvedValue(response('', 304))

      const result = await getProductById(ID, { ifNoneMatch: formatETag(3) })

      expect(result).toEqual({ notModified: true })
      const config = configOf(get, 1)
      expect(config.headers).toEqual({ 'If-None-Match': '"3"' })
      expect(config.validateStatus(304)).toBe(true)
      expect(config.validateStatus(404)).toBe(false)
    })
  })

  describe('createProduct', () => {
    const body = {
      sku: 'ELECMS001',
      name: 'Wireless Mouse',
      category: 'Electronics',
      price: 24.99,
      stock: 150
    }

    it('generates an Idempotency-Key and expects 201', async () => {
      const post = vi.spyOn(productApiClient, 'post').mockResolvedValue(response(product, 201, { etag: '"0"' }))

      const result = await createProduct(body)

      expect(post).toHaveBeenCalledWith(PRODUCTS_ENDPOINT, body, expect.anything())
      const config = configOf(post, 2)
      const key = config.headers?.['Idempotency-Key']
      expect(key).toMatch(/^[0-9a-f-]{36}$/)
      expect(config.validateStatus(201)).toBe(true)
      expect(config.validateStatus(200)).toBe(false)
      expect(result).toEqual({ data: product, etag: '"0"', idempotencyKey: key, replayed: false })
    })

    it('reuses the provided Idempotency-Key and detects replays', async () => {
      const post = vi
        .spyOn(productApiClient, 'post')
        .mockResolvedValue(response(product, 201, { 'idempotent-replayed': 'true' }))

      const result = await createProduct(body, { idempotencyKey: ID })

      expect(configOf(post, 2).headers?.['Idempotency-Key']).toBe(ID)
      expect(result.replayed).toBe(true)
      expect(result.etag).toBeNull()
    })
  })

  describe('updateProduct', () => {
    it('sends PUT with If-Match and returns the new ETag', async () => {
      const put = vi.spyOn(productApiClient, 'put').mockResolvedValue(response(product, 200, { etag: '"4"' }))
      const body = { name: 'Wireless Mouse Pro', category: 'Electronics', price: 29.99, stock: 90 }

      const result = await updateProduct('ELECMS001', body, { ifMatch: '"3"' })

      expect(put).toHaveBeenCalledWith(`${PRODUCTS_ENDPOINT}/ELECMS001`, body, expect.anything())
      const config = configOf(put, 2)
      expect(config.headers).toEqual({ 'Content-Type': 'application/json', 'If-Match': '"3"' })
      expect(config.validateStatus(200)).toBe(true)
      expect(result).toEqual({ data: product, etag: '"4"' })
    })

    it('omits If-Match when not provided', async () => {
      const put = vi.spyOn(productApiClient, 'put').mockResolvedValue(response(product))

      await updateProduct(ID, { name: 'x', category: 'Electronics', price: 1, stock: 0 })

      expect(configOf(put, 2).headers).toEqual({ 'Content-Type': 'application/json' })
    })
  })

  describe('deleteProduct', () => {
    it('sends DELETE and expects 204', async () => {
      const del = vi.spyOn(productApiClient, 'delete').mockResolvedValue(response('', 204))

      await expect(deleteProduct(ID)).resolves.toBeUndefined()

      expect(del).toHaveBeenCalledWith(`${PRODUCTS_ENDPOINT}/${ID}`, expect.anything())
      const config = configOf(del, 1)
      expect(config.validateStatus(204)).toBe(true)
      expect(config.validateStatus(200)).toBe(false)
    })
  })

  describe('importProductsCsv', () => {
    it('posts the file as multipart form data', async () => {
      const report = {
        totalRows: 1,
        insertedRows: 1,
        updatedRows: 0,
        rejectedRows: 0,
        errorsTruncated: false,
        aborted: false,
        durationMs: 5,
        errors: []
      }
      const post = vi.spyOn(productApiClient, 'post').mockResolvedValue(response(report))
      const file = new File(['name,description,sku,category,price,stock,weight_kg'], 'products.csv', {
        type: 'text/csv'
      })

      const result = await importProductsCsv(file, { idempotencyKey: ID })

      const [url, formData] = post.mock.calls[0] as [string, FormData]
      expect(url).toBe(`${PRODUCTS_ENDPOINT}/import-csv`)
      expect(formData.get('file')).toBeInstanceOf(File)
      const config = configOf(post, 2)
      expect(config.headers).toEqual({ 'Idempotency-Key': ID })
      expect(config.validateStatus(200)).toBe(true)
      expect((config as { timeout?: number }).timeout).toBe(IMPORT_CSV_TIMEOUT_MS)
      expect(result).toEqual({ data: report, idempotencyKey: ID, replayed: false })
    })
  })

  describe('errors', () => {
    it('propagates axios errors with the backend error body', async () => {
      const body = { statusCode: 404, response: 'Not Found', timestamp: '', error: 'Product not found' }
      const error = new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, response(body, 404))
      vi.spyOn(productApiClient, 'get').mockRejectedValue(error)

      const caught = await getProductById(ID).catch((e: unknown) => e)

      expect(getApiError(caught)).toEqual(body)
      expect(getApiError(new Error('boom'))).toBeNull()
    })
  })
})
