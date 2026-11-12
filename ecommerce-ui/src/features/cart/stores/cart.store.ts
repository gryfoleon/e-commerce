import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

export type CartProduct = {
  readonly id: string
  readonly sku: string
  readonly name: string
  readonly description: string | null
  readonly price: number
  readonly stock: number
}

export type CartItem = {
  readonly productId: string
  readonly sku: string
  readonly name: string
  readonly description: string | null
  readonly unitPrice: number
  readonly stock: number
  readonly quantity: number
  readonly subtotal: number
}

type CartState = {
  items: CartItem[]
  total: number
  isOpen: boolean
  addItem: (product: CartProduct) => void
  increment: (productId: string) => void
  decrement: (productId: string) => void
  removeItem: (productId: string) => void
  clear: () => void
  toggleOpen: () => void
  setOpen: (open: boolean) => void
}

const toCents = (amount: number) => Math.round(amount * 100)

const withQuantity = (item: Omit<CartItem, 'quantity' | 'subtotal'>, quantity: number): CartItem => ({
  ...item,
  quantity,
  subtotal: (toCents(item.unitPrice) * quantity) / 100
})

const totalOf = (items: readonly CartItem[]) => items.reduce((cents, item) => cents + toCents(item.subtotal), 0) / 100

const withItems = (items: CartItem[], isOpen: boolean) => ({
  items,
  total: totalOf(items),
  isOpen: items.length > 0 && isOpen
})

const updateQuantity = (items: CartItem[], productId: string, change: (item: CartItem) => number) =>
  items.map((item) => {
    if (item.productId !== productId) return item
    const quantity = Math.min(Math.max(change(item), 1), item.stock)
    return quantity === item.quantity ? item : withQuantity(item, quantity)
  })

export const CART_STORAGE_KEY = 'ecommerce-cart'

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      total: 0,
      isOpen: false,
      addItem: (product) =>
        set((state) => {
          if (product.stock <= 0) return state
          const existing = state.items.find((item) => item.productId === product.id)
          if (existing) return withItems(updateQuantity(state.items, product.id, (item) => item.quantity + 1), state.isOpen)
          const item = withQuantity(
            {
              productId: product.id,
              sku: product.sku,
              name: product.name,
              description: product.description,
              unitPrice: product.price,
              stock: product.stock
            },
            1
          )
          return withItems([...state.items, item], state.isOpen || state.items.length === 0)
        }),
      increment: (productId) =>
        set((state) => withItems(updateQuantity(state.items, productId, (item) => item.quantity + 1), state.isOpen)),
      decrement: (productId) =>
        set((state) => withItems(updateQuantity(state.items, productId, (item) => item.quantity - 1), state.isOpen)),
      removeItem: (productId) =>
        set((state) => withItems(state.items.filter((item) => item.productId !== productId), state.isOpen)),
      clear: () => set(withItems([], false)),
      toggleOpen: () => set((state) => ({ isOpen: !state.isOpen })),
      setOpen: (open) => set({ isOpen: open })
    }),
    {
      name: CART_STORAGE_KEY,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ items: state.items }),
      merge: (persisted, current) => {
        const stored = (persisted as Partial<Pick<CartState, 'items'>> | undefined)?.items ?? []
        const items = stored.map((item) => withQuantity(item, item.quantity))
        return { ...current, ...withItems(items, true) }
      }
    }
  )
)

export const selectCartCount = (state: Pick<CartState, 'items'>) =>
  state.items.reduce((count, item) => count + item.quantity, 0)

export const selectCartQuantity = (productId: string) => (state: Pick<CartState, 'items'>) =>
  state.items.find((item) => item.productId === productId)?.quantity ?? 0
