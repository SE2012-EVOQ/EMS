export default function FilterPills({ items, value, onChange }) {
  return <div className="flex gap-2 mb-4 overflow-x-auto">{items.map(item => <button key={item} onClick={() => onChange(item)} className={`px-3 py-2 rounded-full text-xs font-bold shrink-0 ${value === item ? 'bg-[#1A1D1F] dark-primary text-white' : 'surface bg-white border border-gray-200'}`}>{item}</button>)}</div>
}
