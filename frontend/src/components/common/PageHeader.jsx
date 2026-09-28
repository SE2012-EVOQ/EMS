export default function PageHeader({ title, description, actions }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5">
      <div>
        <h2 className="text-lg font-extrabold tracking-tight txt">{title}</h2>
        <p className="text-xs text-app-muted muted font-medium mt-1">{description}</p>
      </div>
      {actions && <div className="flex items-center gap-2 flex-wrap">{actions}</div>}
    </div>
  )
}
