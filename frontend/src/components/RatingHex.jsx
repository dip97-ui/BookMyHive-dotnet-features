export default function RatingHex({ value }) {
  if (value == null) return null;
  return (
    <span className="ratinghex" title={`Rated ${value} out of 10`}>
      {Number(value).toFixed(1)}
    </span>
  );
}
