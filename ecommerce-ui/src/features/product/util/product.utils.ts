import type { ProductFilterParams } from '../api/product.dto'
import type { Product, ProductFilters, ProductInput } from '../types/product.types'
import type { ProductFormValues } from '../model/product.schema'

export const truncate = (text: string | null, max = 50): string => {
  if (!text) return '—'
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text
}

export const toFormValues = (product?: Product): ProductFormValues => ({
  sku: product?.sku ?? '',
  name: product?.name ?? '',
  description: product?.description ?? '',
  category: product?.category ?? '',
  price: product ? String(product.price) : '',
  stock: product ? String(product.stock) : '',
  weightKg: product?.weightKg != null ? String(product.weightKg) : ''
})

export const toProductInput = (values: ProductFormValues): ProductInput => ({
  sku: values.sku.trim(),
  name: values.name.trim(),
  description: values.description.trim() === '' ? null : values.description.trim(),
  category: values.category.trim(),
  price: Number(values.price),
  stock: Number(values.stock),
  weightKg: values.weightKg.trim() === '' ? null : Number(values.weightKg)
})

type AppliedProductFilters = {
  readonly applied: ProductFilters
  readonly minStock?: number
  readonly page: number
  readonly pageSize: number
}

const toOptionalText = (value: string): string | undefined => value.trim() || undefined

const toOptionalNumber = (value: string): number | undefined => {
  if (value.trim() === '') return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

export const toProductFilterParams = ({ applied, minStock, page, pageSize }: AppliedProductFilters): ProductFilterParams => ({
  name: toOptionalText(applied.name),
  sku: toOptionalText(applied.sku),
  category: toOptionalText(applied.category),
  minPrice: toOptionalNumber(applied.priceMin),
  maxPrice: toOptionalNumber(applied.priceMax),
  minStock,
  minWeightKg: toOptionalNumber(applied.weightMin),
  maxWeightKg: toOptionalNumber(applied.weightMax),
  page,
  size: pageSize
})
