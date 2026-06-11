export default function Card({
  as: Tag = 'div',
  padded = true,
  className = '',
  children,
  ...props
}) {
  return (
    <Tag
      className={`bg-surface border border-border rounded-xl ${padded ? 'p-6' : ''} ${className}`}
      {...props}
    >
      {children}
    </Tag>
  );
}
