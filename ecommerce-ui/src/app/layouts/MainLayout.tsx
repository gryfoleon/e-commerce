import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import { type ReactNode } from 'react'
import { AppFooter } from './footer/AppFooter'
import { AppHeader } from './header/AppHeader'

interface MainLayoutProps {
  readonly children: ReactNode
  readonly cartCount?: number
  readonly onSearch?: (query: string) => void
  readonly onCartClick?: () => void
  readonly aside?: ReactNode
  readonly asideWidth?: number
}

export const MainLayout = ({ children, cartCount, onSearch, onCartClick, aside, asideWidth = 0 }: MainLayoutProps) => (
  <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
    <AppHeader cartCount={cartCount} onSearch={onSearch} onCartClick={onCartClick} />
    <Box
      sx={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        mr: { xs: 0, md: `${asideWidth}px` },
        transition: (theme) => theme.transitions.create('margin')
      }}
    >
      <Container component='main' maxWidth='xl' sx={{ flex: 1, py: { xs: 2, md: 4 }, px: { xs: 2, md: 3 } }}>
        {children}
      </Container>
      <AppFooter />
    </Box>
    {aside}
  </Box>
)
