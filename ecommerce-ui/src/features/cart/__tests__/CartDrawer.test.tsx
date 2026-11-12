import { ThemeProvider } from '@mui/material/styles'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import App from '../../../App'
import { appTheme } from '../../../app/theme/theme'
import { API_BASE_URL } from '../../../core/http/api-client'
import { server } from '../../../mocks/node'
import { PRODUCTS_ENDPOINT } from '../../product/api/product.api'
import type { ProductSearchDto, UUID } from '../../product/api/product.dto'
import { useProductFilterStore } from '../../product/stores/product-filter.store'
import { useCartStore } from '../stores/cart.store'

const product = (n: number, price: number, stock: number): ProductSearchDto => ({
  id: `00000000-0000-4000-8000-${String(n).padStart(12, '0')}` as UUID,
  sku: `ELEC${String(n).padStart(3, '0')}`,
  name: `Product ${n}`,
  description: `Description ${n}`,
  category: 'Electronics',
  price,
  stock,
  weightKg: null
})

const PRODUCTS = [product(1, 10.5, 5), product(2, 3.25, 2), product(3, 99, 0)]

const renderApp = () => {
  server.use(
    http.get(`${API_BASE_URL}${PRODUCTS_ENDPOINT}/filter`, () =>
      HttpResponse.json({
        content: PRODUCTS,
        pageDetails: { page: 0, size: 15, totalPages: 1, totalElements: PRODUCTS.length, isLast: true }
      })
    )
  )
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ThemeProvider theme={appTheme}>
        <App />
      </ThemeProvider>
    </QueryClientProvider>
  )
}

const cart = () => screen.getByRole('complementary', { name: 'Shopping cart' })
const queryCart = () => screen.queryByRole('complementary', { name: 'Shopping cart' })
const cartButton = () => screen.getByRole('button', { name: /^Shopping cart, \d+ items$/ })
const addToCart = async (name: string) => userEvent.click(await screen.findByRole('button', { name: `Add ${name} to cart` }))
const total = () => within(cart()).getByLabelText('Cart total')

describe('Shopping cart', () => {
  beforeEach(() => {
    localStorage.clear()
    useCartStore.setState({ items: [], total: 0, isOpen: false })
    useProductFilterStore.getState().clearFilters()
  })

  it('is hidden while empty and opens when a product is added', async () => {
    renderApp()
    await screen.findByText('ELEC001')
    expect(queryCart()).not.toBeInTheDocument()

    await addToCart('Product 1')
    expect(cart()).toBeVisible()
    expect(within(cart()).getByText('Product 1')).toBeInTheDocument()
    expect(total()).toHaveTextContent('$10.50')
    expect(screen.getByRole('button', { name: 'Shopping cart, 1 items' })).toBeInTheDocument()
  })

  it('shows the empty cart message when opened while empty, replaced by the list when adding products', async () => {
    renderApp()
    await screen.findByText('ELEC001')
    await userEvent.click(cartButton())

    expect(cart()).toBeVisible()
    expect(within(cart()).getByText('Your cart is empty')).toBeInTheDocument()
    expect(within(cart()).queryByRole('list', { name: 'Cart items' })).not.toBeInTheDocument()
    expect(within(cart()).getByRole('button', { name: 'Checkout' })).toBeDisabled()
    expect(within(cart()).getByRole('button', { name: 'Clear cart' })).toBeDisabled()
    expect(total()).toHaveTextContent('$0.00')

    await addToCart('Product 1')
    expect(within(cart()).queryByText('Your cart is empty')).not.toBeInTheDocument()
    expect(within(cart()).getByRole('list', { name: 'Cart items' })).toBeInTheDocument()
    expect(within(cart()).getByText('Product 1')).toBeInTheDocument()
    expect(within(cart()).getByRole('button', { name: 'Checkout' })).toBeEnabled()

    await userEvent.click(cartButton())
    await waitFor(() => expect(queryCart()).not.toBeInTheDocument())
  })

  it('toggles the panel with the cart button', async () => {
    renderApp()
    await addToCart('Product 1')
    await userEvent.click(cartButton())
    await waitFor(() => expect(queryCart()).not.toBeInTheDocument())
    await userEvent.click(cartButton())
    expect(cart()).toBeVisible()
  })

  it('updates quantity, subtotal and total with the + and - buttons', async () => {
    renderApp()
    await addToCart('Product 1')
    await addToCart('Product 2')
    const row = within(cart()).getByRole('listitem', { name: 'Product 1' })

    await userEvent.click(within(row).getByRole('button', { name: 'Increase quantity of Product 1' }))
    await userEvent.click(within(row).getByRole('button', { name: 'Increase quantity of Product 1' }))
    expect(within(row).getByLabelText('Quantity of Product 1')).toHaveTextContent('3')
    expect(within(row).getByLabelText('Subtotal of Product 1')).toHaveTextContent('$31.50')
    expect(total()).toHaveTextContent('$34.75')

    await userEvent.click(within(row).getByRole('button', { name: 'Decrease quantity of Product 1' }))
    expect(within(row).getByLabelText('Quantity of Product 1')).toHaveTextContent('2')
    expect(total()).toHaveTextContent('$24.25')
    expect(cartButton()).toHaveAccessibleName('Shopping cart, 3 items')
  })

  it('limits the quantity to the available stock', async () => {
    renderApp()
    expect(await screen.findByRole('button', { name: 'Add Product 3 to cart' })).toBeDisabled()
    await addToCart('Product 2')
    await addToCart('Product 2')
    expect(screen.getByRole('button', { name: 'Add Product 2 to cart' })).toBeDisabled()
    expect(within(cart()).getByRole('button', { name: 'Increase quantity of Product 2' })).toBeDisabled()
  })

  it('asks before removing a product when its quantity reaches zero', async () => {
    renderApp()
    await addToCart('Product 1')
    await addToCart('Product 2')
    const decrease = () => within(cart()).getByRole('button', { name: 'Decrease quantity of Product 1' })

    await userEvent.click(decrease())
    let dialog = screen.getByRole('dialog', { name: 'Remove product' })
    expect(within(dialog).getByText('Are you sure you want to stop buying this product?')).toBeInTheDocument()
    expect(within(dialog).getByText('Product 1')).toBeInTheDocument()
    expect(within(dialog).getByText('Description 1')).toBeInTheDocument()
    expect(within(dialog).getByText('Unit price: $10.50')).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(within(cart()).getByLabelText('Quantity of Product 1')).toHaveTextContent('1')

    await userEvent.click(decrease())
    dialog = screen.getByRole('dialog', { name: 'Remove product' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Remove' }))
    await waitFor(() => expect(within(cart()).queryByText('Product 1')).not.toBeInTheDocument())
    expect(total()).toHaveTextContent('$3.25')
  })

  it('clears the cart after confirming', async () => {
    renderApp()
    await addToCart('Product 1')
    await addToCart('Product 2')
    await userEvent.click(within(cart()).getByRole('button', { name: 'Clear cart' }))
    const dialog = screen.getByRole('dialog', { name: 'Clear cart' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Clear cart' }))

    await waitFor(() => expect(queryCart()).not.toBeInTheDocument())
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(useCartStore.getState().items).toEqual([])
    expect(cartButton()).toHaveAccessibleName('Shopping cart, 0 items')
  })

  it('checks out, shows the success modal and empties the cart', async () => {
    renderApp()
    await addToCart('Product 1')
    await userEvent.click(within(cart()).getByRole('button', { name: 'Checkout' }))

    const dialog = screen.getByRole('dialog', { name: 'Compra exitosa' })
    expect(useCartStore.getState().items).toEqual([])
    await waitFor(() => expect(queryCart()).not.toBeInTheDocument())
    await userEvent.click(within(dialog).getByRole('button', { name: 'Close' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('shows a non-empty cart by default after reloading', async () => {
    useCartStore.getState().addItem(product(1, 10.5, 5))
    useCartStore.setState({ isOpen: false })
    await useCartStore.persist.rehydrate()
    renderApp()
    expect(cart()).toBeVisible()
    expect(within(cart()).getByText('Product 1')).toBeInTheDocument()
  })
})
