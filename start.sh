#!/bin/bash
# Script to start the application with Docker Compose

echo "🚀 Starting SkyHigh Backend Services..."
echo ""

# Stop any existing containers
echo "📦 Stopping existing containers..."
docker-compose down

# Build and start services
echo "🔨 Building and starting services..."
docker-compose up -d --build

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service status
echo ""
echo "📊 Service Status:"
docker-compose ps

# Show logs
echo ""
echo "📋 Application Logs (last 50 lines):"
docker-compose logs --tail=50 app

echo ""
echo "✅ SkyHigh Backend is starting up!"
echo ""
echo "🌐 Access the application:"
echo "   - API: http://localhost:8080"
echo "   - Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   - API Docs: http://localhost:8080/api-docs"
echo "   - Health Check: http://localhost:8080/actuator/health"
echo ""
echo "📊 To view logs: docker-compose logs -f app"
echo "🛑 To stop: docker-compose down"

