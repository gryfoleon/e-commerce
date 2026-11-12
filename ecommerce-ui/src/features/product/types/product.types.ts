export type Product = {
  id: string
  sku: string
  name: string
  description: string | null
  category: string
  price: number
  stock: number
  weightKg: number | null
}

export type ProductInput = Omit<Product, 'id'>

export type StockState = 'any' | 'in-stock' | 'low' | 'out'

export type ProductFilters = {
  sku: string
  name: string
  category: string
  priceMin: string
  priceMax: string
  weightMin: string
  weightMax: string
  stockState: StockState
}
