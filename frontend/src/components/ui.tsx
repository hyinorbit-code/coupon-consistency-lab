import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { Link, type LinkProps } from 'react-router'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'kakao'
type Size = 'md' | 'lg'

const VARIANT_CLASSES: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 disabled:bg-stone-300 disabled:text-stone-500',
  secondary: 'bg-white text-stone-800 ring-1 ring-stone-300 hover:bg-stone-50 disabled:text-stone-400',
  ghost: 'text-stone-600 hover:bg-stone-100 disabled:text-stone-300',
  danger: 'bg-white text-red-600 ring-1 ring-red-200 hover:bg-red-50 disabled:text-red-300',
  kakao: 'bg-[#FEE500] text-[#191919] hover:brightness-95 disabled:opacity-50',
}

const SIZE_CLASSES: Record<Size, string> = {
  md: 'h-10 px-4 text-sm',
  lg: 'h-14 px-6 text-base',
}

function buttonClasses(variant: Variant, size: Size, block: boolean) {
  return [
    'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-colors',
    'disabled:cursor-not-allowed focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500',
    VARIANT_CLASSES[variant],
    SIZE_CLASSES[size],
    block ? 'w-full' : '',
  ].join(' ')
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
  block?: boolean
  loading?: boolean
}

export function Button({ variant = 'primary', size = 'md', block = false, loading = false, className = '', children, disabled, ...rest }: ButtonProps) {
  return (
    <button className={`${buttonClasses(variant, size, block)} ${className}`} disabled={disabled || loading} {...rest}>
      {loading && <Spinner />}
      {children}
    </button>
  )
}

type LinkButtonProps = LinkProps & { variant?: Variant; size?: Size; block?: boolean }

export function LinkButton({ variant = 'primary', size = 'md', block = false, className = '', ...rest }: LinkButtonProps) {
  return <Link className={`${buttonClasses(variant, size, block)} ${className}`} {...rest} />
}

export function Spinner({ className = '' }: { className?: string }) {
  return (
    <span
      aria-hidden
      className={`inline-block size-4 animate-spin rounded-full border-2 border-current border-r-transparent ${className}`}
    />
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-2xl bg-white p-5 shadow-sm ring-1 ring-stone-200 ${className}`}>{children}</div>
}

type Tone = 'brand' | 'green' | 'stone' | 'red' | 'blue' | 'amber'

const TONE_CLASSES: Record<Tone, string> = {
  brand: 'bg-brand-100 text-brand-700',
  green: 'bg-emerald-100 text-emerald-700',
  stone: 'bg-stone-200 text-stone-600',
  red: 'bg-red-100 text-red-700',
  blue: 'bg-sky-100 text-sky-700',
  amber: 'bg-amber-100 text-amber-800',
}

export function Badge({ tone, children }: { tone: Tone; children: ReactNode }) {
  return <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${TONE_CLASSES[tone]}`}>{children}</span>
}

type AlertKind = 'error' | 'info' | 'success' | 'warning'

const ALERT_CLASSES: Record<AlertKind, string> = {
  error: 'bg-red-50 text-red-700 ring-red-200',
  info: 'bg-sky-50 text-sky-800 ring-sky-200',
  success: 'bg-emerald-50 text-emerald-800 ring-emerald-200',
  warning: 'bg-amber-50 text-amber-900 ring-amber-200',
}

export function Alert({ kind, children }: { kind: AlertKind; children: ReactNode }) {
  return (
    <div role={kind === 'error' ? 'alert' : 'status'} className={`rounded-xl px-4 py-3 text-sm ring-1 ${ALERT_CLASSES[kind]}`}>
      {children}
    </div>
  )
}

type FieldProps = InputHTMLAttributes<HTMLInputElement> & { label: string; hint?: string }

export function Field({ label, hint, id, className = '', ...rest }: FieldProps) {
  const inputId = id ?? rest.name ?? label
  return (
    <label htmlFor={inputId} className="grid gap-1.5">
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <input
        id={inputId}
        className={`h-11 rounded-xl border-0 bg-white px-3.5 text-[15px] ring-1 ring-stone-300 placeholder:text-stone-400 focus:ring-2 focus:ring-brand-500 focus:outline-none ${className}`}
        {...rest}
      />
      {hint && <span className="text-xs text-stone-500">{hint}</span>}
    </label>
  )
}

type TextAreaFieldProps = TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string; hint?: string }

export function TextAreaField({ label, hint, id, className = '', ...rest }: TextAreaFieldProps) {
  const inputId = id ?? rest.name ?? label
  return (
    <label htmlFor={inputId} className="grid gap-1.5">
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <textarea
        id={inputId}
        className={`min-h-32 rounded-xl border-0 bg-white px-3.5 py-3 font-mono text-sm ring-1 ring-stone-300 focus:ring-2 focus:ring-brand-500 focus:outline-none ${className}`}
        {...rest}
      />
      {hint && <span className="text-xs text-stone-500">{hint}</span>}
    </label>
  )
}

export function PageHeader({ title, description, action }: { title: string; description?: ReactNode; action?: ReactNode }) {
  return (
    <div className="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {description && <p className="mt-1 text-sm text-stone-500">{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function LoadingBlock() {
  return (
    <div className="flex justify-center py-16 text-stone-400">
      <Spinner className="size-6" />
    </div>
  )
}

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="rounded-2xl border border-dashed border-stone-300 px-6 py-12 text-center">
      <p className="font-semibold text-stone-700">{title}</p>
      {description && <p className="mt-1 text-sm text-stone-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
