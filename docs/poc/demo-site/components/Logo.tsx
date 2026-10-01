export default function Logo({ size = 32 }: { size?: number }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width={size} height={size} fill="none" aria-hidden="true">
      <rect width="48" height="48" rx="12" fill="#2F5D45" />
      <path d="M12 36H36" stroke="#FFFFFF" strokeWidth="2.5" strokeLinecap="round" />
      <path d="M15 36V18C15 16.8954 15.8954 16 17 16H20C21.1046 16 22 16.8954 22 18V36" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M22 36V13C22 11.8954 22.8954 11 24 11H27C28.1046 11 29 11.8954 29 13V36" stroke="#E4EEE8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M29 36L34 19.5C34.3 18.5 35.4 18 36.4 18.3L36.6 18.4C37.6 18.7 38.1 19.8 37.8 20.8L33.2 36" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="18.5" cy="22" r="1" fill="#FFFFFF" />
      <circle cx="25.5" cy="17" r="1" fill="#2F5D45" />
    </svg>
  );
}
