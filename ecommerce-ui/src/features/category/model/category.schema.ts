import { z } from 'zod'

export const CATEGORY_PAGE_MAX = 1000
export const CATEGORY_PAGE_SIZE_MAX = 100

export const CATEGORY_SORT_FIELDS = ['name', 'createdAt'] as const
export const SORT_DIRECTIONS = ['ASC', 'DESC'] as const

export const categoryResponseSchema = z.object({
  id: z.number().int(),
  name: z.string(),
  createdAt: z.iso.datetime({ offset: true })
})

export const pageDetailsSchema = z.object({
  page: z.number().int().nonnegative(),
  size: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
  isLast: z.boolean()
})

export const categoryPageSchema = z.object({
  content: z.array(categoryResponseSchema),
  pageDetails: pageDetailsSchema
})

export const categoryListParamsSchema = z
  .object({
    page: z.number().int().min(0).max(CATEGORY_PAGE_MAX),
    size: z.number().int().min(1).max(CATEGORY_PAGE_SIZE_MAX),
    sortBy: z.enum(CATEGORY_SORT_FIELDS),
    sortDirection: z.enum(SORT_DIRECTIONS)
  })
  .partial()
  .strict()
