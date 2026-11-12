import { MainLayout } from './app/layouts/MainLayout'
import { CART_DRAWER_WIDTH, CartDrawer } from './features/cart/components/CartDrawer'
import { selectCartCount, useCartStore } from './features/cart/stores/cart.store'
import { ProductsPage } from './features/product/components/ProductsPage'
import { useProductSearchActions } from './features/product/hooks/product-search'

function App() {
  const { search } = useProductSearchActions()
  const cartCount = useCartStore(selectCartCount)
  const cartItems = useCartStore((state) => state.items)
  const isCartOpen = useCartStore((state) => state.isOpen)
  const toggleCart = useCartStore((state) => state.toggleOpen)
  const addToCart = useCartStore((state) => state.addItem)

  return (
    <MainLayout
      onSearch={search}
      cartCount={cartCount}
      onCartClick={toggleCart}
      aside={<CartDrawer />}
      asideWidth={isCartOpen ? CART_DRAWER_WIDTH : 0}
    >
      <ProductsPage
        onAddToCart={addToCart}
        canAddToCart={(product) =>
          product.stock > (cartItems.find((item) => item.productId === product.id)?.quantity ?? 0)
        }
      />
    </MainLayout>
  )
}

export default App
