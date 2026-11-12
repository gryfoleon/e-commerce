import type { z } from 'zod'
import type {
  categoryListParamsSchema,
  categoryPageSchema,
  categoryResponseSchema,
  pageDetailsSchema
} from '../model/category.schema'

export type CategoryDto = z.infer<typeof categoryResponseSchema>

export type CategoryId = CategoryDto['id']

export type CategoryPageDetails = z.infer<typeof pageDetailsSchema>

export type CategoryPage = z.infer<typeof categoryPageSchema>

export type CategoryListParams = z.infer<typeof categoryListParamsSchema>

export type CategorySortBy = NonNullable<CategoryListParams['sortBy']>

export type CategorySortDirection = NonNullable<CategoryListParams['sortDirection']>
