const CACHE_SCHEMA_VERSION = "3";
const CACHE_TTL_SECONDS = 900;
const DEFAULT_COUNTRY = "us";
const COUNTRY_PATTERN = /^[a-z]{2}$/;

export async function onRequestGet(context) {
  const { request, env } = context;

  if (!env.NEWSDATA_API_KEY || !env.IMAGE_PROXY_SIGNING_KEY) {
    return jsonResponse(
      { error: "missing_api_configuration" },
      500,
    );
  }

  const incomingUrl = new URL(request.url);
  const requestedCountry = (
    incomingUrl.searchParams.get("country") ?? DEFAULT_COUNTRY
  ).toLowerCase();

  const country = COUNTRY_PATTERN.test(requestedCountry)
    ? requestedCountry
    : DEFAULT_COUNTRY;

  const cacheUrl = new URL(request.url);
  cacheUrl.search = "";
  cacheUrl.searchParams.set("country", country);
  cacheUrl.searchParams.set("v", CACHE_SCHEMA_VERSION);

  const cacheKey = new Request(cacheUrl.toString(), {
    method: "GET",
  });

  const cache = caches.default;
  const cachedResponse = await cache.match(cacheKey);

  if (cachedResponse) {
    return cachedResponse;
  }

  const upstreamUrl = new URL("https://newsdata.io/api/1/latest");
  upstreamUrl.searchParams.set("apikey", env.NEWSDATA_API_KEY);
  upstreamUrl.searchParams.set("language", "en");
  upstreamUrl.searchParams.set("country", country);
  upstreamUrl.searchParams.set("category", "top");
  upstreamUrl.searchParams.set("image", "1");
  upstreamUrl.searchParams.set("removeduplicate", "1");

  let upstreamResponse;

  try {
    upstreamResponse = await fetch(upstreamUrl.toString(), {
      headers: {
        Accept: "application/json",
      },
    });
  } catch {
    return jsonResponse(
      { error: "upstream_unavailable" },
      502,
    );
  }

  if (!upstreamResponse.ok) {
    return jsonResponse(
      {
        error: "upstream_error",
        status: upstreamResponse.status,
      },
      502,
    );
  }

  const payload = await upstreamResponse.json();

  if (payload.status !== "success" || !Array.isArray(payload.results)) {
    return jsonResponse(
      { error: "invalid_upstream_response" },
      502,
    );
  }

  const signingKey = await importSigningKey(
    env.IMAGE_PROXY_SIGNING_KEY,
  );

  const articles = await Promise.all(
    payload.results
      .filter((article) => article.title && article.link)
      .map(async (article) => {
        const originalImageUrl = normalizeImageUrl(article.image_url);

        return {
          id: article.article_id,
          title: article.title,
          description: article.description ?? null,
          url: article.link,
          imageUrl: originalImageUrl
            ? await buildImageProxyReference(originalImageUrl, signingKey)
            : null,
          publishedAt: article.pubDate,
          sourceName: normalizeSourceName(
            article.source_name,
            article.source_id,
          ),
        };
      }),
  );

  const response = jsonResponse(
    { articles },
    200,
    {
      "Cache-Control": `public, max-age=60, s-maxage=${CACHE_TTL_SECONDS}`,
    },
  );

  context.waitUntil(cache.put(cacheKey, response.clone()));

  return response;
}

function normalizeImageUrl(value) {
  if (typeof value !== "string") {
    return null;
  }

  try {
    const url = new URL(value);

    return url.protocol === "https:"
      ? url.toString()
      : null;
  } catch {
    return null;
  }
}

function normalizeSourceName(name, sourceId) {
  const value = typeof name === "string" ? name.trim() : "";

  if (value && !/^https?:\/\//i.test(value)) {
    return value;
  }

  if (sourceId) {
    return formatSourceId(sourceId);
  }

  if (value) {
    try {
      return new URL(value).hostname.replace(/^www\./, "");
    } catch {
      return "Unknown";
    }
  }

  return "Unknown";
}

function formatSourceId(sourceId) {
  return sourceId
    .split(/[-_]/)
    .filter(Boolean)
    .map((part) =>
      part.length <= 5
        ? part.toUpperCase()
        : part.charAt(0).toUpperCase() + part.slice(1),
    )
    .join(" ");
}

async function buildImageProxyReference(imageUrl, key) {
  const signature = await signValue(imageUrl, key);
  const params = new URLSearchParams({
    url: imageUrl,
    sig: signature,
    v: "2",
  });

  return `/api/image?${params.toString()}`;
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
    ["sign", "verify"],
  );
}

async function signValue(value, key) {
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(value),
  );

  return Array.from(new Uint8Array(signature))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function jsonResponse(body, status, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...extraHeaders,
    },
  });
}
