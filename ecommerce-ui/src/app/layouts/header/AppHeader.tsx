import StorefrontIcon from '@mui/icons-material/Storefront'
import AppBar from '@mui/material/AppBar'
import Container from '@mui/material/Container'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { CartButton } from './CartButton'
import { SearchBar } from './SearchBar'

interface AppHeaderProps {
  readonly cartCount?: number
  readonly onSearch?: (query: string) => void
  readonly onCartClick?: () => void
}

export const AppHeader = ({ cartCount, onSearch, onCartClick }: AppHeaderProps) => (
  <AppBar position='sticky' component='header'>
    <Container maxWidth='xl'>
      <Toolbar disableGutters sx={{ gap: 2 }}>
        <StorefrontIcon color='primary' fontSize='medium' />
        <Typography variant='h6' component='span' color='primary' sx={{ display: { xs: 'none', sm: 'block' } }}>
          Ecommerce
        </Typography>
        <div className='flex flex-1 justify-center'>
          <SearchBar onSearch={onSearch} />
        </div>
        <CartButton count={cartCount} onClick={onCartClick} />
      </Toolbar>
    </Container>
  </AppBar>
)
