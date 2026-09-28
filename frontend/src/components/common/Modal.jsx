import { X } from 'lucide-react'

export default function Modal({ open, onClose, title, subtitle, children, footer, size = 'max-w-xl' }) {
  if (!open) return null
  return (
    <div className="modal-layer fixed inset-0 z-[80] modal-backdrop p-4 flex items-center justify-center" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className={`modal-panel surface bg-white w-full ${size} max-h-[90vh] overflow-hidden rounded-[30px] shadow-2xl border border-white/30`}>
        <div className="p-5 sm:p-6 flex items-start justify-between gap-3 border-b border-app-border">
          <div><div className="text-lg font-extrabold tracking-tight txt">{title}</div><div className="text-xs text-app-muted muted mt-1">{subtitle}</div></div>
          <button onClick={onClose} className="w-9 h-9 rounded-full bg-app-subtle subtle flex items-center justify-center"><X className="w-4 h-4" /></button>
        </div>
        <div className="p-5 sm:p-6 overflow-y-auto max-h-[68vh]">{children}</div>
        {footer && <div className="p-5 sm:p-6 border-t border-app-border flex justify-end gap-2">{footer}</div>}
      </div>
    </div>
  )
}
