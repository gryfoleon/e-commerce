import { useState } from 'react'
import { useCartStore } from '../stores/cart.store'

export const useCheckout = () => {
  const clear = useCartStore((state) => state.clear)
  const [isSuccessOpen, setSuccessOpen] = useState(false)

  return {
    checkout: () => {
      clear()
      setSuccessOpen(true)
    },
    isSuccessOpen,
    closeSuccess: () => setSuccessOpen(false)
  }
}
