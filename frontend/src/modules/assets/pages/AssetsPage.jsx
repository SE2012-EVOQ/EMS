import { useEffect, useState } from 'react'
import { Laptop } from 'lucide-react'

import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'
import StatusBadge from '../../../components/common/StatusBadge'

import { getAllAssets } from '../services/assetService'

export default function AssetsPage() {
  const [assets, setAssets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadAssets()
  }, [])

  async function loadAssets() {
    try {
      setLoading(true)
      setError('')

      const data = await getAllAssets()
      setAssets(data)
    } catch (err) {
      setError(err.message || 'Could not load assets')
    } finally {
      setLoading(false)
    }
  }

  function displayStatus(status) {
    if (!status) return 'Unknown'

    return status
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
  }

  return (
    <>
      <PageHeader
        title="Assets & equipment"
        description="Manage company assets, equipment and employee assignments."
      />

      <Card>
        {loading && (
          <p className="text-sm text-app-muted muted">
            Loading assets...
          </p>
        )}

        {!loading && error && (
          <div className="text-sm font-semibold text-red-500">
            {error}
          </div>
        )}

        {!loading && !error && assets.length === 0 && (
          <EmptyState
            icon="Laptop"
            title="No asset records available"
            description="Register your first company asset to get started."
          />
        )}

        {!loading && !error && assets.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-app-border">
                  <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                    Asset
                  </th>

                  <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                    Type
                  </th>

                  <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                    Serial Number
                  </th>

                  <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                    Status
                  </th>
                </tr>
              </thead>

              <tbody>
                {assets.map(asset => (
                  <tr
                    key={asset.assetId}
                    className="border-b border-app-border/60 last:border-0"
                  >
                    <td className="py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-app-subtle subtle flex items-center justify-center">
                          <Laptop className="w-4 h-4" />
                        </div>

                        <div>
                          <div className="text-xs font-bold txt">
                            {asset.assetName}
                          </div>

                          <div className="text-[10px] text-app-muted muted mt-1">
                            Asset #{asset.assetId}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="py-4 text-xs font-semibold txt">
                      {asset.assetType}
                    </td>

                    <td className="py-4 text-xs text-app-muted muted">
                      {asset.serialNumber}
                    </td>

                    <td className="py-4">
                      <StatusBadge status={displayStatus(asset.status)} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  )
}