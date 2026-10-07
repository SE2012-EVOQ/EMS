export default function RequestFeedback({ error, loading, loadingText = 'Loading…', onRetry }) {
  if (error) return <div role="alert" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-app-pink-bg p-4 text-sm text-app-pink">
    <span>{error}</span>
    {onRetry && <button type="button" onClick={onRetry} disabled={loading} className="font-bold underline disabled:opacity-50">{loading ? 'Retrying…' : 'Retry'}</button>}
  </div>
  if (loading) return <p role="status" className="mb-5 text-sm text-app-muted">{loadingText}</p>
  return null
}
