import { apiClient } from '../../../core/http/api-client'
import { categoryListParamsSchema, categoryPageSchema, categoryResponseSchema } from '../model/category.schema'
import type { CategoryDto, CategoryId, CategoryListParams, CategoryPage } from './category.dto'

export const CATEGORIES_ENDPOINT = '/v1/categories'

export const categoryApiClient = apiClient

const toQueryParams = (params: CategoryListParams): CategoryListParams =>
  categoryListParamsSchema.parse(
    Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null))
  )

export const getCategories = async (params: CategoryListParams = {}, signal?: AbortSignal): Promise<CategoryPage> => {
  const response = await categoryApiClient.get<unknown>(CATEGORIES_ENDPOINT, {
    params: toQueryParams(params),
    validateStatus: (status) => status === 200,
    signal
  })
  return categoryPageSchema.parse(response.data)
}

export const getCategoryById = async (id: CategoryId, signal?: AbortSignal): Promise<CategoryDto> => {
  const response = await categoryApiClient.get<unknown>(`${CATEGORIES_ENDPOINT}/${id}`, {
    validateStatus: (status) => status === 200,
    signal
  })
  return categoryResponseSchema.parse(response.data)
}

export const getCategoryByName = async (name: string, signal?: AbortSignal): Promise<CategoryDto> => {
  const response = await categoryApiClient.get<unknown>(`${CATEGORIES_ENDPOINT}/name/${encodeURIComponent(name)}`, {
    validateStatus: (status) => status === 200,
    signal
  })
  return categoryResponseSchema.parse(response.data)
}
