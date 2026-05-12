module.exports = {
  devServer: {
    port: 3003,
    proxy: {
      '/api': {
        target: 'http://localhost:8003',
        changeOrigin: true
      }
    }
  }
}
