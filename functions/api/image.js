const IMAGE_CACHE_TTL_SECONDS = 86400;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export async function onRequestGet(context) {
  const { request, env } = context;

  if (!env.IMAGE_PROXY_SIGNING_KEY) {
    return errorResponse("missing_api_configuration", 500);
  }

  const requestUrl = new URL(request.url);
  const imageUrl = requestUrl.searchParams.get("url");
  const signature = requestUrl.searchParams.get("sig");

  if (!imageUrl || !signature) {
    return errorResponse("invalid_request", 400);
  }

  if (!isAllowedImageUrl(imageUrl)) {
    return errorResponse("invalid_image_url", 400);
  }

  const signingKey = await importSigningKey(
    env.IMAGE_PROXY_SIGNING_KEY,
  );

  const signatureValid = await verifyValue(
    imageUrl,
    signature,
    signingKey,
  );

  if (!signatureValid) {
    return errorResponse("invalid_signature", 403);
  }

  const cache = caches.default;
  const cacheKey = new Request(request.url, {
    method: "GET",
  });

  const cachedResponse = await cache.match(cacheKey);

  if (cachedResponse) {
    return cachedResponse;
  }

  let upstreamResponse;

  try {
    upstreamResponse = await fetch(imageUrl, {
      headers: {
        Accept: "image/webp,image/jpeg,image/png,image/*;q=0.8,*/*;q=0.5",
      },
    });
  } catch {
    return errorResponse("image_upstream_unavailable", 502);
  }

  if (!upstreamResponse.ok) {
    return errorResponse("image_upstream_error", 502);
  }

  const contentType =
    upstreamResponse.headers.get("content-type") ?? "";

  if (!contentType.toLowerCase().startsWith("image/")) {
    return errorResponse("invalid_image_content_type", 415);
  }

  const declaredLength = Number(
    upstreamResponse.headers.get("content-length") ?? 0,
  );

  if (declaredLength > MAX_IMAGE_BYTES) {
    return errorResponse("image_too_large", 413);
  }

  const body = await upstreamResponse.arrayBuffer();

  if (body.byteLength > MAX_IMAGE_BYTES) {
    return errorResponse("image_too_large", 413);
  }

  const response = new Response(body, {
    status: 200,
    headers: {
      "Content-Type": contentType,
      "Cache-Control": `public, max-age=${IMAGE_CACHE_TTL_SECONDS}`,
      "X-Content-Type-Options": "nosniff",
    },
  });

  context.waitUntil(cache.put(cacheKey, response.clone()));

  return response;
}

function isAllowedImageUrl(value) {
  try {
    const url = new URL(value);

    if (url.protocol !== "https:") {
      return false;
    }

    const host = url.hostname.toLowerCase();

    if (
      host === "localhost" ||
      host.endsWith(".localhost") ||
      host.endsWith(".local") ||
      host.includes(":")
    ) {
      return false;
    }

    return !isPrivateIpv4(host);
  } catch {
    return false;
  }
}

function isPrivateIpv4(host) {
  const parts = host.split(".");

  if (parts.length !== 4) {
    return false;
  }

  const numbers = parts.map(Number);

  if (
    numbers.some(
      (part) =>
        !Number.isInteger(part) ||
        part < 0 ||
        part > 255,
    )
  ) {
    return false;
  }

  const [a, b] = numbers;

  return (
    a === 0 ||
    a === 10 ||
    a === 127 ||
    a >= 224 ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168)
  );
}

async function importSigningKey(secret) {
  return crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    {
      name: "HMAC",
      hash: "SHA-256",
    },
    false,
    ["verify"],
  );
}

async function verifyValue(value, signature, key) {
  const signatureBytes = hexToBytes(signature);

  if (!signatureBytes) {
    return false;
  }

  return crypto.subtle.verify(
    "HMAC",
    key,
    signatureBytes,
    new TextEncoder().encode(value),
  );
}

function hexToBytes(value) {
  if (
    value.length === 0 ||
    value.length % 2 !== 0 ||
    !/^[0-9a-f]+$/i.test(value)
  ) {
    return null;
  }

  const bytes = new Uint8Array(value.length / 2);

  for (let index = 0; index < bytes.length; index += 1) {
    bytes[index] = Number.parseInt(
      value.slice(index * 2, index * 2 + 2),
      16,
    );
  }

  return bytes;
}

function errorResponse(error, status) {
  return new Response(
    JSON.stringify({ error }),
    {
      status,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "no-store",
      },
    },
  );
}
