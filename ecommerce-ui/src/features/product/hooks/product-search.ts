import { useQueryClient } from '@tanstack/react-query'
import { useProductFilterStore } from '../stores/product-filter.store'
import { toProductFilterParams } from '../util/product.utils'
import { productQueryKeys } from './product.query'

export const useProductSearchActions = () => {
  const queryClient = useQueryClient()
  const applyFilters = useProductFilterStore((state) => state.applyFilters)
  const setSearch = useProductFilterStore((state) => state.setSearch)

  const refetchCurrentList = () => {
    const params = toProductFilterParams(useProductFilterStore.getState())
    void queryClient.refetchQueries({ queryKey: productQueryKeys.list(params), exact: true }, { cancelRefetch: false })
  }

  return {
    applyFilters: () => {
      applyFilters()
      refetchCurrentList()
    },
    search: (name: string) => {
      setSearch(name)
      refetchCurrentList()
    }
  }
}
