.PHONY: setup run-android run-ios test lint build-debug build-release clean

ANDROID_PACKAGE = com.kami.gamelist
ANDROID_ACTIVITY = $(ANDROID_PACKAGE).MainActivity
IOS_BUNDLE_ID = com.kami.gamelist
IOS_SCHEME = iosApp
IOS_PROJECT = iosApp/iosApp.xcodeproj
IOS_SIMULATOR ?= iPhone 17
IOS_DERIVED_DATA = build/ios-derived-data

setup:
	./gradlew --version
	@echo "✓ Gradle OK"
	@echo "Run 'make run-android' or 'make run-ios' to start the app"

run-android:
	@echo "▸ Starting Android emulator if needed..."
	@if ! adb devices 2>/dev/null | grep -q "device$$"; then \
		echo "  No device found. Starting Pixel_10 emulator..."; \
		$(HOME)/Library/Android/sdk/emulator/emulator -avd Pixel_10 -no-snapshot-load &>/dev/null & \
		adb wait-for-device; \
		echo "  Waiting for boot..."; \
		while [ "$$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do sleep 1; done; \
	fi
	@echo "▸ Building and installing..."
	./gradlew :composeApp:installDebug
	@echo "▸ Launching app..."
	adb shell am start -n $(ANDROID_PACKAGE)/$(ANDROID_ACTIVITY) -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
	@echo "✓ Android app running"

run-ios:
	@echo "▸ Booting simulator '$(IOS_SIMULATOR)' if needed..."
	@xcrun simctl boot "$(IOS_SIMULATOR)" 2>/dev/null || true
	@open -a Simulator
	@echo "▸ Building for simulator..."
	xcodebuild -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) \
		-destination 'platform=iOS Simulator,name=$(IOS_SIMULATOR)' \
		-derivedDataPath $(IOS_DERIVED_DATA) \
		-configuration Debug \
		build 2>&1 | tail -5
	@echo "▸ Installing and launching..."
	@xcrun simctl install "$(IOS_SIMULATOR)" $$(find $(IOS_DERIVED_DATA) -name "$(IOS_SCHEME).app" -path "*/Debug-iphonesimulator/*" | head -1)
	@xcrun simctl launch "$(IOS_SIMULATOR)" $(IOS_BUNDLE_ID)
	@echo "✓ iOS app running on $(IOS_SIMULATOR)"

test:
	./gradlew :composeApp:allTests

test-android:
	./gradlew :composeApp:testDebugUnitTest

lint:
	./gradlew :composeApp:lintDebug

build-debug:
	./gradlew :composeApp:assembleDebug

build-release:
	./gradlew :composeApp:assembleRelease

clean:
	./gradlew clean
	rm -rf $(IOS_DERIVED_DATA)
