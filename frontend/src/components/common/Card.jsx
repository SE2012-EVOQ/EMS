export default function Card({ children, className = '' }) {
  return <section className={`surface bg-white rounded-[28px] p-5 sm:p-6 shadow-card border border-app-border/50 ${className}`}>{children}</section>
}
