import { create } from 'zustand'
import type { ProductFilterParams } from '../api/product.dto'
import type { ProductFilters, StockState } from '../types/product.types'

export const EMPTY_FILTERS: ProductFilters = {
  sku: '',
  name: '',
  category: '',
  priceMin: '',
  priceMax: '',
  weightMin: '',
  weightMax: '',
  stockState: 'any'
}

export const STOCK_STATE_MIN_STOCK: Readonly<Record<StockState, ProductFilterParams['minStock']>> = {
  any: undefined,
  'in-stock': 1,
  low: 10,
  out: 0
}

type ProductFilterState = {
  draft: ProductFilters
  applied: ProductFilters
  minStock: ProductFilterParams['minStock']
  page: number
  pageSize: number
  setDraft: (patch: Partial<ProductFilters>) => void
  applyFilters: () => void
  setSearch: (name: string) => void
  clearFilters: () => void
  setPage: (page: number) => void
  setPageSize: (pageSize: number) => void
}

export const useProductFilterStore = create<ProductFilterState>((set) => ({
  draft: EMPTY_FILTERS,
  applied: EMPTY_FILTERS,
  minStock: undefined,
  page: 0,
  pageSize: 15,
  setDraft: (patch) => set((s) => ({ draft: { ...s.draft, ...patch } })),
  applyFilters: () =>
    set((s) => ({ applied: { ...s.draft }, minStock: STOCK_STATE_MIN_STOCK[s.draft.stockState], page: 0 })),
  setSearch: (name) =>
    set((s) => ({ draft: { ...s.draft, name }, applied: { ...s.applied, name }, page: 0 })),
  clearFilters: () => set({ draft: EMPTY_FILTERS, applied: EMPTY_FILTERS, minStock: undefined, page: 0 }),
  setPage: (page) => set({ page }),
  setPageSize: (pageSize) => set({ pageSize, page: 0 })
}))
