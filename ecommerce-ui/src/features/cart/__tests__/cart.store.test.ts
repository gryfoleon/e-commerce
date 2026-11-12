import { CART_STORAGE_KEY, type CartProduct, selectCartCount, useCartStore } from '../stores/cart.store'

const product = (id: string, price: number, stock = 5): CartProduct => ({
  id,
  sku: `SKU${id}`,
  name: `Product ${id}`,
  description: `Description ${id}`,
  price,
  stock
})

const state = () => useCartStore.getState()

describe('useCartStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useCartStore.setState({ items: [], total: 0, isOpen: false })
  })

  it('adds products, increments existing ones and computes subtotals and total', () => {
    state().addItem(product('1', 10.5))
    state().addItem(product('2', 3))
    state().addItem(product('1', 10.5))

    expect(state().items.map(({ productId, quantity, subtotal }) => ({ productId, quantity, subtotal }))).toEqual([
      { productId: '1', quantity: 2, subtotal: 21 },
      { productId: '2', quantity: 1, subtotal: 3 }
    ])
    expect(state().total).toBe(24)
    expect(selectCartCount(state())).toBe(3)
  })

  it('keeps money exact', () => {
    state().addItem(product('1', 0.1))
    state().addItem(product('2', 0.2))
    expect(state().total).toBe(0.3)
    state().increment('1')
    state().increment('1')
    expect(state().items[0].subtotal).toBe(0.3)
    expect(state().total).toBe(0.5)
  })

  it('caps quantities at the stock and ignores products without stock', () => {
    state().addItem(product('1', 1, 2))
    state().addItem(product('1', 1, 2))
    state().addItem(product('1', 1, 2))
    state().increment('1')
    expect(state().items[0].quantity).toBe(2)

    state().addItem(product('2', 1, 0))
    expect(state().items).toHaveLength(1)
  })

  it('never decrements below 1', () => {
    state().addItem(product('1', 4))
    state().increment('1')
    state().decrement('1')
    state().decrement('1')
    expect(state().items[0]).toMatchObject({ quantity: 1, subtotal: 4 })
    expect(state().total).toBe(4)
  })

  it('opens when the first product is added and closes when it becomes empty', () => {
    state().addItem(product('1', 1))
    expect(state().isOpen).toBe(true)
    state().toggleOpen()
    state().addItem(product('2', 1))
    expect(state().isOpen).toBe(false)
    state().toggleOpen()
    state().removeItem('1')
    expect(state().isOpen).toBe(true)
    state().removeItem('2')
    expect(state()).toMatchObject({ items: [], total: 0, isOpen: false })
  })

  it('can be opened and closed while empty', () => {
    state().toggleOpen()
    expect(state().isOpen).toBe(true)
    state().toggleOpen()
    expect(state().isOpen).toBe(false)
    state().setOpen(true)
    expect(state().isOpen).toBe(true)
  })

  it('stays open when the first product is added to an opened empty cart', () => {
    state().setOpen(true)
    state().addItem(product('1', 1))
    expect(state().isOpen).toBe(true)
  })

  it('clears all products', () => {
    state().addItem(product('1', 1))
    state().addItem(product('2', 2))
    state().clear()
    expect(state()).toMatchObject({ items: [], total: 0, isOpen: false })
  })

  it('persists the items and recomputes totals when rehydrating', async () => {
    state().addItem(product('1', 2.5))
    state().increment('1')
    const stored = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? '{}')
    expect(stored.state).toEqual({ items: state().items })

    const saved = localStorage.getItem(CART_STORAGE_KEY)!
    useCartStore.setState({ items: [], total: 0, isOpen: false })
    localStorage.setItem(CART_STORAGE_KEY, saved)
    await useCartStore.persist.rehydrate()
    expect(state()).toMatchObject({ total: 5, isOpen: true })
    expect(state().items[0]).toMatchObject({ productId: '1', quantity: 2, subtotal: 5 })
  })
})
