#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   Disney TriPeaks Solver - Setup & Upload Script         ║"
echo "║   Created by Dev Malvryx                                  ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

echo "📁 Current directory: $(pwd)"
echo ""

# Initialize git if not already
if [ ! -d ".git" ]; then
    echo "🔧 Initializing Git repository..."
    git init
    git branch -M main
    echo "✅ Git initialized"
else
    echo "✅ Git repository already initialized"
fi

# Configure git (update with your details)
echo ""
echo "⚙️  Configuring Git..."
read -p "Enter your GitHub username: " github_user
read -p "Enter your email: " github_email

git config user.name "$github_user"
git config user.email "$github_email"

echo "✅ Git configured"

# Add all files
echo ""
echo "📦 Adding files to Git..."
git add .

# Create .gitignore if needed
if [ ! -f ".gitignore" ]; then
    echo "Creating .gitignore..."
    cat > .gitignore << 'EOF'
*.iml
.gradle
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
*.apk
*.ap_
*.dex
*.class
bin/
gen/
out/
*.log
*.hprof
EOF
fi

# Commit
echo ""
echo "💾 Committing files..."
git commit -m "Initial commit: TriPeaks Solver with intelligent DFS algorithm

Features:
- Smart DFS solver with heuristics
- Move prioritization (peaks, uncovers, middle ranks)
- State hashing to avoid cycles
- Test UI for validation
- TensorFlow Lite support for card recognition (Phase 2)
- Accessibility Service stub for auto-play (Phase 2)
- GitHub Actions for automatic APK building

Ready to solve TriPeaks puzzles! 🎮"

echo "✅ Files committed"

# GitHub repo creation
echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   GitHub Repository Setup                                 ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "Now you need to:"
echo "1. Go to https://github.com/new on your phone/browser"
echo "2. Create a new repository named: DisneyTriPeaksSolver"
echo "3. Do NOT initialize with README (we already have one)"
echo "4. Make it Public or Private (your choice)"
echo ""
read -p "Press ENTER when you've created the GitHub repo..."

# Add remote
echo ""
echo "🔗 Adding GitHub remote..."
read -p "Enter the repo URL (e.g., https://github.com/$github_user/DisneyTriPeaksSolver.git): " repo_url

git remote remove origin 2>/dev/null
git remote add origin "$repo_url"

echo "✅ Remote added"

# Push to GitHub
echo ""
echo "🚀 Pushing to GitHub..."
echo ""
echo "You may be asked for your GitHub credentials:"
echo "  - Username: $github_user"
echo "  - Password: Use a Personal Access Token (not your password!)"
echo "  - Get token at: https://github.com/settings/tokens"
echo ""
read -p "Press ENTER to push..."

git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║   🎉 SUCCESS! Project uploaded to GitHub!                ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo ""
    echo "✅ Next steps:"
    echo ""
    echo "1. Go to: $repo_url/actions"
    echo "2. Watch the build process (takes ~5 minutes)"
    echo "3. Download the APK from 'Artifacts' section"
    echo "4. Install on your phone"
    echo "5. Test the solver!"
    echo ""
    echo "📱 On your phone:"
    echo "   • Download APK from GitHub Actions artifacts"
    echo "   • Enable 'Unknown Sources' in settings"
    echo "   • Install the APK"
    echo "   • Tap 'Test Solver' to see it work!"
    echo ""
    echo "🚀 Happy solving!"
else
    echo ""
    echo "❌ Push failed. Common issues:"
    echo "  • Wrong credentials (use Personal Access Token)"
    echo "  • Repository doesn't exist"
    echo "  • Network issues"
    echo ""
    echo "Try again with: git push -u origin main"
fi
