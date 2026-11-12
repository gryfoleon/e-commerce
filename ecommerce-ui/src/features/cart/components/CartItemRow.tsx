import AddIcon from '@mui/icons-material/Add'
import RemoveIcon from '@mui/icons-material/Remove'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Typography from '@mui/material/Typography'
import { formatPrice } from '../../../shared/utils/format'
import type { CartItem } from '../stores/cart.store'

interface CartItemRowProps {
  readonly item: CartItem
  readonly onIncrement: () => void
  readonly onDecrement: () => void
  readonly onRequestRemove: () => void
}

export const CartItemRow = ({ item, onIncrement, onDecrement, onRequestRemove }: CartItemRowProps) => {
  const atStockLimit = item.quantity >= item.stock
  return (
    <Box
      component='li'
      aria-label={item.name}
      sx={{ display: 'flex', flexDirection: 'column', gap: 1, py: 1.5, borderBottom: 1, borderColor: 'divider' }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography sx={{ fontWeight: 600 }} noWrap title={item.name}>
            {item.name}
          </Typography>
          <Typography variant='caption' color='text.secondary'>
            {item.sku} · {formatPrice(item.unitPrice)} each
          </Typography>
        </Box>
        <Typography sx={{ fontWeight: 600, whiteSpace: 'nowrap' }} aria-label={`Subtotal of ${item.name}`}>
          {formatPrice(item.subtotal)}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <IconButton
          size='small'
          aria-label={`Decrease quantity of ${item.name}`}
          onClick={item.quantity <= 1 ? onRequestRemove : onDecrement}
          sx={{ border: 1, borderColor: 'divider' }}
        >
          <RemoveIcon fontSize='small' />
        </IconButton>
        <Typography aria-label={`Quantity of ${item.name}`} sx={{ minWidth: 24, textAlign: 'center' }}>
          {item.quantity}
        </Typography>
        <IconButton
          size='small'
          aria-label={`Increase quantity of ${item.name}`}
          title={atStockLimit ? 'No more stock available' : undefined}
          onClick={onIncrement}
          disabled={atStockLimit}
          sx={{ border: 1, borderColor: 'divider' }}
        >
          <AddIcon fontSize='small' />
        </IconButton>
      </Box>
    </Box>
  )
}
