export type UUID = ReturnType<typeof crypto.randomUUID>

export type ProductDto = {
  id: UUID
  sku: string
  name: string
  description: string | null
  category: string
  price: number
  stock: number
  weightKg: number | null
  version: number
  createdAt: string
  updatedAt: string
}

export type ProductSearchDto = Omit<ProductDto, 'version' | 'createdAt' | 'updatedAt'>

export type PageDetails = {
  page: number
  size: number
  totalPages: number
  totalElements: number
  isLast: boolean
}

export type PageableResponse<T> = {
  content: T[]
  pageDetails: PageDetails
}

export type ProductCreateRequest = {
  sku: string
  name: string
  description?: string | null
  category: string
  price: number
  stock: number
  weightKg?: number | null
}

export type ProductUpdateRequest = Omit<ProductCreateRequest, 'sku'> & { sku?: string }

export type ProductSortBy = 'createdAt' | 'price' | 'name'

export type SortDirection = 'ASC' | 'DESC'

export type ProductFilterParams = {
  name?: string
  sku?: string
  category?: string
  minPrice?: number
  maxPrice?: number
  minStock?: number
  minWeightKg?: number
  maxWeightKg?: number
  page?: number
  size?: number
  sortBy?: ProductSortBy
  sortDirection?: SortDirection
}

export type CsvImportError = {
  line: number
  sku: string | null
  field: string | null
  rejectedValue: string | null
  message: string
}

export type CsvImportReport = {
  totalRows: number
  insertedRows: number
  updatedRows: number
  rejectedRows: number
  errorsTruncated: boolean
  aborted: boolean
  durationMs: number
  errors: CsvImportError[]
}

export type WithETag<T> = {
  data: T
  etag: string | null
}

export type IdempotentResult<T> = WithETag<T> & {
  idempotencyKey: UUID
  replayed: boolean
}
