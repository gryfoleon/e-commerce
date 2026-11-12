import type { ApiErrorResponse } from '../../../core/http/api-client'
import type { ProductFilterParams } from '../api/product.dto'
import type { Product, ProductFilters, ProductInput } from '../types/product.types'
import type { ProductFormValues } from '../model/product.schema'

export type CsvImportFailureEntry = {
  rows: number[]
  field: string | null
  rejectedValue: string | null
  message: string
}

export type CsvImportFailure = {
  message: string
  rejectedRows: number
  errors: CsvImportFailureEntry[]
}

const ROW_FIELD_PATTERN = /^row\[(\d+)\](?:\.(.+))?$/

const friendlyImportMessage = (message: string): string => {
  const summary = message
    .split('\n')[0]
    .replace(/^Batch persistence failed:\s*/i, '')
    .replace(/^ERROR:\s*/i, '')
    .trim()
  const constraint = /violates check constraint "([^"]+)"/i.exec(summary)?.[1]
  if (constraint) return `The row could not be saved: it violates the "${constraint}" rule.`
  const uniqueConstraint = /violates unique constraint "([^"]+)"/i.exec(summary)?.[1]
  if (uniqueConstraint) return `The row could not be saved: it duplicates an existing value ("${uniqueConstraint}").`
  return summary
}

export const toCsvImportFailure = (apiError: ApiErrorResponse | null, fallback: string): CsvImportFailure => {
  const groups = new Map<string, CsvImportFailureEntry>()
  const rejected = new Set<number>()
  for (const detail of apiError?.errorDetails ?? []) {
    const match = detail.field ? ROW_FIELD_PATTERN.exec(detail.field) : null
    const row = match ? Number(match[1]) : null
    const field = match ? (match[2] ?? null) : (detail.field ?? null)
    const rejectedValue = detail.rejectedValue ?? null
    const message = friendlyImportMessage(detail.message)
    const key = JSON.stringify([field, rejectedValue, message])
    const entry = groups.get(key) ?? { rows: [], field, rejectedValue, message }
    if (row !== null) {
      rejected.add(row)
      if (!entry.rows.includes(row)) entry.rows.push(row)
    }
    groups.set(key, entry)
  }
  const errors = [...groups.values()].sort((a, b) => (a.rows[0] ?? 0) - (b.rows[0] ?? 0))
  return { message: apiError?.error.replace(/^\w+Exception:\s*/, '') || fallback, rejectedRows: rejected.size, errors }
}

export const formatRowRanges = (rows: readonly number[]): string => {
  const sorted = [...rows].sort((a, b) => a - b)
  const ranges: string[] = []
  for (let i = 0; i < sorted.length; i++) {
    const start = sorted[i]
    while (i + 1 < sorted.length && sorted[i + 1] === sorted[i] + 1) i++
    ranges.push(start === sorted[i] ? String(start) : `${start}–${sorted[i]}`)
  }
  return ranges.join(', ')
}

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
