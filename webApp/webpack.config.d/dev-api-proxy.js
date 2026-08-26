if (config.devServer) {
    const existingProxy = Array.isArray(config.devServer.proxy)
        ? config.devServer.proxy
        : [];

    config.devServer.proxy = [
        ...existingProxy,
        {
            context: ["/api"],
            target: "https://signalbrief-bj7.pages.dev",
            changeOrigin: true,
            secure: true,
        },
    ];
}
