import type { AxiosResponse } from 'axios'
import { apiClient } from '../../../core/http/api-client'
import type {
  CsvImportReport,
  IdempotentResult,
  PageableResponse,
  ProductCreateRequest,
  ProductDto,
  ProductFilterParams,
  ProductSearchDto,
  ProductUpdateRequest,
  UUID,
  WithETag
} from './product.dto'

export const PRODUCTS_ENDPOINT = '/v1/products'

export const productApiClient = apiClient

export type GetProductByIdResult = ({ notModified: false } & WithETag<ProductDto>) | { notModified: true }

export const formatETag = (version: number): string => `"${version}"`

const readETag = (response: AxiosResponse): string | null => {
  const etag: unknown = response.headers['etag']
  return typeof etag === 'string' ? etag : null
}

const isReplayed = (response: AxiosResponse): boolean =>
  String(response.headers['idempotent-replayed']).toLowerCase() === 'true'

const toQueryParams = (filters: ProductFilterParams): Partial<ProductFilterParams> =>
  Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined && value !== null && value !== '')
  ) as Partial<ProductFilterParams>

const BYPASS_HTTP_CACHE = {
  adapter: 'fetch',
  fetchOptions: { cache: 'no-store' },
  headers: { 'User-Agent': false }
} as const

export const searchProducts = async (
  filters: ProductFilterParams = {},
  signal?: AbortSignal
): Promise<PageableResponse<ProductSearchDto>> => {
  const response = await productApiClient.get<PageableResponse<ProductSearchDto>>(`${PRODUCTS_ENDPOINT}/filter`, {
    params: toQueryParams(filters),
    validateStatus: (status) => status === 200,
    signal,
    ...BYPASS_HTTP_CACHE
  })
  return response.data
}

export const getProductById = async (
  id: UUID,
  options: { ifNoneMatch?: string; signal?: AbortSignal } = {}
): Promise<GetProductByIdResult> => {
  const { ifNoneMatch, signal } = options
  const response = await productApiClient.get<ProductDto>(`${PRODUCTS_ENDPOINT}/${id}`, {
    headers: ifNoneMatch ? { 'If-None-Match': ifNoneMatch } : undefined,
    validateStatus: (status) => status === 200 || status === 304,
    signal
  })
  if (response.status === 304) return { notModified: true }
  return { notModified: false, data: response.data, etag: readETag(response) }
}

export const createProduct = async (
  product: ProductCreateRequest,
  options: { idempotencyKey?: UUID; signal?: AbortSignal } = {}
): Promise<IdempotentResult<ProductDto>> => {
  const { idempotencyKey = crypto.randomUUID(), signal } = options
  const response = await productApiClient.post<ProductDto>(PRODUCTS_ENDPOINT, product, {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey },
    validateStatus: (status) => status === 201,
    signal
  })
  return { data: response.data, etag: readETag(response), idempotencyKey, replayed: isReplayed(response) }
}

export const updateProduct = async (
  idOrSku: UUID | string,
  product: ProductUpdateRequest,
  options: { ifMatch?: string; signal?: AbortSignal } = {}
): Promise<WithETag<ProductDto>> => {
  const { ifMatch, signal } = options
  const response = await productApiClient.put<ProductDto>(
    `${PRODUCTS_ENDPOINT}/${encodeURIComponent(idOrSku)}`,
    product,
    {
      headers: { 'Content-Type': 'application/json', ...(ifMatch ? { 'If-Match': ifMatch } : {}) },
      validateStatus: (status) => status === 200,
      signal
    }
  )
  return { data: response.data, etag: readETag(response) }
}

export const deleteProduct = async (idOrSku: UUID | string, signal?: AbortSignal): Promise<void> => {
  await productApiClient.delete(`${PRODUCTS_ENDPOINT}/${encodeURIComponent(idOrSku)}`, {
    validateStatus: (status) => status === 204,
    signal
  })
}

export const IMPORT_CSV_TIMEOUT_MS = 10 * 60 * 1000

export const importProductsCsv = async (
  file: Blob,
  options: { idempotencyKey?: UUID; signal?: AbortSignal } = {}
): Promise<Omit<IdempotentResult<CsvImportReport>, 'etag'>> => {
  const { idempotencyKey = crypto.randomUUID(), signal } = options
  const formData = new FormData()
  formData.append('file', file)
  const response = await productApiClient.post<CsvImportReport>(`${PRODUCTS_ENDPOINT}/import-csv`, formData, {
    headers: { 'Idempotency-Key': idempotencyKey },
    validateStatus: (status) => status === 200,
    timeout: IMPORT_CSV_TIMEOUT_MS,
    signal
  })
  return { data: response.data, idempotencyKey, replayed: isReplayed(response) }
}
