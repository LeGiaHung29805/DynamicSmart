import type { NextConfig } from "next";

const apiGatewayInternalUrl = (process.env.API_GATEWAY_INTERNAL_URL ?? "http://localhost:8080").replace(/\/$/, "");
const publicTunnelHost = process.env.NEXT_DEV_TUNNEL_HOST?.trim();
const publicTunnelOrigins = publicTunnelHost ? [publicTunnelHost] : [];

const nextConfig: NextConfig = {
  output: "standalone",
  // Next.js 16 blocks dev resources coming from a reverse-proxy host by
  // default. Keep this explicit: only the configured ngrok host is allowed.
  allowedDevOrigins: publicTunnelOrigins,
  experimental: {
    cpus: 1,
    // Covers Server Actions too when the checkout is accessed through ngrok.
    serverActions: {
      allowedOrigins: publicTunnelOrigins,
    },
  },
  async rewrites() {
    // Browser calls remain on the ngrok frontend domain; Next.js then forwards
    // them to the Gateway running locally on this machine.
    return [
      {
        source: "/api/:path*",
        destination: `${apiGatewayInternalUrl}/api/:path*`,
      },
    ];
  },
  images: {
    remotePatterns: [{ protocol: "https", hostname: "images.unsplash.com" }],
  },
};

export default nextConfig;
