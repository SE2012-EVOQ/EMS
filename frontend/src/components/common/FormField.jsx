export function FormField({ label, ...props }) {
  return <label className="block"><span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">{label}</span><input {...props} className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-transparent focus:border-gray-300 outline-none text-xs font-semibold" /></label>
}

export function SelectField({ label, children, ...props }) {
  return <label className="block"><span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">{label}</span><select {...props} className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-transparent focus:border-gray-300 outline-none text-xs font-semibold">{children}</select></label>
}
