#!/bin/bash

# Script to run all unit tests for Simon mobile app

echo "🧪 Running Simon Voice Assistant Unit Tests..."
echo "============================================"

# Run unit tests
echo ""
echo "📝 Running unit tests..."
./gradlew test

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All unit tests passed!"
    
    # Generate test report location
    echo ""
    echo "📊 Test reports available at:"
    echo "   file://$(pwd)/app/build/reports/tests/test/index.html"
else
    echo ""
    echo "❌ Some tests failed. Check the test report for details:"
    echo "   file://$(pwd)/app/build/reports/tests/test/index.html"
    exit 1
fi

# Optionally run code coverage
echo ""
echo "📈 Running code coverage analysis..."
./gradlew testDebugUnitTest

if [ $? -eq 0 ]; then
    echo ""
    echo "📊 Coverage report available at:"
    echo "   file://$(pwd)/app/build/reports/coverage/test/debug/index.html"
fi

echo ""
echo "============================================"
echo "🎉 Test run complete!"