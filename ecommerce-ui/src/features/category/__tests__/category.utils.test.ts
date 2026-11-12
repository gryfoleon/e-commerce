import { describe, expect, it } from 'vitest'
import { withSelectedOption } from '../util/category.utils'

const options = [
  { value: 'Audio', label: 'Audio' },
  { value: 'Books', label: 'Books' }
]

describe('withSelectedOption', () => {
  it('returns the same options when the selected value is listed or empty', () => {
    expect(withSelectedOption(options, 'Books')).toBe(options)
    expect(withSelectedOption(options, '')).toBe(options)
    expect(withSelectedOption(options, null)).toBe(options)
  })

  it('appends a selected value missing from the catalog', () => {
    expect(withSelectedOption(options, 'Legacy')).toEqual([...options, { value: 'Legacy', label: 'Legacy' }])
  })
})
