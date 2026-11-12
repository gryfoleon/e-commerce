import { EMPTY_FILTERS } from '../stores/product-filter.store'
import { toProductFilterParams, truncate } from '../util/product.utils'

describe('truncate', () => {
  it('keeps short text and cuts at 50 chars', () => {
    expect(truncate('short')).toBe('short')
    expect(truncate('a'.repeat(80))).toBe(`${'a'.repeat(50)}…`)
    expect(truncate(null)).toBe('—')
  })
})

describe('toProductFilterParams', () => {
  it('sends only filled filters plus paging', () => {
    expect(toProductFilterParams({ applied: EMPTY_FILTERS, page: 0, pageSize: 15 })).toEqual({ page: 0, size: 15 })
  })

  it('trims text, parses numbers and includes minStock', () => {
    const params = toProductFilterParams({
      applied: { ...EMPTY_FILTERS, name: ' mouse ', sku: 'ELEC', category: 'Audio', priceMin: '5', priceMax: 'x', weightMax: '2' },
      minStock: 0,
      page: 2,
      pageSize: 25
    })
    expect(params).toEqual({
      name: 'mouse',
      sku: 'ELEC',
      category: 'Audio',
      minPrice: 5,
      minStock: 0,
      maxWeightKg: 2,
      page: 2,
      size: 25
    })
  })
})
