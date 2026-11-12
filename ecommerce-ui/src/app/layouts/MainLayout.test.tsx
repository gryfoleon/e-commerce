import { render, screen } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { appTheme } from '../theme/theme'
import { MainLayout } from './MainLayout'

describe('MainLayout', () => {
  it('renders header, main content and footer', () => {
    render(
      <ThemeProvider theme={appTheme}>
        <MainLayout cartCount={3}>
          <p>contenido</p>
        </MainLayout>
      </ThemeProvider>
    )
    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(screen.getByRole('search')).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveTextContent('contenido')
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })
})
