export default function Card({ children, className = '', ...props }) {
  return <section {...props} className={`surface bg-white rounded-[24px] p-4 sm:p-5 shadow-card border border-app-border/50 ${className}`}>{children}</section>
}
