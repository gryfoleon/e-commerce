import type { StockState } from '../types/product.types'
import { useProductFilterStore } from '../stores/product-filter.store'

const applyStockState = (stockState: StockState) => {
  const { setDraft, applyFilters } = useProductFilterStore.getState()
  setDraft({ stockState })
  applyFilters()
  return useProductFilterStore.getState()
}

describe('useProductFilterStore minStock', () => {
  beforeEach(() => {
    useProductFilterStore.getState().clearFilters()
  })

  it.each([
    ['any', undefined],
    ['in-stock', 1],
    ['low', 10],
    ['out', 0]
  ] as const)('maps the %s stock state to minStock %s when applying', (stockState, minStock) => {
    const state = applyStockState(stockState)
    expect(state.applied.stockState).toBe(stockState)
    expect(state.minStock).toBe(minStock)
  })

  it('only updates minStock when filters are applied', () => {
    useProductFilterStore.getState().setDraft({ stockState: 'low' })
    expect(useProductFilterStore.getState().minStock).toBeUndefined()
  })

  it('clears minStock with the filters', () => {
    applyStockState('in-stock')
    useProductFilterStore.getState().clearFilters()
    expect(useProductFilterStore.getState().minStock).toBeUndefined()
  })
})
