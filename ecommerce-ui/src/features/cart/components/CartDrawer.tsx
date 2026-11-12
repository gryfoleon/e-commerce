import RemoveShoppingCartIcon from '@mui/icons-material/RemoveShoppingCartOutlined'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Drawer from '@mui/material/Drawer'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { formatPrice } from '../../../shared/utils/format'
import { useCheckout } from '../hooks/cart.checkout'
import { type CartItem, useCartStore } from '../stores/cart.store'
import { CartClearDialog } from './CartClearDialog'
import { CartItemRow } from './CartItemRow'
import { CartRemoveItemDialog } from './CartRemoveItemDialog'
import { CheckoutSuccessDialog } from './CheckoutSuccessDialog'

export const CART_DRAWER_WIDTH = 380

export const CartDrawer = () => {
  const { items, total, isOpen, increment, decrement, removeItem, clear } = useCartStore()
  const { checkout, isSuccessOpen, closeSuccess } = useCheckout()
  const [removing, setRemoving] = useState<CartItem | null>(null)
  const [isClearOpen, setClearOpen] = useState(false)
  const isEmpty = items.length === 0

  return (
    <>
      <Drawer
        variant='persistent'
        anchor='right'
        open={isOpen}
        slotProps={{ paper: { role: 'complementary', 'aria-label': 'Shopping cart' } }}
        sx={{
          '& .MuiDrawer-paper': {
            width: { xs: '100%', md: CART_DRAWER_WIDTH },
            zIndex: (theme) => theme.zIndex.appBar - 1,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden'
          }
        }}
      >
        <Toolbar />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
          <Typography variant='h6' component='h2'>
            Shopping cart
          </Typography>
          <Button size='small' color='error' disabled={isEmpty} onClick={() => setClearOpen(true)}>
            Clear cart
          </Button>
        </Box>
        {isEmpty ? (
          <Box
            sx={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 1,
              px: 3,
              color: 'text.secondary',
              textAlign: 'center'
            }}
          >
            <RemoveShoppingCartIcon fontSize='large' />
            <Typography sx={{ fontWeight: 600 }}>Your cart is empty</Typography>
            <Typography variant='body2'>Add products from the table to start your purchase.</Typography>
          </Box>
        ) : (
          <Box component='ul' aria-label='Cart items' sx={{ flex: 1, minHeight: 0, overflowY: 'auto', m: 0, px: 2, listStyle: 'none' }}>
            {items.map((item) => (
              <CartItemRow
                key={item.productId}
                item={item}
                onIncrement={() => increment(item.productId)}
                onDecrement={() => decrement(item.productId)}
                onRequestRemove={() => setRemoving(item)}
              />
            ))}
          </Box>
        )}
        <Box component='footer' sx={{ borderTop: 1, borderColor: 'divider', p: 2, bgcolor: 'background.paper' }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
            <Typography sx={{ fontWeight: 600 }}>Total</Typography>
            <Typography sx={{ fontWeight: 700 }} aria-label='Cart total'>
              {formatPrice(total)}
            </Typography>
          </Box>
          <Button fullWidth variant='contained' size='large' disabled={isEmpty} onClick={checkout}>
            Checkout
          </Button>
        </Box>
      </Drawer>
      <CartRemoveItemDialog
        item={removing}
        onCancel={() => setRemoving(null)}
        onConfirm={() => {
          if (removing) removeItem(removing.productId)
          setRemoving(null)
        }}
      />
      <CartClearDialog
        open={isClearOpen}
        onCancel={() => setClearOpen(false)}
        onConfirm={() => {
          clear()
          setClearOpen(false)
        }}
      />
      <CheckoutSuccessDialog open={isSuccessOpen} onClose={closeSuccess} />
    </>
  )
}
