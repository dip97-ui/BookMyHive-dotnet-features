export default function Logo({ size = 34 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true">
      <polygon points="50,3 94,27 94,73 50,97 6,73 6,27" fill="#f2a900" />
      <polygon points="50,22 77,37 77,63 50,78 23,63 23,37" fill="none" stroke="#2b170a" strokeWidth="7" />
      <circle cx="50" cy="50" r="7" fill="#2b170a" />
    </svg>
  );
}
