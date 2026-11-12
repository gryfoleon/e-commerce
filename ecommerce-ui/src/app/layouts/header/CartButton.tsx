import ShoppingCartIcon from '@mui/icons-material/ShoppingCartOutlined'
import Badge from '@mui/material/Badge'
import IconButton from '@mui/material/IconButton'

interface CartButtonProps {
  readonly count?: number
  readonly onClick?: () => void
}

export const CartButton = ({ count = 0, onClick }: CartButtonProps) => (
  <IconButton aria-label={`Shopping cart, ${count} items`} onClick={onClick} color="primary">
    <Badge badgeContent={count} max={99}>
      <ShoppingCartIcon />
    </Badge>
  </IconButton>
)
