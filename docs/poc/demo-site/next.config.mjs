/** @type {import('next').NextConfig} */
const nextConfig = {
  distDir: process.env.NEXT_DIST_DIR || ".next",
  reactStrictMode: true,
  images: {
    remotePatterns: [{ protocol: "https", hostname: "lh3.googleusercontent.com" }],
  },
};
export default nextConfig;
