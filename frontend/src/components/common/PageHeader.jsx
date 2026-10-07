export default function PageHeader({ title, description, actions, primary = false }) {
  const Heading = primary ? 'h1' : 'h2'
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5">
      <div>
        <Heading className={primary ? 'text-2xl sm:text-3xl font-extrabold tracking-tight txt' : 'text-lg font-extrabold tracking-tight txt'}>{title}</Heading>
        {description && <p className="text-xs text-app-muted muted font-medium mt-1">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2 flex-wrap">{actions}</div>}
    </div>
  )
}
