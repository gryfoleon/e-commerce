import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import Typography from '@mui/material/Typography'

export const AppFooter = () => (
  <Box component="footer" sx={{ bgcolor: 'grey.900', color: 'grey.300', py: 4, mt: 'auto' }}>
    <Container maxWidth="xl">
      <Typography variant="body2" align="center">
        © Ecommerce. All rights reserved.
      </Typography>
    </Container>
  </Box>
)
