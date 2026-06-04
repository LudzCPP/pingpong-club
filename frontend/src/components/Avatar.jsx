const COLORS = [
  'bg-green-600', 'bg-blue-600', 'bg-purple-600',
  'bg-orange-600', 'bg-pink-600', 'bg-teal-600',
];

function colorFor(name = '') {
  let hash = 0;
  for (const c of name) hash = c.charCodeAt(0) + ((hash << 5) - hash);
  return COLORS[Math.abs(hash) % COLORS.length];
}

const sizes = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-14 h-14 text-lg',
};

export default function Avatar({ firstName = '', lastName = '', size = 'md' }) {
  const initials = `${firstName[0] ?? ''}${lastName[0] ?? ''}`.toUpperCase();
  const bg = colorFor(firstName + lastName);
  return (
    <span className={`${sizes[size]} ${bg} rounded-full flex items-center justify-center font-bold text-white shrink-0`}>
      {initials || '?'}
    </span>
  );
}
