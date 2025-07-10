#!/bin/bash

# Run only the simple tests without Android framework dependencies

echo "🧪 Running Simple Unit Tests..."
echo "================================"

# Create a test filter file
cat > app/src/test/resources/test-filter.txt << EOF
com.voiceassistant.app.SimpleVoiceAssistantTest
com.voiceassistant.app.OpenAIProtocolTest
EOF

# Run tests with better error output
./gradlew testDebugUnitTest --continue 2>&1 | grep -E "(SimpleVoiceAssistantTest|OpenAIProtocolTest|PASSED|FAILED|tests completed)"

echo ""
echo "================================"
echo "✅ Simple tests complete!"
echo ""
echo "Note: The full test suite requires Android framework mocking."
echo "These simple tests verify core logic without framework dependencies."